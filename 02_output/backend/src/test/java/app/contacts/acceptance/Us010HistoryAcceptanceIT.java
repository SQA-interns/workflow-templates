package app.contacts.acceptance;

import static app.contacts.acceptance.support.Versions.changes;
import static app.contacts.acceptance.support.Versions.kinds;
import static org.assertj.core.api.Assertions.assertThat;

import app.contacts.acceptance.support.AcceptanceTest;
import app.contacts.acceptance.support.Json;
import app.contacts.acceptance.support.Res;
import app.contacts.acceptance.support.TestUser;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** US-010 History of a Contact: API part (display of times in {@code frontend/acceptance}). */
class Us010HistoryAcceptanceIT extends AcceptanceTest {

  private String janez(Team t) {
    return contact(
        t.ana(),
        t.groupId(),
        "firstName",
        "Janez",
        "lastName",
        "Novak",
        "email",
        "janez.novak@example.si",
        "city",
        "Ljubljana");
  }

  @Test
  @DisplayName("AC-010-01 the History lists every Version newest first with who, when and what")
  void ac01001() {
    Team t = team();
    clock.set(Instant.parse("2026-03-05T09:00:00Z"));
    String janez = janez(t);
    clock.set(Instant.parse("2026-07-01T08:00:00Z"));
    edited(t.marko(), t.groupId(), janez, field("email", "janez@novak.si"));

    JsonNode history = history(t.petra(), t.groupId(), janez);

    assertThat(kinds(history)).containsExactly("2 edited", "1 created");
    assertThat(Json.text(history.get(0).path("changedBy"))).isEqualTo("Marko Kranjc");
    assertThat(Instant.parse(Json.text(history.get(0).path("changedAt"))))
        .isEqualTo(Instant.parse("2026-07-01T08:00:00Z"));
    assertThat(changes(history.get(0)))
        .containsExactly("email: janez.novak@example.si → janez@novak.si");
    assertThat(Json.text(history.get(1).path("changedBy"))).isEqualTo("Ana Kovač");
    assertThat(Instant.parse(Json.text(history.get(1).path("changedAt"))))
        .isEqualTo(Instant.parse("2026-03-05T09:00:00Z"));
  }

  @Test
  @DisplayName("AC-010-02 one Version shows the full content as it was after that Version")
  void ac01002() {
    Team t = team();
    String janez = janez(t);
    edited(t.marko(), t.groupId(), janez, field("email", "janez@novak.si"));

    Res res = api.get(t.petra(), contactPath(t.groupId(), janez) + "/versions/1");

    assertThat(res.status()).isEqualTo(200);
    JsonNode fields = res.json().path("snapshot").path("fields");
    assertThat(Json.text(fields.path("email"))).isEqualTo("janez.novak@example.si");
    assertThat(Json.text(fields.path("firstName"))).isEqualTo("Janez");
    assertThat(Json.text(fields.path("lastName"))).isEqualTo("Novak");
    assertThat(Json.text(fields.path("city"))).isEqualTo("Ljubljana");
    assertThat(Json.text(res.json().path("kind"))).isEqualTo("created");
  }

  @Test
  @DisplayName("AC-010-03 a User of another Group gets 404 and no data for the History")
  void ac01003() {
    Team t = team();
    String janez = janez(t);
    TestUser nina = user("nina.kos@example.si", "Nina Kos");
    group(nina, "Podjetje Beta");

    Res res = api.get(nina, contactPath(t.groupId(), janez) + "/versions");

    assertThat(res.status()).isEqualTo(404);
    assertThat(res.body()).doesNotContain("janez.novak@example.si").doesNotContain("Novak");
  }

  @Test
  @DisplayName("EX-010-01 a Version made at 31.12.2026 23:30 UTC carries that instant")
  void ex01001() {
    Team t = team();
    clock.set(Instant.parse("2026-12-31T23:30:00Z"));
    String janez = janez(t);

    JsonNode history = history(t.petra(), t.groupId(), janez);

    assertThat(Instant.parse(Json.text(history.get(0).path("changedAt"))))
        .isEqualTo(Instant.parse("2026-12-31T23:30:00Z"));
  }

  @Test
  @DisplayName("EX-010-02 a Version made by a removed Member shows his display name")
  void ex01002() {
    Team t = team();
    String janez = janez(t);
    edited(t.marko(), t.groupId(), janez, field("city", "Maribor"));
    api.delete(t.ana(), t.path() + "/members/" + idOf(t.marko()), null);

    JsonNode history = history(t.petra(), t.groupId(), janez);

    assertThat(Json.text(history.get(0).path("changedBy"))).isEqualTo("Marko Kranjc");
  }

  @Test
  @DisplayName("EX-010-03 a request to change or delete a Version is 405 and changes nothing")
  void ex01003() {
    Team t = team();
    String janez = janez(t);
    String version = contactPath(t.groupId(), janez) + "/versions/1";
    final String before = state(t.ana(), version);

    Res put = api.put(t.ana(), version, Json.obj("kind", "edited"));
    Res patch = api.patch(t.ana(), version, Json.obj("kind", "edited"));
    Res delete = api.delete(t.ana(), version, null);

    assertThat(put.status()).isEqualTo(405);
    assertThat(patch.status()).isEqualTo(405);
    assertThat(delete.status()).isEqualTo(405);
    assertThat(state(t.ana(), version)).isEqualTo(before);
    assertThat(history(t.ana(), t.groupId(), janez).size()).isEqualTo(1);
  }

  @Test
  @DisplayName("EX-010-04 a Contact with one Version has one History entry")
  void ex01004() {
    Team t = team();
    String janez = janez(t);

    assertThat(kinds(history(t.petra(), t.groupId(), janez))).containsExactly("1 created");
  }

  @Test
  @DisplayName("EX-010-05 a Version number that does not exist is 404")
  void ex01005() {
    Team t = team();
    String janez = janez(t);

    Res res = api.get(t.petra(), contactPath(t.groupId(), janez) + "/versions/99");

    assertThat(res.status()).isEqualTo(404);
  }
}
