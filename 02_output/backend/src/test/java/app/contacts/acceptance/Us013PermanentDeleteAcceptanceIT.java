package app.contacts.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import app.contacts.acceptance.support.AcceptanceTest;
import app.contacts.acceptance.support.Json;
import app.contacts.acceptance.support.Res;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;

/** US-013 Permanently delete a Contact: API part. */
class Us013PermanentDeleteAcceptanceIT extends AcceptanceTest {

  /** "Janez Novak" with a Tag and an Attribute, 4 Versions, in the recycle bin. */
  private String janezInRecycleBin(Team t) {
    ObjectNode body =
        contactBody("firstName", "Janez", "lastName", "Novak", "email", "janez.novak@example.si");
    body.set("tags", Json.strings("udelezenec2025"));
    body.set("attributes", Json.attributes("kompetenca", "spletnoProgramiranje"));
    String id = contact(t.ana(), t.groupId(), body);
    edited(t.marko(), t.groupId(), id, field("city", "Ljubljana"));
    edited(t.marko(), t.groupId(), id, field("phone", "+386 41 123 456"));
    moveToRecycleBin(t.marko(), t.groupId(), id);
    return id;
  }

  private Res deletePermanently(Team t, String contactId) {
    return api.delete(t.ana(), t.path() + "/recycle-bin/" + contactId, null);
  }

  @Test
  @DisplayName("AC-013-01 permanent delete erases the Contact and Versions; one Audit entry")
  void ac01301() {
    Team t = team();
    String janez = janezInRecycleBin(t);
    final List<Map<String, Object>> before = auditEntries(t.groupId());
    final Instant now = clock.setLjubljana("2026-03-06T09:00");

    Res res = deletePermanently(t, janez);

    assertThat(res.status()).isEqualTo(204);
    assertThat(api.get(t.ana(), t.path() + "/recycle-bin").json().size()).isZero();
    assertThat(api.get(t.ana(), contactPath(t.groupId(), janez)).status()).isEqualTo(404);
    assertThat(api.get(t.ana(), contactPath(t.groupId(), janez) + "/versions").status())
        .isEqualTo(404);
    Map<String, Object> entry = assertAudited(t.groupId(), t.ana(), now, before);
    assertThat(entry.get("target_id")).isEqualTo(janez);
    assertThat(auditEntries(t.groupId()).stream().filter(e -> janez.equals(e.get("target_id"))))
        .hasSize(1);
    assertThat(String.valueOf(entry.get("action")))
        .doesNotContain("Janez")
        .doesNotContain("Novak")
        .doesNotContain("janez.novak@example.si");
    for (String table : List.of("contact_version", "attribute", "contact_tag")) {
      assertThat(rowsOfContact(table, janez)).as(table).isZero();
    }
    assertThat(
            db.queryForObject(
                "SELECT count(*) FROM contact WHERE id = ?::uuid", Integer.class, janez))
        .isZero();
  }

  @Test
  @DisplayName("AC-013-02 an Editor cannot delete permanently (403, stays in the recycle bin)")
  void ac01302() {
    Team t = team();
    String janez = janezInRecycleBin(t);
    final String before = state(t.ana(), t.path() + "/recycle-bin");

    Res res = api.delete(t.marko(), t.path() + "/recycle-bin/" + janez, null);

    assertProblem(res, 403, "auth.forbidden");
    assertThat(state(t.ana(), t.path() + "/recycle-bin")).isEqualTo(before);
  }

  @Test
  @DisplayName("AC-013-03 an active Contact cannot be deleted permanently (409, unchanged)")
  void ac01303() {
    Team t = team();
    String maja = contact(t.ana(), t.groupId(), "firstName", "Maja", "lastName", "Zupan");
    final String before = state(t.ana(), contactPath(t.groupId(), maja));

    Res res = deletePermanently(t, maja);

    assertProblem(res, 409, "contact.notInRecycleBin");
    assertThat(state(t.ana(), contactPath(t.groupId(), maja))).isEqualTo(before);
  }

  @Test
  @DisplayName("EX-013-02 a Tag used only by the deleted Contact is no longer suggested")
  void ex01302() {
    Team t = team();
    String janez = janezInRecycleBin(t);

    deletePermanently(t, janez);

    assertThat(api.get(t.marko(), t.path() + "/tags?q=udel").json().size()).isZero();
  }

  @Test
  @DisplayName("EX-013-03 (D-21) deleting a Primary contact also deletes the merged Contacts")
  void ex01303() {
    Team t = team();
    String janez =
        contact(t.ana(), t.groupId(), "firstName", "Janez", "lastName", "Novak", "email", "j@n.si");
    String other =
        contact(t.ana(), t.groupId(), "firstName", "J.", "lastName", "Novak", "email", "j@n.si");
    Res merged =
        api.post(
            t.marko(),
            t.path() + "/merges",
            Json.obj(
                "primaryId",
                janez,
                "otherId",
                other,
                "primaryVersionNumber",
                1,
                "otherVersionNumber",
                1));
    assertThat(merged.status()).as("merge: %s", merged).isEqualTo(201);
    moveToRecycleBin(t.marko(), t.groupId(), janez);
    final List<Map<String, Object>> before = auditEntries(t.groupId());

    Res res = deletePermanently(t, janez);

    assertThat(res.status()).isEqualTo(204);
    assertThat(api.get(t.ana(), contactPath(t.groupId(), other)).status()).isEqualTo(404);
    assertThat(api.get(t.ana(), contactPath(t.groupId(), janez)).status()).isEqualTo(404);
    List<Map<String, Object>> added = auditEntries(t.groupId());
    added.removeAll(before);
    assertThat(added.stream().map(e -> e.get("target_id"))).contains(janez, other);
  }

  @Test
  @DisplayName("EX-013-04 a Contact id that does not exist is 404")
  void ex01304() {
    Team t = team();

    Res res = deletePermanently(t, "00000000-0000-4000-8000-000000000999");

    assertThat(res.status()).isEqualTo(404);
  }
}
