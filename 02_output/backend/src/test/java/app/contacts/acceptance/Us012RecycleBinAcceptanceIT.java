package app.contacts.acceptance;

import static app.contacts.acceptance.support.Versions.kinds;
import static org.assertj.core.api.Assertions.assertThat;

import app.contacts.acceptance.support.AcceptanceTest;
import app.contacts.acceptance.support.Json;
import app.contacts.acceptance.support.Res;
import app.contacts.acceptance.support.TestUser;
import app.contacts.acceptance.support.Xlsx;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** US-012 Recycle bin for Contacts: API part. */
class Us012RecycleBinAcceptanceIT extends AcceptanceTest {

  private static final Duration PERIOD = Duration.ofDays(30);

  /** "Janez Novak" with Tag and Attribute, at Version 3. */
  private String janezAtVersion3(Team t) {
    ObjectNode body =
        contactBody("firstName", "Janez", "lastName", "Novak", "email", "janez.novak@example.si");
    body.set("tags", Json.strings("udelezenec2025"));
    body.set("attributes", Json.attributes("kompetenca", "spletnoProgramiranje"));
    String id = contact(t.ana(), t.groupId(), body);
    edited(t.marko(), t.groupId(), id, field("city", "Ljubljana"));
    edited(t.marko(), t.groupId(), id, field("phone", "+386 41 123 456"));
    return id;
  }

  private Res move(TestUser user, Team t, String contactId, int versionNumber) {
    return api.post(
        user,
        contactPath(t.groupId(), contactId) + "/move-to-recycle-bin",
        Json.obj("versionNumber", versionNumber));
  }

  private Res restore(TestUser user, Team t, String contactId, int versionNumber) {
    return api.post(
        user,
        t.path() + "/recycle-bin/" + contactId + "/restore",
        Json.obj("versionNumber", versionNumber));
  }

  private JsonNode bin(TestUser user, Team t) {
    Res res = api.get(user, t.path() + "/recycle-bin");
    assertThat(res.status()).as("recycle bin: %s", res).isEqualTo(200);
    return res.json();
  }

  @Test
  @DisplayName(
      "AC-012-01 moved to the recycle bin: gone from the list, listed in the bin, Version 4")
  void ac01201() {
    Team t = team();
    String janez = janezAtVersion3(t);
    final Instant now = clock.setLjubljana("2026-03-05T11:00");

    Res res = move(t.marko(), t, janez, 3);

    assertThat(res.status()).isEqualTo(200);
    assertThat(names(list(t.marko(), t.groupId(), ""))).doesNotContain("Janez Novak");
    JsonNode entries = bin(t.marko(), t);
    assertThat(entries.size()).isEqualTo(1);
    JsonNode entry = entries.get(0);
    assertThat(Json.text(entry.path("contact").path("name"))).isEqualTo("Janez Novak");
    assertThat(Json.text(entry.path("deletedBy"))).isEqualTo("Marko Kranjc");
    assertThat(Instant.parse(Json.text(entry.path("deletedAt")))).isEqualTo(now);
    assertThat(Instant.parse(Json.text(entry.path("removalAt"))))
        .isEqualTo(Instant.parse("2026-04-04T10:00:00Z"));
    assertThat(kinds(history(t.marko(), t.groupId(), janez)).get(0))
        .isEqualTo("4 movedToRecycleBin");
  }

  @Test
  @DisplayName("AC-012-02 restored from the recycle bin with Tags and Attributes, Version 5")
  void ac01202() {
    Team t = team();
    String janez = janezAtVersion3(t);
    clock.setLjubljana("2026-03-05T11:00");
    move(t.marko(), t, janez, 3);
    clock.setLjubljana("2026-03-20T09:00");

    Res res = restore(t.ana(), t, janez, 4);

    assertThat(res.status()).isEqualTo(200);
    assertThat(names(list(t.ana(), t.groupId(), ""))).containsExactly("Janez Novak");
    JsonNode contact = getContact(t.ana(), t.groupId(), janez);
    assertThat(Json.texts(contact.path("tags"), "name")).containsExactly("udelezenec2025");
    assertThat(Json.texts(contact.path("attributes"), "value"))
        .containsExactly("spletnoProgramiranje");
    assertThat(Json.text(contact.path("fields").path("phone"))).isEqualTo("+386 41 123 456");
    assertThat(kinds(history(t.ana(), t.groupId(), janez)).get(0))
        .isEqualTo("5 restoredFromRecycleBin");
    assertThat(bin(t.ana(), t).size()).isZero();
  }

  @Test
  @DisplayName("AC-012-03 30 days and 1 hour after the move the Contact no longer exists")
  void ac01203() {
    Team t = team();
    String janez = janezAtVersion3(t);
    Instant moved = clock.setLjubljana("2026-03-05T11:00");
    move(t.marko(), t, janez, 3);

    clock.set(moved.plus(PERIOD).plus(Duration.ofHours(1)));
    runScheduledJobs();

    assertThat(bin(t.ana(), t).size()).isZero();
    assertThat(api.get(t.ana(), contactPath(t.groupId(), janez)).status()).isEqualTo(404);
  }

  @Test
  @DisplayName("AC-012-04 a Viewer cannot move a Contact to the recycle bin (403, unchanged)")
  void ac01204() {
    Team t = team();
    String janez = janezAtVersion3(t);
    final String before = state(t.ana(), contactPath(t.groupId(), janez));

    Res res = move(t.petra(), t, janez, 3);

    assertProblem(res, 403, "auth.forbidden");
    assertThat(state(t.ana(), contactPath(t.groupId(), janez))).isEqualTo(before);
    assertThat(names(list(t.ana(), t.groupId(), ""))).containsExactly("Janez Novak");
  }

  @Test
  @DisplayName("AC-012-05 a Viewer cannot open the recycle bin (403)")
  void ac01205() {
    Team t = team();
    String janez = janezAtVersion3(t);
    move(t.marko(), t, janez, 3);

    Res res = api.get(t.petra(), t.path() + "/recycle-bin");

    assertProblem(res, 403, "auth.forbidden");
    assertThat(res.body()).doesNotContain("Novak");
  }

  @Test
  @DisplayName("EX-012-01 restore one minute before the removal date succeeds")
  void ex01201() {
    Team t = team();
    String janez = janezAtVersion3(t);
    Instant moved = clock.setLjubljana("2026-03-05T11:00");
    move(t.marko(), t, janez, 3);
    clock.set(moved.plus(PERIOD).minus(Duration.ofMinutes(1)));

    Res res = restore(t.marko(), t, janez, 4);

    assertThat(res.status()).isEqualTo(200);
    assertThat(names(list(t.ana(), t.groupId(), ""))).containsExactly("Janez Novak");
  }

  @Test
  @DisplayName("EX-012-02 restore exactly at the removal date is rejected, nothing changes")
  void ex01202() {
    Team t = team();
    String janez = janezAtVersion3(t);
    Instant moved = clock.setLjubljana("2026-03-05T11:00");
    move(t.marko(), t, janez, 3);
    clock.set(moved.plus(PERIOD));
    final String before = state(t.ana(), t.path() + "/recycle-bin");

    Res res = restore(t.marko(), t, janez, 4);

    assertProblem(res, 409, "contact.noLongerRestorable");
    assertThat(state(t.ana(), t.path() + "/recycle-bin")).isEqualTo(before);
    assertThat(names(list(t.ana(), t.groupId(), ""))).isEmpty();
  }

  @Test
  @DisplayName("EX-012-04 a move with a stale Version number is rejected as BR-06")
  void ex01204() {
    Team t = team();
    String janez = janezAtVersion3(t);
    final String before = state(t.ana(), contactPath(t.groupId(), janez));

    Res res = move(t.marko(), t, janez, 2);

    assertProblem(res, 409, "version.conflict");
    assertThat(state(t.ana(), contactPath(t.groupId(), janez))).isEqualTo(before);
    assertThat(bin(t.ana(), t).size()).isZero();
  }

  @Test
  @DisplayName("EX-012-05 a Contact in the recycle bin is not in search, filters or Export")
  void ex01205() {
    Team t = team();
    String janez = janezAtVersion3(t);
    contact(t.ana(), t.groupId(), "firstName", "Maja", "lastName", "Zupan");
    String tag = tagId(t.ana(), t.groupId(), "udelezenec2025");
    move(t.marko(), t, janez, 3);

    assertThat(list(t.ana(), t.groupId(), "q=novak").path("total").asInt()).isZero();
    assertThat(list(t.ana(), t.groupId(), "hasTagIds=" + tag).path("total").asInt()).isZero();
    Res export = api.post(t.ana(), t.path() + "/exports", Json.obj());
    assertThat(export.status()).isEqualTo(200);
    Xlsx file = Xlsx.read(export.bytes());
    assertThat(file.rows()).hasSize(1);
    assertThat(file.rows().get(0)).contains("Maja", "Zupan").doesNotContain("Janez");
  }

  @Test
  @DisplayName("EX-012-06 moving a Contact that is already in the recycle bin is 409")
  void ex01206() {
    Team t = team();
    String janez = janezAtVersion3(t);
    move(t.marko(), t, janez, 3);
    final String before = state(t.ana(), t.path() + "/recycle-bin");

    Res res = move(t.marko(), t, janez, 4);

    assertThat(res.status()).isEqualTo(409);
    assertThat(state(t.ana(), t.path() + "/recycle-bin")).isEqualTo(before);
  }
}
