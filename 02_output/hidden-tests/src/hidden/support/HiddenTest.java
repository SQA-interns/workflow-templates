package app.contacts.hidden.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Hidden suite base (general/testing/hidden-evaluation.md): the backend on a random port with
 * PostgreSQL and the Firebase Auth emulator, a settable clock, only public API calls (plus the
 * Operator's database view for Audit entries and erasure). Self-contained: shares no code with
 * the visible tests.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "APP_PUBLIC_URL=http://localhost:8080",
      "FIREBASE_PROJECT_ID=demo-contacts",
      "RATE_LIMIT_PER_MINUTE=100000"
    })
@Import(HiddenTest.ClockConfig.class)
public abstract class HiddenTest {

  public static final JsonMapper JSON = JsonMapper.builder().build();
  public static final ZoneId LJUBLJANA = ZoneId.of("Europe/Ljubljana");
  public static final String PASSWORD = "Skrito#2027";

  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16.15");
  static final GenericContainer<?> AUTH =
      new GenericContainer<>(
              new ImageFromDockerfile("contacts-auth-emulator-hidden", false)
                  .withFileFromPath(
                      ".",
                      Path.of(
                          System.getProperty(
                              "emulator.dockerfile.dir", "../docker/firebase-emulator"))))
          .withExposedPorts(9099)
          .waitingFor(
              Wait.forLogMessage(".*All emulators ready.*", 1)
                  .withStartupTimeout(Duration.ofMinutes(5)));

  static {
    POSTGRES.start();
    AUTH.start();
  }

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("DB_URL", POSTGRES::getJdbcUrl);
    registry.add("DB_USER", POSTGRES::getUsername);
    registry.add("DB_PASSWORD", POSTGRES::getPassword);
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add(
        "FIREBASE_AUTH_EMULATOR_HOST", () -> AUTH.getHost() + ":" + AUTH.getMappedPort(9099));
  }

  @LocalServerPort private int port;
  @Autowired protected MovableClock clock;
  @Autowired private DataSource dataSource;
  @Autowired private ApplicationContext context;
  private final HttpClient http = HttpClient.newHttpClient();
  protected JdbcTemplate db;

  @BeforeEach
  final void emptyState() {
    db = new JdbcTemplate(dataSource);
    clock.reset();
    List<String> tables =
        db.queryForList(
            "SELECT tablename FROM pg_tables WHERE schemaname = 'public'"
                + " AND tablename <> 'flyway_schema_history'",
            String.class);
    if (!tables.isEmpty()) {
      db.execute("TRUNCATE " + String.join(", ", tables) + " CASCADE");
    }
    emulator("DELETE", "/emulator/v1/projects/demo-contacts/accounts", "owner", null);
  }

  // ------------------------------------------------------------------ HTTP

  /** One answer of the API. */
  public record Answer(int status, String body, java.net.http.HttpHeaders headers, byte[] bytes) {
    /** Body as JSON (missing node if not JSON). */
    public JsonNode json() {
      try {
        return body.isBlank() ? JSON.missingNode() : JSON.readTree(body);
      } catch (RuntimeException e) {
        return JSON.missingNode();
      }
    }

    /** Problem code, or a field error code. */
    public List<String> codes() {
      List<String> codes = new ArrayList<>();
      codes.add(json().path("code").asString(""));
      for (JsonNode e : json().path("errors")) {
        codes.add(e.path("code").asString(""));
      }
      return codes;
    }
  }

  /** A person with a Firebase account and a token. */
  public record Person(String email, String uid, String name, String token) {}

  protected Answer call(String method, String path, String token, JsonNode body) {
    HttpRequest.Builder b =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + path))
            .timeout(Duration.ofSeconds(120));
    if (token != null) {
      b.header("Authorization", "Bearer " + token);
    }
    if (body != null) {
      b.header("Content-Type", "application/json");
      b.method(method, HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)));
    } else {
      b.method(method, HttpRequest.BodyPublishers.noBody());
    }
    try {
      HttpResponse<byte[]> r = http.send(b.build(), HttpResponse.BodyHandlers.ofByteArray());
      return new Answer(
          r.statusCode(), new String(r.body(), StandardCharsets.UTF_8), r.headers(), r.body());
    } catch (java.io.IOException e) {
      throw new IllegalStateException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  protected Answer get(Person p, String path) {
    return call("GET", path, p == null ? null : p.token(), null);
  }

  protected Answer post(Person p, String path, JsonNode body) {
    return call("POST", path, p.token(), body);
  }

  protected Answer put(Person p, String path, JsonNode body) {
    return call("PUT", path, p.token(), body);
  }

  protected Answer patch(Person p, String path, JsonNode body) {
    return call("PATCH", path, p.token(), body);
  }

  protected Answer delete(Person p, String path, JsonNode body) {
    return call("DELETE", path, p.token(), body);
  }

  protected static void problem(Answer a, int status, String code) {
    assertThat(a.status()).as("status of %s", a.body()).isEqualTo(status);
    assertThat(a.codes()).as("codes of %s", a.body()).contains(code);
  }

  // -------------------------------------------------------------- Emulator

  protected JsonNode emulator(String method, String path, String bearer, JsonNode body) {
    String base = "http://" + AUTH.getHost() + ":" + AUTH.getMappedPort(9099);
    HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path));
    b.header("Content-Type", "application/json");
    if (bearer != null) {
      b.header("Authorization", "Bearer " + bearer);
    }
    b.method(
        method,
        body == null
            ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)));
    try {
      HttpResponse<String> r = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
      return r.body().isBlank() ? JSON.missingNode() : JSON.readTree(r.body());
    } catch (java.io.IOException e) {
      throw new IllegalStateException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  private static final String ID = "/identitytoolkit.googleapis.com/v1/";

  /** An e-mail/password account; verified or not; not yet known to the backend. */
  protected Person account(String email, String name, boolean verified) {
    JsonNode up =
        emulator(
            "POST",
            ID + "accounts:signUp?key=k",
            null,
            obj("email", email, "password", PASSWORD, "returnSecureToken", true));
    String uid = up.path("localId").asString();
    if (verified || name != null) {
      ObjectNode update = obj("localId", uid);
      if (verified) {
        update.put("emailVerified", true);
      }
      if (name != null) {
        update.put("displayName", name);
      }
      emulator("POST", ID + "projects/demo-contacts/accounts:update", "owner", update);
    }
    return new Person(email, uid, name, signIn(email));
  }

  protected String signIn(String email) {
    return emulator(
            "POST",
            ID + "accounts:signInWithPassword?key=k",
            null,
            obj("email", email, "password", PASSWORD, "returnSecureToken", true))
        .path("idToken")
        .asString();
  }

  protected Person google(String email, String name) {
    String claims =
        JSON.writeValueAsString(
            obj("sub", "g-" + email, "email", email, "email_verified", true, "name", name));
    JsonNode r =
        emulator(
            "POST",
            ID + "accounts:signInWithIdp?key=k",
            null,
            obj(
                "postBody",
                "id_token="
                    + URLEncoder.encode(claims, StandardCharsets.UTF_8)
                    + "&providerId=google.com",
                "requestUri",
                "http://localhost",
                "returnSecureToken",
                true));
    return new Person(email, r.path("localId").asString(), name, r.path("idToken").asString());
  }

  /** A verified User known to the backend. */
  protected Person person(String email, String name) {
    Person p = account(email, name, true);
    assertThat(get(p, "/me").status()).as("GET /me of %s", email).isEqualTo(200);
    return p;
  }

  protected String id(Person p) {
    return get(p, "/me").json().path("id").asString();
  }

  /** The token with changed claims (unsigned, as emulator tokens are). */
  protected static String claims(String token, Consumer<ObjectNode> editor) {
    String[] parts = token.split("\\.", -1);
    ObjectNode c =
        (ObjectNode)
            JSON.readTree(new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8));
    editor.accept(c);
    return parts[0]
        + "."
        + Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(JSON.writeValueAsString(c).getBytes(StandardCharsets.UTF_8))
        + "."
        + (parts.length > 2 ? parts[2] : "");
  }

  // ------------------------------------------------------------------ JSON

  public static ObjectNode obj(Object... kv) {
    ObjectNode n = JSON.createObjectNode();
    for (int i = 0; i < kv.length; i += 2) {
      Object v = kv[i + 1];
      String k = (String) kv[i];
      if (v == null) {
        n.putNull(k);
      } else if (v instanceof JsonNode j) {
        n.set(k, j);
      } else if (v instanceof Integer x) {
        n.put(k, x);
      } else if (v instanceof Boolean x) {
        n.put(k, x);
      } else {
        n.put(k, v.toString());
      }
    }
    return n;
  }

  public static ArrayNode arr(String... values) {
    ArrayNode a = JSON.createArrayNode();
    for (String v : values) {
      a.add(v);
    }
    return a;
  }

  public static ArrayNode attrs(String... kv) {
    ArrayNode a = JSON.createArrayNode();
    for (int i = 0; i < kv.length; i += 2) {
      a.add(obj("key", kv[i], "value", kv[i + 1]));
    }
    return a;
  }

  public static List<String> texts(JsonNode array, String field) {
    List<String> r = new ArrayList<>();
    for (JsonNode n : array) {
      JsonNode v = n.path(field);
      r.add(v.isNull() || v.isMissingNode() ? null : v.asString());
    }
    return r;
  }

  // ------------------------------------------------------- Groups, Contacts

  protected String group(Person owner, String name) {
    Answer a = post(owner, "/groups", obj("name", name));
    assertThat(a.status()).as("create Group: %s", a.body()).isEqualTo(201);
    return a.json().path("id").asString();
  }

  protected String token(Answer created) {
    String link = created.json().path("link").asString();
    return link.substring(link.lastIndexOf('/') + 1);
  }

  protected String invite(String g, Person owner, String email, String role) {
    Answer a = post(owner, "/groups/" + g + "/invitations", obj("email", email, "role", role));
    assertThat(a.status()).as("invite: %s", a.body()).isEqualTo(201);
    return token(a);
  }

  protected void join(String g, Person owner, Person member, String role) {
    String t = invite(g, owner, member.email(), role);
    assertThat(post(member, "/invitations/" + t + "/accept", null).status()).isEqualTo(200);
  }

  protected List<String> groups(Person p) {
    return texts(get(p, "/groups").json(), "name");
  }

  protected static ObjectNode body(String... fields) {
    return obj("fields", obj((Object[]) fields), "attributes", attrs(), "tags", arr());
  }

  protected String contact(Person p, String g, ObjectNode body) {
    Answer a = post(p, "/groups/" + g + "/contacts", body);
    assertThat(a.status()).as("create Contact: %s", a.body()).isEqualTo(201);
    return a.json().path("id").asString();
  }

  protected String contact(Person p, String g, String... fields) {
    return contact(p, g, body(fields));
  }

  protected static String path(String g, String c) {
    return "/groups/" + g + "/contacts/" + c;
  }

  protected JsonNode read(Person p, String g, String c) {
    Answer a = get(p, path(g, c));
    assertThat(a.status()).as("GET Contact: %s", a.body()).isEqualTo(200);
    return a.json();
  }

  protected static ObjectNode form(JsonNode contact) {
    ArrayNode attributes = JSON.createArrayNode();
    for (JsonNode x : contact.path("attributes")) {
      attributes.add(obj("key", x.path("key").asString(), "value", x.path("value").asString()));
    }
    ArrayNode tags = JSON.createArrayNode();
    for (JsonNode x : contact.path("tags")) {
      tags.add(x.path("name").asString());
    }
    return obj(
        "fields",
        contact.path("fields").deepCopy(),
        "attributes",
        attributes,
        "tags",
        tags,
        "versionNumber",
        contact.path("versionNumber").asInt());
  }

  protected Answer save(Person p, String g, String c, Consumer<ObjectNode> edit) {
    ObjectNode f = form(read(p, g, c));
    edit.accept(f);
    return put(p, path(g, c), f);
  }

  protected static Consumer<ObjectNode> set(String field, String value) {
    return f -> ((ObjectNode) f.path("fields")).put(field, value);
  }

  protected JsonNode history(Person p, String g, String c) {
    Answer a = get(p, path(g, c) + "/versions");
    assertThat(a.status()).as("History: %s", a.body()).isEqualTo(200);
    return a.json();
  }

  protected int version(Person p, String g, String c) {
    return read(p, g, c).path("versionNumber").asInt();
  }

  protected Answer bin(Person p, String g, String c) {
    return post(p, path(g, c) + "/move-to-recycle-bin", obj("versionNumber", version(p, g, c)));
  }

  protected JsonNode page(Person p, String g, String query) {
    Answer a = get(p, "/groups/" + g + "/contacts" + (query.isEmpty() ? "" : "?" + query));
    assertThat(a.status()).as("list: %s", a.body()).isEqualTo(200);
    return a.json();
  }

  protected static List<String> names(JsonNode page) {
    List<String> r = new ArrayList<>();
    for (JsonNode i : page.path("items")) {
      String n = (i.path("firstName").asString("") + " " + i.path("lastName").asString("")).trim();
      r.add(n.isEmpty() ? i.path("company").asString("") : n);
    }
    return r;
  }

  protected String tag(Person p, String g, String name) {
    for (JsonNode t : get(p, "/groups/" + g + "/tags?q=" + name).json()) {
      if (name.equalsIgnoreCase(t.path("name").asString())) {
        return t.path("id").asString();
      }
    }
    throw new AssertionError("no Tag " + name);
  }

  protected Answer merge(Person p, String g, String primary, String other, ObjectNode extra) {
    ObjectNode r =
        obj(
            "primaryId",
            primary,
            "otherId",
            other,
            "primaryVersionNumber",
            version(p, g, primary),
            "otherVersionNumber",
            version(p, g, other));
    if (extra != null) {
      r.setAll(extra);
    }
    return post(p, "/groups/" + g + "/merges", r);
  }

  /** "number kind" of each Version, newest first. */
  protected static List<String> kinds(JsonNode history) {
    List<String> r = new ArrayList<>();
    for (JsonNode v : history) {
      r.add(v.path("number").asInt() + " " + v.path("kind").asString());
    }
    return r;
  }

  /** "item: old → new" of each change, empty values as "∅". */
  protected static List<String> changes(JsonNode version) {
    List<String> r = new ArrayList<>();
    for (JsonNode c : version.path("changes")) {
      String o = c.path("oldValue").isNull() ? "" : c.path("oldValue").asString("");
      String n = c.path("newValue").isNull() ? "" : c.path("newValue").asString("");
      r.add(
          c.path("item").asString()
              + ": "
              + (o.isEmpty() ? "∅" : o)
              + " → "
              + (n.isEmpty() ? "∅" : n));
    }
    return r;
  }

  // ------------------------------------------------- Operator, scheduler

  protected List<Map<String, Object>> audit(String g) {
    return db.queryForList(
        "SELECT user_id::text AS user_id, target_id::text AS target_id, at FROM audit_entry"
            + " WHERE group_id = ?::uuid",
        g);
  }

  protected int count(String sql, Object... args) {
    Integer n = db.queryForObject(sql, Integer.class, args);
    return n == null ? 0 : n;
  }

  protected void runScheduledJobs() {
    for (String name : context.getBeanDefinitionNames()) {
      Class<?> type = context.getType(name);
      if (type == null
          || !type.getName().startsWith("app.contacts.")
          || type.getName().startsWith("app.contacts.hidden.")) {
        continue;
      }
      Object bean = context.getBean(name);
      for (Method m : AopUtils.getTargetClass(bean).getDeclaredMethods()) {
        if (AnnotatedElementUtils.hasAnnotation(m, Scheduled.class) && m.getParameterCount() == 0) {
          try {
            bean.getClass().getMethod(m.getName()).invoke(bean);
          } catch (NoSuchMethodException e) {
            try {
              m.setAccessible(true);
              m.invoke(bean);
            } catch (IllegalAccessException | InvocationTargetException inner) {
              throw new IllegalStateException(inner);
            }
          } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
          }
        }
      }
    }
  }

  // ----------------------------------------------------------------- Clock

  /** The backend's clock (ADR-10): system time until a test sets it. */
  public static final class MovableClock extends Clock {
    private volatile Instant fixed;

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      MovableClock self = this;
      return new Clock() {
        @Override
        public ZoneId getZone() {
          return zone;
        }

        @Override
        public Clock withZone(ZoneId z) {
          return self.withZone(z);
        }

        @Override
        public Instant instant() {
          return self.instant();
        }
      };
    }

    @Override
    public Instant instant() {
      Instant f = fixed;
      return f != null ? f : Instant.now();
    }

    public Instant at(String ljubljanaLocal) {
      fixed = LocalDateTime.parse(ljubljanaLocal).atZone(LJUBLJANA).toInstant();
      return fixed;
    }

    public Instant at(Instant instant) {
      fixed = instant;
      return instant;
    }

    public void reset() {
      fixed = null;
    }
  }

  /** Registers the clock as the primary Clock bean. */
  @TestConfiguration(proxyBeanMethods = false)
  public static class ClockConfig {
    @Bean
    @Primary
    MovableClock movableClock() {
      return new MovableClock();
    }
  }
}
