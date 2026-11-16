package app.contacts.acceptance;

import static app.contacts.acceptance.support.Versions.changes;
import static app.contacts.acceptance.support.Versions.kinds;
import static org.assertj.core.api.Assertions.assertThat;

import app.contacts.acceptance.support.AcceptanceTest;
import app.contacts.acceptance.support.Json;
import app.contacts.acceptance.support.Res;
import app.contacts.acceptance.support.TestUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** US-011 Restore a Version: API part. */
class Us011RestoreVersionAcceptanceIT extends AcceptanceTest {

  private String janez(Team t, String... tags) {
    ObjectNode body =
        contactBody("firstName", "Janez", "lastName", "Novak", "email", "janez.novak@example.si");
    body.set("tags", Json.strings(tags));
    return contact(t.ana(), t.groupId(), body);
  }

  private Res restore(TestUser user, Team t, String contactId, int number, int current) {
    return api.post(
        user,
        contactPath(t.groupId(), contactId) + "/versions/" + number + "/restore",
        Json.obj("versionNumber", current));
  }

  @Test
  @DisplayName("AC-011-01 restoring Version 1 makes it current as Version 3, keeping Version 2")
  void ac01101() {
    Team t = team();
    String janez = janez(t);
    edited(t.marko(), t.groupId(), janez, field("email", "janez@novak.si"));

    Res res = restore(t.marko(), t, janez, 1, 2);

    assertThat(res.status()).isEqualTo(200);
    JsonNode contact = getContact(t.ana(), t.groupId(), janez);
    assertThat(Json.text(contact.path("fields").path("email"))).isEqualTo("janez.novak@example.si");
    JsonNode history = history(t.ana(), t.groupId(), janez);
    assertThat(kinds(history)).containsExactly("3 restored", "2 edited", "1 created");
    assertThat(history.get(0).path("restoredFrom").asInt()).isEqualTo(1);
    assertThat(changes(history.get(0)))
        .containsExactly("email: janez@novak.si → janez.novak@example.si");
  }

  @Test
  @DisplayName("AC-011-02 restoring a Version brings back a removed Tag")
  void ac01102() {
    Team t = team();
    String janez = janez(t, "udelezenec2025");
    edited(t.marko(), t.groupId(), janez, u -> u.set("tags", Json.strings()));

    restore(t.marko(), t, janez, 1, 2);

    assertThat(Json.texts(getContact(t.ana(), t.groupId(), janez).path("tags"), "name"))
        .containsExactly("udelezenec2025");
  }

  @Test
  @DisplayName("AC-011-03 a Viewer cannot restore a Version (403, unchanged)")
  void ac01103() {
    Team t = team();
    String janez = janez(t);
    edited(t.marko(), t.groupId(), janez, field("email", "janez@novak.si"));
    final String before = state(t.ana(), contactPath(t.groupId(), janez));

    Res res = restore(t.petra(), t, janez, 1, 2);

    assertProblem(res, 403, "auth.forbidden");
    assertThat(state(t.ana(), contactPath(t.groupId(), janez))).isEqualTo(before);
    assertThat(history(t.ana(), t.groupId(), janez).size()).isEqualTo(2);
  }

  @Test
  @DisplayName("AC-011-04 restoring a Version equal to the current one creates no Version")
  void ac01104() {
    Team t = team();
    String janez = janez(t);
    edited(t.marko(), t.groupId(), janez, field("email", "janez@novak.si"));
    restore(t.marko(), t, janez, 1, 2);

    Res res = restore(t.marko(), t, janez, 1, 3);

    assertProblem(res, 409, "version.sameAsCurrent");
    assertThat(history(t.ana(), t.groupId(), janez).size()).isEqualTo(3);
  }

  @Test
  @DisplayName("EX-011-01 a restore with a stale current Version number is rejected as BR-06")
  void ex01101() {
    Team t = team();
    String janez = janez(t);
    edited(t.marko(), t.groupId(), janez, field("email", "janez@novak.si"));
    edited(t.ana(), t.groupId(), janez, field("city", "Maribor"));
    final String before = state(t.ana(), contactPath(t.groupId(), janez));

    Res res = restore(t.marko(), t, janez, 1, 2);

    assertProblem(res, 409, "version.conflict");
    assertThat(Json.text(res.json().path("params").path("changedBy"))).isEqualTo("Ana Kovač");
    assertThat(state(t.ana(), contactPath(t.groupId(), janez))).isEqualTo(before);
  }

  @Test
  @DisplayName("EX-011-02 a Tag of the restored Version that is on no Contact exists again")
  void ex01102() {
    Team t = team();
    String janez = janez(t, "vip");
    edited(t.marko(), t.groupId(), janez, u -> u.set("tags", Json.strings()));

    restore(t.marko(), t, janez, 1, 2);

    assertThat(Json.texts(getContact(t.ana(), t.groupId(), janez).path("tags"), "name"))
        .containsExactly("vip");
    assertThat(tagId(t.ana(), t.groupId(), "vip")).isNotBlank();
  }

  @Test
  @DisplayName("EX-011-04 a Contact in the recycle bin cannot be restored to a Version (409)")
  void ex01104() {
    Team t = team();
    String janez = janez(t);
    edited(t.marko(), t.groupId(), janez, field("email", "janez@novak.si"));
    moveToRecycleBin(t.marko(), t.groupId(), janez);
    final String before = state(t.ana(), contactPath(t.groupId(), janez));

    Res res = restore(t.marko(), t, janez, 1, 3);

    assertProblem(res, 409, "contact.inRecycleBin");
    assertThat(state(t.ana(), contactPath(t.groupId(), janez))).isEqualTo(before);
  }
}
