package app.contacts.acceptance;

import static app.contacts.acceptance.support.Versions.kinds;
import static org.assertj.core.api.Assertions.assertThat;

import app.contacts.acceptance.support.AcceptanceTest;
import app.contacts.acceptance.support.Json;
import app.contacts.acceptance.support.Res;
import app.contacts.acceptance.support.TestUser;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** US-019 Undo a Merge: API part. */
class Us019UndoMergeAcceptanceIT extends AcceptanceTest {

  /**
   * As AC-018-01: "J. Novak" merged into "Janez Novak" (Version 4 merged), phone of J. Novak kept.
   *
   * @return Janez id, J. Novak id, Merge id
   */
  private String[] merged(Team t) {
    ObjectNode janez =
        contactBody("firstName", "Janez", "lastName", "Novak", "email", "janez.novak@example.si");
    janez.set("tags", Json.strings("udelezenec2025"));
    String first = contact(t.ana(), t.groupId(), janez);
    edited(t.marko(), t.groupId(), first, field("city", "Ljubljana"));
    edited(t.marko(), t.groupId(), first, field("city", "Maribor"));
    ObjectNode other =
        contactBody(
            "firstName", "J.",
            "lastName", "Novak",
            "email", "janez.novak@example.si",
            "phone", "+386 41 123 456");
    other.set("tags", Json.strings("vip"));
    String second = contact(t.ana(), t.groupId(), other);
    Res res =
        merge(
            t.marko(),
            t.groupId(),
            first,
            second,
            Json.obj("fieldChoices", Json.obj("phone", "other")));
    assertThat(res.status()).as("merge: %s", res).isEqualTo(201);
    return new String[] {first, second, Json.text(res.json().path("id"))};
  }

  private Res undo(TestUser user, Team t, String mergeId, int versionNumber) {
    return api.post(
        user, t.path() + "/merges/" + mergeId + "/undo", Json.obj("versionNumber", versionNumber));
  }

  @Test
  @DisplayName("AC-019-01 Undo merge restores both Contacts with a mergeUndone Version each")
  void ac01901() {
    Team t = team();
    String[] ids = merged(t);
    final List<Map<String, Object>> before = auditEntries(t.groupId());
    final Instant now = clock.setLjubljana("2026-03-06T10:00");

    Res res = undo(t.marko(), t, ids[2], 4);

    assertThat(res.status()).isEqualTo(200);
    JsonNode janez = getContact(t.ana(), t.groupId(), ids[0]);
    String phone = Json.text(janez.path("fields").path("phone"));
    assertThat(phone == null || phone.isEmpty()).as("phone %s", phone).isTrue();
    assertThat(Json.texts(janez.path("tags"), "name")).containsExactly("udelezenec2025");
    assertThat(kinds(history(t.ana(), t.groupId(), ids[0])).get(0)).isEqualTo("5 mergeUndone");
    assertThat(names(list(t.ana(), t.groupId(), "")))
        .containsExactlyInAnyOrder("Janez Novak", "J. Novak");
    assertThat(kinds(history(t.ana(), t.groupId(), ids[1])).get(0)).isEqualTo("3 mergeUndone");
    assertAudited(t.groupId(), t.marko(), now, before);
  }

  @Test
  @DisplayName("AC-019-02 after a later change of the Primary contact the undo is rejected")
  void ac01902() {
    Team t = team();
    String[] ids = merged(t);
    edited(t.ana(), t.groupId(), ids[0], field("jobTitle", "CEO"));
    final String beforeA = state(t.ana(), contactPath(t.groupId(), ids[0]));
    final String beforeB = state(t.ana(), contactPath(t.groupId(), ids[1]));

    Res res = undo(t.marko(), t, ids[2], 5);

    assertProblem(res, 409, "merge.changedAfter");
    assertThat(state(t.ana(), contactPath(t.groupId(), ids[0]))).isEqualTo(beforeA);
    assertThat(state(t.ana(), contactPath(t.groupId(), ids[1]))).isEqualTo(beforeB);
  }

  @Test
  @DisplayName("AC-019-03 a Viewer cannot undo a Merge (403, nothing changes)")
  void ac01903() {
    Team t = team();
    String[] ids = merged(t);
    final String beforeA = state(t.ana(), contactPath(t.groupId(), ids[0]));
    final String beforeB = state(t.ana(), contactPath(t.groupId(), ids[1]));

    Res res = undo(t.petra(), t, ids[2], 4);

    assertProblem(res, 403, "auth.forbidden");
    assertThat(state(t.ana(), contactPath(t.groupId(), ids[0]))).isEqualTo(beforeA);
    assertThat(state(t.ana(), contactPath(t.groupId(), ids[1]))).isEqualTo(beforeB);
  }

  @Test
  @DisplayName("EX-019-01 a second undo of the same Merge is 409 already undone")
  void ex01901() {
    Team t = team();
    String[] ids = merged(t);
    undo(t.marko(), t, ids[2], 4);
    final String before = state(t.ana(), contactPath(t.groupId(), ids[0]));

    Res res = undo(t.marko(), t, ids[2], 5);

    assertProblem(res, 409, "merge.alreadyUndone");
    assertThat(state(t.ana(), contactPath(t.groupId(), ids[0]))).isEqualTo(before);
  }

  @Test
  @DisplayName("EX-019-02 a Primary contact in the recycle bin must be restored first")
  void ex01902() {
    Team t = team();
    String[] ids = merged(t);
    moveToRecycleBin(t.marko(), t.groupId(), ids[0]);
    final String before = state(t.ana(), t.path() + "/recycle-bin");

    Res res = undo(t.marko(), t, ids[2], 5);

    assertProblem(res, 409, "merge.restoreFirst");
    assertThat(state(t.ana(), t.path() + "/recycle-bin")).isEqualTo(before);
  }
}
