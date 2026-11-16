package app.contacts.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import app.contacts.acceptance.support.AcceptanceTest;
import app.contacts.acceptance.support.AuthEmulator;
import app.contacts.acceptance.support.Json;
import app.contacts.acceptance.support.Res;
import app.contacts.acceptance.support.TestUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** US-020 Language: API part (the texts on screen are in {@code frontend/acceptance}). */
class Us020LanguageAcceptanceIT extends AcceptanceTest {

  private String language(TestUser user) {
    return Json.text(api.get(user, "/me").json().path("language"));
  }

  @Test
  @DisplayName("AC-020-01 a new User has the language Slovenian")
  void ac02001() {
    TestUser ana = ana();

    assertThat(language(ana)).isEqualTo("sl");
  }

  @Test
  @DisplayName("AC-020-02 the chosen language is kept for later sign-ins")
  void ac02002() {
    TestUser ana = ana();

    Res res = api.patch(ana, "/me", Json.obj("language", "en"));

    assertThat(res.status()).isEqualTo(200);
    assertThat(Json.text(res.json().path("language"))).isEqualTo("en");
    TestUser again = ana.withToken(auth.signIn(ANA, AuthEmulator.PASSWORD));
    assertThat(language(again)).isEqualTo("en");
  }

  @Test
  @DisplayName("EX-020-01 language de is rejected with 400 and the language is unchanged")
  void ex02001() {
    TestUser ana = ana();

    Res res = api.patch(ana, "/me", Json.obj("language", "de"));

    assertThat(res.status()).isEqualTo(400);
    assertThat(language(ana)).isEqualTo("sl");
  }
}
