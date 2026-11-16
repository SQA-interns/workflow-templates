package app.contacts.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import app.contacts.acceptance.support.AcceptanceTest;
import app.contacts.acceptance.support.Json;
import app.contacts.acceptance.support.Res;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** US-005 Manage Members and Roles: API part. */
class Us005MembersAcceptanceIT extends AcceptanceTest {

  private static List<String> rows(JsonNode members) {
    List<String> rows = new ArrayList<>();
    for (JsonNode m : members) {
      rows.add(
          Json.text(m.path("displayName"))
              + " | "
              + Json.text(m.path("email"))
              + " | "
              + Json.text(m.path("role")));
    }
    return rows;
  }

  private String member(Team t, String userId) {
    return t.path() + "/members/" + userId;
  }

  @Test
  @DisplayName("AC-005-01 the Owner sees every Member with display name, e-mail and Role")
  void ac00501() {
    Team t = team();

    Res res = api.get(t.ana(), t.path() + "/members");

    assertThat(res.status()).isEqualTo(200);
    assertThat(rows(res.json()))
        .containsExactlyInAnyOrder(
            "Ana Kovač | " + ANA + " | OWNER",
            "Marko Kranjc | " + MARKO + " | EDITOR",
            "Petra Horvat | " + PETRA + " | VIEWER");
  }

  @Test
  @DisplayName("AC-005-02 a Member changed to Viewer can no longer edit; the change is audited")
  void ac00502() {
    Team t = team();
    String janez = contact(t.ana(), t.groupId(), "firstName", "Janez", "lastName", "Novak");
    final List<Map<String, Object>> before = auditEntries(t.groupId());
    final Instant now = clock.setLjubljana("2026-03-05T10:00");

    Res changed = api.patch(t.ana(), member(t, idOf(t.marko())), Json.obj("role", "VIEWER"));
    assertThat(changed.status()).isEqualTo(200);
    String beforeEdit = state(t.ana(), contactPath(t.groupId(), janez));
    Res edit = edit(t.marko(), t.groupId(), janez, field("city", "Maribor"));

    assertProblem(edit, 403, "auth.forbidden");
    assertThat(state(t.ana(), contactPath(t.groupId(), janez))).isEqualTo(beforeEdit);
    assertAudited(t.groupId(), t.ana(), now, before);
  }

  @Test
  @DisplayName("AC-005-03 a removed Member loses access; his Versions keep his name; audited")
  void ac00503() {
    Team t = team();
    String janez = contact(t.ana(), t.groupId(), "firstName", "Janez", "lastName", "Novak");
    edited(t.marko(), t.groupId(), janez, field("city", "Maribor"));
    final List<Map<String, Object>> before = auditEntries(t.groupId());
    final Instant now = clock.setLjubljana("2026-03-05T10:00");

    Res removed = api.delete(t.ana(), member(t, idOf(t.marko())), null);

    assertThat(removed.status()).isEqualTo(204);
    assertThat(groupNames(t.marko())).isEmpty();
    assertThat(api.get(t.marko(), t.path() + "/contacts").status()).isEqualTo(404);
    JsonNode history = history(t.ana(), t.groupId(), janez);
    assertThat(history.get(0).path("number").asInt()).isEqualTo(2);
    assertThat(Json.text(history.get(0).path("changedBy"))).isEqualTo("Marko Kranjc");
    assertAudited(t.groupId(), t.ana(), now, before);
  }

  @Test
  @DisplayName("AC-005-04 a Viewer cannot read the Member list (403)")
  void ac00504() {
    Team t = team();

    Res res = api.get(t.petra(), t.path() + "/members");

    assertProblem(res, 403, "auth.forbidden");
    assertThat(res.body()).doesNotContain(ANA).doesNotContain(MARKO);
  }

  @Test
  @DisplayName("AC-005-05 an Editor cannot change a Role (403, Role unchanged)")
  void ac00505() {
    Team t = team();

    Res res = api.patch(t.marko(), member(t, idOf(t.petra())), Json.obj("role", "EDITOR"));

    assertProblem(res, 403, "auth.forbidden");
    assertThat(rows(api.get(t.ana(), t.path() + "/members").json()))
        .contains("Petra Horvat | " + PETRA + " | VIEWER");
  }

  @Test
  @DisplayName("EX-005-01 the Owner cannot remove herself")
  void ex00501() {
    Team t = team();
    final String beforeMembers = state(t.ana(), t.path() + "/members");

    Res res = api.delete(t.ana(), member(t, idOf(t.ana())), null);

    assertProblem(res, 409, "member.ownerRequired");
    assertThat(state(t.ana(), t.path() + "/members")).isEqualTo(beforeMembers);
  }

  @Test
  @DisplayName("EX-005-02 the Owner cannot change her own Role")
  void ex00502() {
    Team t = team();
    final String beforeMembers = state(t.ana(), t.path() + "/members");

    Res res = api.patch(t.ana(), member(t, idOf(t.ana())), Json.obj("role", "EDITOR"));

    assertProblem(res, 409, "member.ownerRequired");
    assertThat(state(t.ana(), t.path() + "/members")).isEqualTo(beforeMembers);
  }

  @Test
  @DisplayName("EX-005-03 a Role change to Owner is rejected (400, nothing changes)")
  void ex00503() {
    Team t = team();
    final String beforeMembers = state(t.ana(), t.path() + "/members");

    Res res = api.patch(t.ana(), member(t, idOf(t.marko())), Json.obj("role", "OWNER"));

    assertThat(res.status()).isEqualTo(400);
    assertThat(state(t.ana(), t.path() + "/members")).isEqualTo(beforeMembers);
  }

  @Test
  @DisplayName("EX-005-05 a removed Member invited again is a Member with the new Role")
  void ex00505() {
    Team t = team();
    api.delete(t.ana(), member(t, idOf(t.marko())), null);

    join(t.groupId(), t.ana(), t.marko(), "VIEWER");

    assertThat(rows(api.get(t.ana(), t.path() + "/members").json()))
        .contains("Marko Kranjc | " + MARKO + " | VIEWER");
  }
}
