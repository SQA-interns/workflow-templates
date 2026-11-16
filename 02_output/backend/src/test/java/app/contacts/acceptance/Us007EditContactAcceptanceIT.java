package app.contacts.acceptance;

import static app.contacts.acceptance.support.Versions.changes;
import static app.contacts.acceptance.support.Versions.orEmpty;
import static org.assertj.core.api.Assertions.assertThat;

import app.contacts.acceptance.support.AcceptanceTest;
import app.contacts.acceptance.support.Json;
import app.contacts.acceptance.support.Res;
import app.contacts.acceptance.support.TestUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** US-007 Edit a Contact: API part. */
class Us007EditContactAcceptanceIT extends AcceptanceTest {

  private String janez(Team t) {
    return contact(
        t.ana(),
        t.groupId(),
        "firstName",
        "Janez",
        "lastName",
        "Novak",
        "email",
        "janez.novak@example.si");
  }

  @Test
  @DisplayName("AC-007-01 an Editor changes the e-mail; Version 2 edited lists old and new")
  void ac00701() {
    Team t = team();
    String janez = janez(t);

    Res res = edit(t.marko(), t.groupId(), janez, field("email", "janez@novak.si"));

    assertThat(res.status()).isEqualTo(200);
    assertThat(Json.text(getContact(t.petra(), t.groupId(), janez).path("fields").path("email")))
        .isEqualTo("janez@novak.si");
    JsonNode latest = history(t.petra(), t.groupId(), janez).get(0);
    assertThat(latest.path("number").asInt()).isEqualTo(2);
    assertThat(Json.text(latest.path("kind"))).isEqualTo("edited");
    assertThat(Json.text(latest.path("changedBy"))).isEqualTo("Marko Kranjc");
    assertThat(changes(latest)).containsExactly("email: janez.novak@example.si → janez@novak.si");
  }

  @Test
  @DisplayName("AC-007-02 a save on a stale Version is rejected with the name of who changed it")
  void ac00702() {
    Team t = team();
    String janez = janez(t);
    edited(t.ana(), t.groupId(), janez, field("phone", "+386 41 123 456"));
    ObjectNode anasForm = updateOf(getContact(t.ana(), t.groupId(), janez));
    edited(t.marko(), t.groupId(), janez, field("city", "Maribor"));
    ((ObjectNode) anasForm.path("fields")).put("jobTitle", "CEO");

    Res res = api.put(t.ana(), contactPath(t.groupId(), janez), anasForm);

    assertProblem(res, 409, "version.conflict");
    assertThat(Json.text(res.json().path("params").path("changedBy"))).isEqualTo("Marko Kranjc");
    JsonNode now = getContact(t.ana(), t.groupId(), janez);
    assertThat(orEmpty(Json.text(now.path("fields").path("jobTitle")))).isEqualTo("∅");
    assertThat(now.path("versionNumber").asInt()).isEqualTo(3);
    assertThat(history(t.ana(), t.groupId(), janez).size()).isEqualTo(3);
  }

  @Test
  @DisplayName("AC-007-03 clearing every name part is rejected, the Contact unchanged")
  void ac00703() {
    Team t = team();
    String janez = janez(t);
    final String before = state(t.ana(), contactPath(t.groupId(), janez));

    Res res =
        edit(t.marko(), t.groupId(), janez, field("firstName", "").andThen(field("lastName", "")));

    assertProblem(res, 400, "contact.nameRequired");
    assertThat(state(t.ana(), contactPath(t.groupId(), janez))).isEqualTo(before);
  }

  @Test
  @DisplayName("AC-007-04 a save without changes creates no Version")
  void ac00704() {
    Team t = team();
    String janez = janez(t);
    edited(t.marko(), t.groupId(), janez, field("city", "Maribor"));

    Res res = edit(t.marko(), t.groupId(), janez, update -> {});

    assertProblem(res, 409, "contact.noChanges");
    assertThat(history(t.ana(), t.groupId(), janez).get(0).path("number").asInt()).isEqualTo(2);
  }

  @Test
  @DisplayName("AC-007-05 a Viewer cannot change a Contact (403, unchanged)")
  void ac00705() {
    Team t = team();
    String janez = janez(t);
    final String before = state(t.ana(), contactPath(t.groupId(), janez));

    Res res = edit(t.petra(), t.groupId(), janez, field("city", "Maribor"));

    assertProblem(res, 403, "auth.forbidden");
    assertThat(state(t.ana(), contactPath(t.groupId(), janez))).isEqualTo(before);
  }

  @Test
  @DisplayName("EX-007-01 two Fields changed in one save give one Version with both items")
  void ex00701() {
    Team t = team();
    String janez = janez(t);

    edited(
        t.marko(), t.groupId(), janez, field("city", "Maribor").andThen(field("jobTitle", "CEO")));

    JsonNode history = history(t.ana(), t.groupId(), janez);
    assertThat(history.size()).isEqualTo(2);
    assertThat(changes(history.get(0)))
        .containsExactlyInAnyOrder("city: ∅ → Maribor", "jobTitle: ∅ → CEO");
  }

  @Test
  @DisplayName("EX-007-02 a value changed only by spaces is no change")
  void ex00702() {
    Team t = team();
    String janez = janez(t);

    Res res = edit(t.marko(), t.groupId(), janez, field("firstName", "Janez "));

    assertProblem(res, 409, "contact.noChanges");
    assertThat(history(t.ana(), t.groupId(), janez).size()).isEqualTo(1);
  }

  @Test
  @DisplayName("EX-007-03 a Contact in the recycle bin cannot be edited (409, unchanged)")
  void ex00703() {
    Team t = team();
    String janez = janez(t);
    ObjectNode form = updateOf(getContact(t.marko(), t.groupId(), janez));
    moveToRecycleBin(t.marko(), t.groupId(), janez);
    form.put("versionNumber", 2);
    ((ObjectNode) form.path("fields")).put("city", "Maribor");
    final String before = state(t.ana(), contactPath(t.groupId(), janez));

    Res res = api.put(t.marko(), contactPath(t.groupId(), janez), form);

    assertProblem(res, 409, "contact.inRecycleBin");
    assertThat(state(t.ana(), contactPath(t.groupId(), janez))).isEqualTo(before);
  }

  @Test
  @DisplayName("EX-007-04 a Version number higher than the current one is rejected as BR-06")
  void ex00704() {
    Team t = team();
    String janez = janez(t);
    final String before = state(t.ana(), contactPath(t.groupId(), janez));

    Res res =
        edit(
            t.marko(),
            t.groupId(),
            janez,
            field("city", "Maribor").andThen(update -> update.put("versionNumber", 5)));

    assertProblem(res, 409, "version.conflict");
    assertThat(state(t.ana(), contactPath(t.groupId(), janez))).isEqualTo(before);
  }

  @Test
  @DisplayName("EX-007-05 editing a Contact of another Group is 404")
  void ex00705() {
    Team t = team();
    TestUser nina = user("nina.kos@example.si", "Nina Kos");
    String other = group(nina, "Podjetje Beta");
    String maja = contact(nina, other, "firstName", "Maja", "lastName", "Zupan");
    ObjectNode form = updateOf(getContact(nina, other, maja));
    ((ObjectNode) form.path("fields")).put("city", "Maribor");
    final String before = state(nina, contactPath(other, maja));

    Res direct = api.put(t.marko(), contactPath(other, maja), form);
    Res throughOwnGroup = api.put(t.marko(), contactPath(t.groupId(), maja), form);

    assertThat(direct.status()).isEqualTo(404);
    assertThat(throughOwnGroup.status()).isEqualTo(404);
    assertThat(state(nina, contactPath(other, maja))).isEqualTo(before);
  }
}
