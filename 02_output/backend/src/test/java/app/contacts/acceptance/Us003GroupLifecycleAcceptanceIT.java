package app.contacts.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import app.contacts.acceptance.support.AcceptanceTest;
import app.contacts.acceptance.support.Json;
import app.contacts.acceptance.support.Res;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** US-003 Rename, move to recycle bin and restore a Group: API part. */
class Us003GroupLifecycleAcceptanceIT extends AcceptanceTest {

  private static final Duration PERIOD = Duration.ofDays(30);

  @Test
  @DisplayName("AC-003-01 the Owner renames the Group")
  void ac00301() {
    Team t = team();

    Res res = api.patch(t.ana(), t.path(), Json.obj("name", "Konferenca 2026"));

    assertThat(res.status()).isEqualTo(200);
    assertThat(groupNames(t.ana())).containsExactly("Konferenca 2026");
    assertThat(groupNames(t.marko())).containsExactly("Konferenca 2026");
  }

  @Test
  @DisplayName("AC-003-02 an Editor cannot rename the Group (403, name unchanged)")
  void ac00302() {
    Team t = team();

    Res res = api.patch(t.marko(), t.path(), Json.obj("name", "Test"));

    assertProblem(res, 403, "auth.forbidden");
    assertThat(groupNames(t.ana())).containsExactly("Konferenca 2025");
  }

  @Test
  @DisplayName("AC-003-03 moved to the recycle bin: hidden, in Deleted Groups with removal date")
  void ac00303() {
    Team t = team();
    final List<Map<String, Object>> before = auditEntries(t.groupId());
    final Instant now = clock.setLjubljana("2026-03-05T11:00");

    Res res = api.post(t.ana(), t.path() + "/move-to-recycle-bin", null);

    assertThat(res.status()).isEqualTo(204);
    assertThat(groupNames(t.ana())).isEmpty();
    assertThat(groupNames(t.marko())).isEmpty();
    JsonNode deleted = api.get(t.ana(), "/deleted-groups").json();
    assertThat(deleted.size()).isEqualTo(1);
    assertThat(Json.text(deleted.get(0).path("name"))).isEqualTo("Konferenca 2025");
    assertThat(Instant.parse(Json.text(deleted.get(0).path("deletedAt")))).isEqualTo(now);
    assertThat(Instant.parse(Json.text(deleted.get(0).path("removalAt"))))
        .isEqualTo(Instant.parse("2026-04-04T10:00:00Z"));
    Res marko = api.get(t.marko(), t.path());
    assertThat(marko.status()).isEqualTo(404);
    assertThat(marko.body()).doesNotContain("Konferenca 2025");
    assertAudited(t.groupId(), t.ana(), now, before);
  }

  @Test
  @DisplayName("AC-003-04 the Owner restores the Group with its Contacts and Members")
  void ac00304() {
    Team t = team();
    contact(t.ana(), t.groupId(), "firstName", "Janez", "lastName", "Novak");
    contact(t.ana(), t.groupId(), "firstName", "Maja", "lastName", "Zupan");
    contact(t.ana(), t.groupId(), "firstName", "Žiga", "lastName", "Čuk");
    clock.setLjubljana("2026-03-05T11:00");
    api.post(t.ana(), t.path() + "/move-to-recycle-bin", null);
    final List<Map<String, Object>> before = auditEntries(t.groupId());
    final Instant now = clock.setLjubljana("2026-03-10T09:00");

    Res res = api.post(t.ana(), "/deleted-groups/" + t.groupId() + "/restore", null);

    assertThat(res.status()).isEqualTo(200);
    assertThat(groupNames(t.ana())).containsExactly("Konferenca 2025");
    JsonNode markoGroups = api.get(t.marko(), "/groups").json();
    assertThat(Json.texts(markoGroups, "name")).containsExactly("Konferenca 2025");
    assertThat(Json.text(markoGroups.get(0).path("role"))).isEqualTo("EDITOR");
    assertThat(list(t.marko(), t.groupId(), "").path("total").asInt()).isEqualTo(3);
    assertThat(api.get(t.ana(), "/deleted-groups").json().size()).isZero();
    assertAudited(t.groupId(), t.ana(), now, before);
  }

  @Test
  @DisplayName("AC-003-05 after the removal date the Group and all it owns no longer exist")
  void ac00305() {
    Team t = team();
    String janez =
        contact(t.ana(), t.groupId(), contactBody("firstName", "Janez", "lastName", "Novak"));
    edited(t.ana(), t.groupId(), janez, u -> u.set("tags", Json.strings("udelezenec2025")));
    invite(t.groupId(), t.ana(), "nina.kos@example.si", "VIEWER");
    clock.setLjubljana("2026-03-05T11:00");
    api.post(t.ana(), t.path() + "/move-to-recycle-bin", null);

    clock.setLjubljana("2026-04-04T12:01");
    runScheduledJobs();

    assertThat(api.get(t.ana(), "/deleted-groups").json().size()).isZero();
    assertThat(api.get(t.ana(), t.path()).status()).isEqualTo(404);
    for (String table :
        List.of("contact", "contact_version", "membership", "invitation", "tag", "contact_tag")) {
      assertThat(
              db.queryForObject(
                  "SELECT count(*) FROM " + table + " WHERE group_id = ?::uuid",
                  Integer.class,
                  t.groupId()))
          .as(table)
          .isZero();
    }
    assertThat(
            db.queryForObject(
                "SELECT count(*) FROM contact_group WHERE id = ?::uuid",
                Integer.class,
                t.groupId()))
        .isZero();
  }

  @Test
  @DisplayName("EX-003-01 restore one minute before the removal date succeeds")
  void ex00301() {
    Team t = team();
    final Instant moved = clock.setLjubljana("2026-03-05T11:00");
    api.post(t.ana(), t.path() + "/move-to-recycle-bin", null);
    clock.set(moved.plus(PERIOD).minus(Duration.ofMinutes(1)));

    Res res = api.post(t.ana(), "/deleted-groups/" + t.groupId() + "/restore", null);

    assertThat(res.status()).isEqualTo(200);
    assertThat(groupNames(t.ana())).containsExactly("Konferenca 2025");
  }

  @Test
  @DisplayName("EX-003-02 restore exactly at the removal date is rejected, nothing changes")
  void ex00302() {
    Team t = team();
    final Instant moved = clock.setLjubljana("2026-03-05T11:00");
    api.post(t.ana(), t.path() + "/move-to-recycle-bin", null);
    clock.set(moved.plus(PERIOD));

    Res res = api.post(t.ana(), "/deleted-groups/" + t.groupId() + "/restore", null);

    assertProblem(res, 409, "group.noLongerRestorable");
    assertThat(groupNames(t.ana())).isEmpty();
    assertThat(Json.texts(api.get(t.ana(), "/deleted-groups").json(), "name"))
        .containsExactly("Konferenca 2025");
  }

  @Test
  @DisplayName("EX-003-03 a Viewer cannot move the Group to the recycle bin (403)")
  void ex00303() {
    Team t = team();

    Res res = api.post(t.petra(), t.path() + "/move-to-recycle-bin", null);

    assertProblem(res, 403, "auth.forbidden");
    assertThat(groupNames(t.ana())).containsExactly("Konferenca 2025");
    assertThat(groupNames(t.petra())).containsExactly("Konferenca 2025");
  }

  @Test
  @DisplayName("EX-003-05 an Invitation of a Group in the recycle bin is no longer valid")
  void ex00305() {
    Team t = team();
    String token = invite(t.groupId(), t.ana(), "nina.kos@example.si", "EDITOR");
    api.post(t.ana(), t.path() + "/move-to-recycle-bin", null);
    var nina = user("nina.kos@example.si", "Nina Kos");

    Res view = api.get(nina, "/invitations/" + token);
    Res accept = api.post(nina, "/invitations/" + token + "/accept", null);

    assertProblem(view, 409, "invitation.noLongerValid");
    assertProblem(accept, 409, "invitation.noLongerValid");
    assertThat(groupNames(nina)).isEmpty();
  }

  @Test
  @DisplayName("EX-003-06 rename with a 101-character name is rejected, name unchanged")
  void ex00306() {
    Team t = team();

    Res res = api.patch(t.ana(), t.path(), Json.obj("name", "K".repeat(101)));

    assertProblem(res, 400, "group.nameLength");
    assertThat(groupNames(t.ana())).containsExactly("Konferenca 2025");
  }
}
