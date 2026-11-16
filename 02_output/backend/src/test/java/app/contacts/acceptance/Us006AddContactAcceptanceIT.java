package app.contacts.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import app.contacts.acceptance.support.AcceptanceTest;
import app.contacts.acceptance.support.Json;
import app.contacts.acceptance.support.Res;
import app.contacts.acceptance.support.TestUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** US-006 Add a Contact: API part. */
class Us006AddContactAcceptanceIT extends AcceptanceTest {

  private Res post(TestUser user, Team t, String... fieldPairs) {
    return api.post(user, t.path() + "/contacts", contactBody(fieldPairs));
  }

  private int total(Team t) {
    return list(t.ana(), t.groupId(), "").path("total").asInt();
  }

  @Test
  @DisplayName("AC-006-01 an Editor adds a Contact with all Fields; Version 1 created by him")
  void ac00601() {
    Team t = team();

    Res res =
        post(
            t.marko(),
            t,
            "firstName",
            "Janez",
            "lastName",
            "Novak",
            "email",
            "janez.novak@example.si",
            "phone",
            "+386 41 123 456",
            "company",
            "Podjetje d.o.o.",
            "jobTitle",
            "Direktor",
            "street",
            "Slovenska cesta 1",
            "postalCode",
            "1000",
            "city",
            "Ljubljana",
            "country",
            "SI",
            "note",
            "Govornik");

    assertThat(res.status()).isEqualTo(201);
    JsonNode fields = res.json().path("fields");
    assertThat(Json.text(fields.path("jobTitle"))).isEqualTo("Direktor");
    assertThat(Json.text(fields.path("country"))).isEqualTo("SI");
    assertThat(Json.text(fields.path("note"))).isEqualTo("Govornik");
    assertThat(res.json().path("versionNumber").asInt()).isEqualTo(1);
    assertThat(names(list(t.petra(), t.groupId(), ""))).containsExactly("Janez Novak");
    JsonNode history = history(t.petra(), t.groupId(), Json.text(res.json().path("id")));
    assertThat(history.size()).isEqualTo(1);
    assertThat(history.get(0).path("number").asInt()).isEqualTo(1);
    assertThat(Json.text(history.get(0).path("kind"))).isEqualTo("created");
    assertThat(Json.text(history.get(0).path("changedBy"))).isEqualTo("Marko Kranjc");
  }

  @Test
  @DisplayName("AC-006-02 a Contact with only a company is created")
  void ac00602() {
    Team t = team();

    Res res = post(t.marko(), t, "company", "Podjetje d.o.o.");

    assertThat(res.status()).isEqualTo(201);
    assertThat(names(list(t.ana(), t.groupId(), ""))).containsExactly("Podjetje d.o.o.");
  }

  @Test
  @DisplayName("AC-006-03 a Contact with only an e-mail is rejected and nothing stored")
  void ac00603() {
    Team t = team();

    Res res = post(t.marko(), t, "email", "info@example.si");

    assertProblem(res, 400, "contact.nameRequired");
    assertThat(total(t)).isZero();
  }

  @Test
  @DisplayName("AC-006-04 an invalid e-mail is rejected and nothing stored")
  void ac00604() {
    Team t = team();

    Res res = post(t.marko(), t, "firstName", "Janez", "email", "janez@");

    assertProblem(res, 400, "field.emailInvalid");
    assertThat(total(t)).isZero();
  }

  @Test
  @DisplayName("AC-006-05 an e-mail of an existing Contact is reported and saving still works")
  void ac00605() {
    Team t = team();
    String janez =
        contact(
            t.marko(),
            t.groupId(),
            "firstName",
            "Janez",
            "lastName",
            "Novak",
            "email",
            "janez.novak@example.si");

    Res matches =
        api.get(t.marko(), t.path() + "/contacts/email-matches?email=Janez.Novak@example.si");
    Res created =
        post(
            t.marko(),
            t,
            "firstName",
            "J.",
            "lastName",
            "Novak",
            "email",
            "Janez.Novak@example.si");

    assertThat(matches.status()).isEqualTo(200);
    assertThat(Json.texts(matches.json(), "id")).containsExactly(janez);
    assertThat(Json.texts(matches.json(), "name")).containsExactly("Janez Novak");
    assertThat(created.status()).isEqualTo(201);
    assertThat(total(t)).isEqualTo(2);
  }

  @Test
  @DisplayName("AC-006-06 a Viewer cannot add a Contact (403, nothing stored)")
  void ac00606() {
    Team t = team();

    Res res = post(t.petra(), t, "firstName", "Janez", "lastName", "Novak");

    assertProblem(res, 403, "auth.forbidden");
    assertThat(total(t)).isZero();
  }

  @Test
  @DisplayName("EX-006-01 a first name of 100 characters is saved")
  void ex00601() {
    Team t = team();

    Res res = post(t.marko(), t, "firstName", "J".repeat(100));

    assertThat(res.status()).isEqualTo(201);
    assertThat(Json.text(res.json().path("fields").path("firstName"))).hasSize(100);
  }

  @Test
  @DisplayName("EX-006-02 a first name of 101 characters is rejected and nothing stored")
  void ex00602() {
    Team t = team();

    Res res = post(t.marko(), t, "firstName", "J".repeat(101));

    assertProblem(res, 400, "field.tooLong");
    assertThat(total(t)).isZero();
  }

  @Test
  @DisplayName("EX-006-03 a note of 2000 characters is saved, of 2001 rejected")
  void ex00603() {
    Team t = team();

    Res ok = post(t.marko(), t, "firstName", "Janez", "note", "n".repeat(2000));
    Res tooLong = post(t.marko(), t, "firstName", "Maja", "note", "n".repeat(2001));

    assertThat(ok.status()).isEqualTo(201);
    assertProblem(tooLong, 400, "field.tooLong");
    assertThat(names(list(t.ana(), t.groupId(), ""))).containsExactly("Janez");
  }

  @Test
  @DisplayName("EX-006-04 an e-mail of 254 characters is saved, of 255 rejected")
  void ex00604() {
    Team t = team();

    Res ok = post(t.marko(), t, "firstName", "Janez", "email", emailOfLength(254));
    Res tooLong = post(t.marko(), t, "firstName", "Maja", "email", emailOfLength(255));

    assertThat(ok.status()).isEqualTo(201);
    assertProblem(tooLong, 400, "field.tooLong");
    assertThat(names(list(t.ana(), t.groupId(), ""))).containsExactly("Janez");
  }

  @Test
  @DisplayName("EX-006-05 name parts of only spaces are rejected")
  void ex00605() {
    Team t = team();

    Res res = post(t.marko(), t, "firstName", "   ", "lastName", "", "company", "  ");

    assertProblem(res, 400, "contact.nameRequired");
    assertThat(total(t)).isZero();
  }

  @Test
  @DisplayName("EX-006-06 country XX is rejected with 400 and nothing stored")
  void ex00606() {
    Team t = team();

    Res res = post(t.marko(), t, "firstName", "Janez", "country", "XX");

    assertThat(res.status()).isEqualTo(400);
    assertThat(total(t)).isZero();
  }

  @Test
  @DisplayName("EX-006-07 leading and trailing spaces are trimmed")
  void ex00607() {
    Team t = team();

    Res res = post(t.marko(), t, "firstName", "  Janez ");

    assertThat(Json.text(res.json().path("fields").path("firstName"))).isEqualTo("Janez");
    String id = Json.text(res.json().path("id"));
    assertThat(Json.text(getContact(t.ana(), t.groupId(), id).path("fields").path("firstName")))
        .isEqualTo("Janez");
  }

  @Test
  @DisplayName("EX-006-08 diacritics are stored and shown as entered")
  void ex00608() {
    Team t = team();

    String id = contact(t.marko(), t.groupId(), "firstName", "Žiga", "lastName", "Čuk");

    JsonNode fields = getContact(t.ana(), t.groupId(), id).path("fields");
    assertThat(Json.text(fields.path("firstName"))).isEqualTo("Žiga");
    assertThat(Json.text(fields.path("lastName"))).isEqualTo("Čuk");
    assertThat(names(list(t.ana(), t.groupId(), ""))).containsExactly("Žiga Čuk");
  }

  @Test
  @DisplayName("EX-006-09 a Contact posted to a Group of which he is no Member is 404")
  void ex00609() {
    Team t = team();
    TestUser outsider = user("nina.kos@example.si", "Nina Kos");

    Res res = post(outsider, t, "firstName", "Janez", "lastName", "Novak");

    assertThat(res.status()).isEqualTo(404);
    assertThat(total(t)).isZero();
  }
}
