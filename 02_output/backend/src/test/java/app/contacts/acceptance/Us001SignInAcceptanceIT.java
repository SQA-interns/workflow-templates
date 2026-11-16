package app.contacts.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import app.contacts.acceptance.support.AcceptanceTest;
import app.contacts.acceptance.support.AuthEmulator;
import app.contacts.acceptance.support.Json;
import app.contacts.acceptance.support.Res;
import app.contacts.acceptance.support.TestUser;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** US-001 Sign up and sign in: API part (the screens are in {@code frontend/acceptance}). */
class Us001SignInAcceptanceIT extends AcceptanceTest {

  @Test
  @DisplayName("AC-001-01 new e-mail account: /me has the e-mail and language sl, no Groups")
  void ac00101() {
    TestUser ana = auth.verifiedUser(ANA, "Ana Kovač");

    Res me = api.get(ana, "/me");

    assertThat(me.status()).isEqualTo(200);
    assertThat(Json.text(me.json().path("email"))).isEqualTo(ANA);
    assertThat(Json.text(me.json().path("language"))).isEqualTo("sl");
    assertThat(me.json().path("emailVerified").asBoolean()).isTrue();
    assertThat(groupNames(ana)).isEmpty();
  }

  @Test
  @DisplayName("AC-001-02 Google account: /me has the Google e-mail")
  void ac00102() {
    TestUser marko = auth.googleUser(MARKO, "Marko Kranjc");

    Res me = api.get(marko, "/me");

    assertThat(me.status()).isEqualTo(200);
    assertThat(Json.text(me.json().path("email"))).isEqualTo(MARKO);
    assertThat(api.get(marko, "/groups").status()).isEqualTo(200);
  }

  @Test
  @DisplayName("AC-001-03 GET /groups without an ID token is 401 with no Group data")
  void ac00103() {
    TestUser ana = ana();
    String groupId = group(ana, "Konferenca 2025");

    Res res = api.send("GET", "/groups", null, null);

    assertThat(res.status()).isEqualTo(401);
    assertThat(res.body()).doesNotContain("Konferenca 2025").doesNotContain(groupId);
  }

  @Test
  @DisplayName("AC-001-04 GET /groups with an expired ID token is 401 with no Group data")
  void ac00104() {
    TestUser ana = ana();
    String groupId = group(ana, "Konferenca 2025");
    long hourAgo = Instant.now().getEpochSecond() - 3600;
    String expired =
        AuthEmulator.withClaims(
            ana.token(),
            claims -> {
              claims.put("iat", hourAgo - 3600);
              claims.put("auth_time", hourAgo - 3600);
              claims.put("exp", hourAgo);
            });

    Res res = api.send("GET", "/groups", expired, null);

    assertThat(res.status()).isEqualTo(401);
    assertThat(res.body()).doesNotContain("Konferenca 2025").doesNotContain(groupId);
  }

  @Test
  @DisplayName("AC-001-05 unverified e-mail: GET /groups is 403, only GET /me is allowed")
  void ac00105() {
    TestUser ana = auth.unverifiedUser(ANA);

    Res groups = api.get(ana, "/groups");
    Res me = api.get(ana, "/me");

    assertThat(groups.status()).isEqualTo(403);
    assertThat(groups.code()).isEqualTo("auth.emailNotVerified");
    assertThat(me.status()).isEqualTo(200);
    assertThat(me.json().path("emailVerified").asBoolean()).isFalse();
    assertThat(api.post(ana, "/groups", Json.obj("name", "Konferenca 2025")).status())
        .isEqualTo(403);
  }

  @Test
  @DisplayName("AC-001-06 after sign-out a request with no token is 401")
  void ac00106() {
    TestUser ana = ana();
    group(ana, "Konferenca 2025");

    Res res = api.send("GET", "/groups", null, null);

    assertThat(res.status()).isEqualTo(401);
    assertThat(res.body()).doesNotContain("Konferenca 2025");
  }

  @Test
  @DisplayName("EX-001-02 ID token issued for another Firebase project is 401 with no data")
  void ex00102() {
    TestUser ana = ana();
    group(ana, "Konferenca 2025");
    String foreign =
        AuthEmulator.withClaims(
            ana.token(),
            claims -> {
              claims.put("aud", "another-project");
              claims.put("iss", "https://securetoken.google.com/another-project");
            });

    Res res = api.send("GET", "/groups", foreign, null);

    assertThat(res.status()).isEqualTo(401);
    assertThat(res.body()).doesNotContain("Konferenca 2025");
  }

  @Test
  @DisplayName("EX-001-03 signing in a second time returns the same User")
  void ex00103() {
    TestUser ana = ana();
    String firstId = idOf(ana);
    TestUser again = ana.withToken(auth.signIn(ANA, AuthEmulator.PASSWORD));

    JsonNode me = api.get(again, "/me").json();

    assertThat(Json.text(me.path("id"))).isEqualTo(firstId);
    assertThat(
            db.queryForObject(
                "SELECT count(*) FROM app_user WHERE lower(email) = ?", Integer.class, ANA))
        .isEqualTo(1);
  }

  @Test
  @DisplayName("EX-001-04 tampered ID token (one character changed) is 401 with no data (D-36)")
  void ex00104() {
    TestUser ana = ana();
    group(ana, "Konferenca 2025");

    Res res = api.send("GET", "/groups", AuthEmulator.tampered(ana.token()), null);

    assertThat(res.status()).isEqualTo(401);
    assertThat(res.body()).doesNotContain("Konferenca 2025");
  }
}
