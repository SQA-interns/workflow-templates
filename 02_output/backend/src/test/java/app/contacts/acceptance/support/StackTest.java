package app.contacts.acceptance.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
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

/**
 * The backend on a random port with a real PostgreSQL and the Firebase Auth emulator
 * (environments.md "test"), a settable clock (ADR-10), and checks through the Operator's interface
 * (D-26). Every test starts from an empty database and no accounts.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "APP_PUBLIC_URL=http://localhost:8080",
      "FIREBASE_PROJECT_ID=demo-contacts",
      "RATE_LIMIT_PER_MINUTE=100000",
      "LOG_LEVEL=INFO"
    })
@Import(TestClock.Config.class)
public abstract class StackTest {

  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16.15");

  static final GenericContainer<?> AUTH =
      new GenericContainer<>(
              new ImageFromDockerfile("contacts-auth-emulator-acceptance", false)
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

  @Autowired protected TestClock clock;

  @Autowired private DataSource dataSource;

  @Autowired private ApplicationContext context;

  protected Api api;
  protected AuthEmulator auth;
  protected JdbcTemplate db;

  @BeforeEach
  final void startFromEmptyState() {
    api = new Api("http://localhost:" + port + "/api/v1");
    auth = new AuthEmulator("http://" + AUTH.getHost() + ":" + AUTH.getMappedPort(9099));
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
    auth.deleteAllAccounts();
  }

  /** The User id of {@code GET /me}. */
  protected String idOf(TestUser user) {
    return Json.text(api.get(user, "/me").json().path("id"));
  }

  // ------------------------------------------------------------ Assertions

  /**
   * Asserts an RFC 9457 problem with the status and the code of specification §5, either as the
   * problem's {@code code} or as the code of one of its {@code errors[]} (code {@code validation}).
   */
  protected static void assertProblem(Res res, int status, String code) {
    assertThat(res.status()).as("status of %s", res).isEqualTo(status);
    List<String> codes = new ArrayList<>();
    codes.add(res.code());
    for (JsonNode error : res.json().path("errors")) {
      codes.add(Json.text(error.path("code")));
    }
    assertThat(codes).as("problem codes of %s", res).contains(code);
  }

  /** The answer of a GET, to compare before and after a rejected change. */
  protected String state(TestUser user, String path) {
    return api.get(user, path).toString();
  }

  /** A syntactically valid e-mail address of exactly the given length (at least 20). */
  protected static String emailOfLength(int length) {
    StringBuilder domain = new StringBuilder();
    int remaining = length - "janez.novak@".length() - ".si".length();
    while (remaining > 0) {
      int label = Math.min(remaining, 63);
      if (remaining - label == 1) {
        label -= 2;
      }
      if (domain.length() > 0) {
        domain.append('.');
        remaining--;
        label = Math.min(label, remaining);
      }
      domain.append("d".repeat(label));
      remaining -= label;
    }
    String email = "janez.novak@" + domain + ".si";
    assertThat(email).hasSize(length);
    return email;
  }

  // ----------------------------------------------------- Audit, removal job

  /** The Audit entries of a Group (the Operator's interface, D-26). */
  protected List<Map<String, Object>> auditEntries(String groupId) {
    return db.queryForList(
        "SELECT user_id::text AS user_id, action, target_id::text AS target_id, at"
            + " FROM audit_entry WHERE group_id = ?::uuid ORDER BY at",
        groupId);
  }

  /** Asserts that the Group has an Audit entry by the User at the instant, and returns it. */
  protected Map<String, Object> assertAudited(
      String groupId, TestUser actor, Instant at, List<Map<String, Object>> before) {
    List<Map<String, Object>> entries = new ArrayList<>(auditEntries(groupId));
    entries.removeAll(before);
    String actorId = idOf(actor);
    return entries.stream()
        .filter(e -> actorId.equals(e.get("user_id")))
        .filter(e -> at.equals(((java.sql.Timestamp) e.get("at")).toInstant()))
        .findFirst()
        .orElseThrow(
            () ->
                new AssertionError(
                    "no new Audit entry by " + actor.email() + " at " + at + "; new: " + entries));
  }

  /** Runs every scheduled job of the backend once, as the scheduler would (D-38). */
  protected void runScheduledJobs() {
    for (String name : context.getBeanDefinitionNames()) {
      Class<?> type = context.getType(name);
      if (type == null
          || !type.getName().startsWith("app.contacts.")
          || type.getName().startsWith("app.contacts.acceptance.")) {
        continue;
      }
      Object bean = context.getBean(name);
      for (Method method : AopUtils.getTargetClass(bean).getDeclaredMethods()) {
        if (AnnotatedElementUtils.hasAnnotation(method, Scheduled.class)
            && method.getParameterCount() == 0) {
          invoke(bean, method);
        }
      }
    }
  }

  private static void invoke(Object bean, Method method) {
    try {
      Method callable = bean.getClass().getMethod(method.getName());
      callable.invoke(bean);
    } catch (NoSuchMethodException e) {
      try {
        method.setAccessible(true);
        method.invoke(bean);
      } catch (IllegalAccessException | InvocationTargetException inner) {
        throw new IllegalStateException("scheduled job " + method + " failed", inner);
      }
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new IllegalStateException("scheduled job " + method + " failed", e);
    }
  }

  /** Number of rows of a table that refer to a Contact (persistence contract, SR-06). */
  protected int rowsOfContact(String table, String contactId) {
    Integer count =
        db.queryForObject(
            "SELECT count(*) FROM " + table + " WHERE contact_id = ?::uuid",
            Integer.class,
            contactId);
    return count == null ? 0 : count;
  }
}
