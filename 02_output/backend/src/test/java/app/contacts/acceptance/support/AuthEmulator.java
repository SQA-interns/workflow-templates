package app.contacts.acceptance.support;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.function.Consumer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * The Firebase Auth emulator, the local substitute of Firebase Authentication (environments.md),
 * through its REST interface. Project {@code demo-contacts}.
 */
public final class AuthEmulator {

  /** Password of every test account (seed-local.sh, AC-001-01). */
  public static final String PASSWORD = "Kontakti#2026";

  private static final String PROJECT = "demo-contacts";
  private static final String KEY = "?key=fake-api-key";

  private final HttpClient http = HttpClient.newHttpClient();
  private final String base;

  /** The emulator at the base URL, e.g. {@code http://localhost:9099}. */
  public AuthEmulator(String base) {
    this.base = base;
  }

  /** Creates an e-mail/password account with a verified e-mail and signs in. */
  public TestUser verifiedUser(String email, String displayName) {
    JsonNode created = signUp(email, PASSWORD);
    String uid = Json.text(created.path("localId"));
    admin(
        "accounts:update",
        Json.obj("localId", uid, "emailVerified", true, "displayName", displayName));
    return new TestUser(email, PASSWORD, uid, displayName, signIn(email, PASSWORD));
  }

  /** Creates an e-mail/password account whose e-mail is not verified, signed in. */
  public TestUser unverifiedUser(String email) {
    JsonNode created = signUp(email, PASSWORD);
    return new TestUser(
        email,
        PASSWORD,
        Json.text(created.path("localId")),
        null,
        Json.text(created.path("idToken")));
  }

  /** Signs in with a (fake) Google account, as the emulator allows. */
  public TestUser googleUser(String email, String displayName) {
    String claims =
        Json.write(
            Json.obj(
                "sub",
                "google-" + email,
                "email",
                email,
                "email_verified",
                true,
                "name",
                displayName));
    JsonNode result =
        call(
            "POST",
            "/identitytoolkit.googleapis.com/v1/accounts:signInWithIdp" + KEY,
            null,
            Json.obj(
                "postBody",
                "id_token="
                    + URLEncoder.encode(claims, StandardCharsets.UTF_8)
                    + "&providerId=google.com",
                "requestUri",
                "http://localhost",
                "returnIdpCredential",
                true,
                "returnSecureToken",
                true));
    return new TestUser(
        email,
        null,
        Json.text(result.path("localId")),
        displayName,
        Json.text(result.path("idToken")));
  }

  /** Creates an account; returns the emulator's answer (localId, idToken). */
  public JsonNode signUp(String email, String password) {
    return call(
        "POST",
        "/identitytoolkit.googleapis.com/v1/accounts:signUp" + KEY,
        null,
        Json.obj("email", email, "password", password, "returnSecureToken", true));
  }

  /** Signs in with e-mail and password; returns a fresh ID token. */
  public String signIn(String email, String password) {
    JsonNode result =
        call(
            "POST",
            "/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword" + KEY,
            null,
            Json.obj("email", email, "password", password, "returnSecureToken", true));
    return Json.text(result.path("idToken"));
  }

  /** Marks the e-mail of an account verified (as the link in the e-mail would). */
  public void verifyEmail(String uid) {
    admin("accounts:update", Json.obj("localId", uid, "emailVerified", true));
  }

  /** Number of accounts with this e-mail. */
  public int accountsWithEmail(String email) {
    JsonNode result = admin("accounts:lookup", Json.obj("email", Json.strings(email)));
    return result.path("users").size();
  }

  /** Deletes every account. */
  public void deleteAllAccounts() {
    call("DELETE", "/emulator/v1/projects/" + PROJECT + "/accounts", "owner", null);
  }

  /** The out-of-band codes (e-mail links) the emulator "sent". */
  public JsonNode oobCodes() {
    return call("GET", "/emulator/v1/projects/" + PROJECT + "/oobCodes", null, null)
        .path("oobCodes");
  }

  /** A token with the same claims as the given one, changed by the editor, unsigned. */
  public static String withClaims(String token, Consumer<ObjectNode> editor) {
    String[] parts = token.split("\\.", -1);
    Base64.Decoder decoder = Base64.getUrlDecoder();
    ObjectNode claims =
        (ObjectNode) Json.parse(new String(decoder.decode(parts[1]), StandardCharsets.UTF_8));
    editor.accept(claims);
    Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
    return parts[0]
        + "."
        + encoder.encodeToString(Json.write(claims).getBytes(StandardCharsets.UTF_8))
        + "."
        + (parts.length > 2 ? parts[2] : "");
  }

  /**
   * The token with one character of its payload changed, so that it no longer decodes to valid
   * claims (D-36).
   */
  public static String tampered(String token) {
    int payloadStart = token.indexOf('.') + 1;
    char first = token.charAt(payloadStart);
    char replacement = first == 'A' ? 'B' : 'A';
    return token.substring(0, payloadStart) + replacement + token.substring(payloadStart + 1);
  }

  private JsonNode admin(String action, JsonNode body) {
    return call(
        "POST",
        "/identitytoolkit.googleapis.com/v1/projects/" + PROJECT + "/" + action,
        "owner",
        body);
  }

  private JsonNode call(String method, String path, String bearer, JsonNode body) {
    HttpRequest.Builder request =
        HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(30));
    if (bearer != null) {
      request.header("Authorization", "Bearer " + bearer);
    }
    request.header("Content-Type", "application/json");
    request.method(
        method,
        body == null
            ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofString(Json.write(body)));
    try {
      HttpResponse<String> response =
          http.send(request.build(), HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() >= 300) {
        throw new IllegalStateException(
            "Auth emulator "
                + path
                + " answered "
                + response.statusCode()
                + ": "
                + response.body());
      }
      return Json.parse(response.body());
    } catch (IOException e) {
      throw new IllegalStateException("Auth emulator not reachable: " + path, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("interrupted", e);
    }
  }
}
