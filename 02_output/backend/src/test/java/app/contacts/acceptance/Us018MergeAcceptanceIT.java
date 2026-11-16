package app.contacts.acceptance;

import static app.contacts.acceptance.support.Versions.kinds;
import static org.assertj.core.api.Assertions.assertThat;

import app.contacts.acceptance.support.AcceptanceTest;
import app.contacts.acceptance.support.Json;
import app.contacts.acceptance.support.Res;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** US-018 Merge two Contacts: API part (the Merge screen is in {@code frontend/acceptance}). */
class Us018MergeAcceptanceIT extends AcceptanceTest {

  /**
   * "Janez Novak" at Version 3 (no phone, Tag udelezenec2025) and "J. Novak" at Version 1 (phone,
   * Tag vip), same e-mail (AC-018-01); Janez first.
   */
  private String[] janezAndJ(Team t) {
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
    return new String[] {first, contact(t.ana(), t.groupId(), other)};
  }

  private static ObjectNode keepOtherPhone() {
    return Json.obj("fieldChoices", Json.obj("phone", "other"));
  }

  private String withAttributes(Team t, String first, ArrayNode attributes) {
    ObjectNode body = contactBody("firstName", first, "lastName", "Novak");
    body.set("attributes", attributes);
    return contact(t.ana(), t.groupId(), body);
  }

  private static ArrayNode keys(String prefix, int count) {
    ArrayNode attributes = Json.MAPPER.createArrayNode();
    for (int i = 1; i <= count; i++) {
      attributes.add(Json.obj("key", prefix + i, "value", "v"));
    }
    return attributes;
  }

  @Test
  @DisplayName(
      "AC-018-01 merge: Primary gets chosen Fields and all Tags; both get a merged Version")
  void ac01801() {
    Team t = team();
    String[] ids = janezAndJ(t);
    final List<Map<String, Object>> before = auditEntries(t.groupId());
    final Instant now = clock.setLjubljana("2026-03-05T10:00");

    Res res = merge(t.marko(), t.groupId(), ids[0], ids[1], keepOtherPhone());

    assertThat(res.status()).isEqualTo(201);
    JsonNode janez = getContact(t.ana(), t.groupId(), ids[0]);
    assertThat(Json.text(janez.path("fields").path("phone"))).isEqualTo("+386 41 123 456");
    assertThat(Json.text(janez.path("fields").path("firstName"))).isEqualTo("Janez");
    assertThat(Json.texts(janez.path("tags"), "name"))
        .containsExactlyInAnyOrder("udelezenec2025", "vip");
    assertThat(kinds(history(t.ana(), t.groupId(), ids[0])).get(0)).isEqualTo("4 merged");
    assertThat(names(list(t.ana(), t.groupId(), ""))).containsExactly("Janez Novak");
    assertThat(kinds(history(t.ana(), t.groupId(), ids[1])))
        .containsExactly("2 merged", "1 created");
    assertAudited(t.groupId(), t.marko(), now, before);
  }

  @Test
  @DisplayName("AC-018-02 the Primary contact shows Merged from with the Other contact's History")
  void ac01802() {
    Team t = team();
    String[] ids = janezAndJ(t);
    merge(t.marko(), t.groupId(), ids[0], ids[1], keepOtherPhone());

    JsonNode mergedFrom = getContact(t.marko(), t.groupId(), ids[0]).path("mergedFrom");

    assertThat(mergedFrom.size()).isEqualTo(1);
    assertThat(Json.text(mergedFrom.get(0).path("contact").path("name"))).isEqualTo("J. Novak");
    assertThat(Json.text(mergedFrom.get(0).path("contact").path("id"))).isEqualTo(ids[1]);
    assertThat(mergedFrom.get(0).path("undone").asBoolean()).isFalse();
    assertThat(kinds(history(t.marko(), t.groupId(), ids[1])))
        .containsExactly("2 merged", "1 created");
  }

  @Test
  @DisplayName("AC-018-03 two values of one Attribute key need a choice; nothing changes without")
  void ac01803() {
    Team t = team();
    String a = withAttributes(t, "Janez", Json.attributes("kompetenca", "java"));
    String b = withAttributes(t, "J.", Json.attributes("kompetenca", "python"));
    final String beforeA = state(t.ana(), contactPath(t.groupId(), a));
    final String beforeB = state(t.ana(), contactPath(t.groupId(), b));

    Res res = merge(t.marko(), t.groupId(), a, b, null);

    assertProblem(res, 400, "merge.choiceRequired");
    assertThat(res.body()).contains("kompetenca");
    assertThat(state(t.ana(), contactPath(t.groupId(), a))).isEqualTo(beforeA);
    assertThat(state(t.ana(), contactPath(t.groupId(), b))).isEqualTo(beforeB);
  }

  @Test
  @DisplayName("AC-018-04 a Viewer cannot merge (403, nothing changes)")
  void ac01804() {
    Team t = team();
    String[] ids = janezAndJ(t);
    final String beforeA = state(t.ana(), contactPath(t.groupId(), ids[0]));
    final String beforeB = state(t.ana(), contactPath(t.groupId(), ids[1]));

    Res res = merge(t.petra(), t.groupId(), ids[0], ids[1], keepOtherPhone());

    assertProblem(res, 403, "auth.forbidden");
    assertThat(state(t.ana(), contactPath(t.groupId(), ids[0]))).isEqualTo(beforeA);
    assertThat(state(t.ana(), contactPath(t.groupId(), ids[1]))).isEqualTo(beforeB);
  }

  @Test
  @DisplayName("EX-018-01 the same Attribute key with the same value is kept once, no choice")
  void ex01801() {
    Team t = team();
    String a = withAttributes(t, "Janez", Json.attributes("kompetenca", "java"));
    String b = withAttributes(t, "J.", Json.attributes("kompetenca", "java"));

    Res res = merge(t.marko(), t.groupId(), a, b, null);

    assertThat(res.status()).isEqualTo(201);
    JsonNode attributes = getContact(t.ana(), t.groupId(), a).path("attributes");
    assertThat(Json.texts(attributes, "key")).containsExactly("kompetenca");
    assertThat(Json.texts(attributes, "value")).containsExactly("java");
  }

  @Test
  @DisplayName("EX-018-02 combined Attributes 50 are merged, 51 rejected")
  void ex01802() {
    Team t = team();
    String a = withAttributes(t, "Janez", keys("a", 25));
    String b = withAttributes(t, "J.", keys("b", 25));
    String c = withAttributes(t, "Maja", keys("c", 25));
    String d = withAttributes(t, "M.", keys("d", 26));
    final String beforeC = state(t.ana(), contactPath(t.groupId(), c));

    Res fifty = merge(t.marko(), t.groupId(), a, b, null);
    Res fiftyOne = merge(t.marko(), t.groupId(), c, d, null);

    assertThat(fifty.status()).isEqualTo(201);
    assertThat(getContact(t.ana(), t.groupId(), a).path("attributes").size()).isEqualTo(50);
    assertProblem(fiftyOne, 409, "merge.tooManyAttributes");
    assertThat(state(t.ana(), contactPath(t.groupId(), c))).isEqualTo(beforeC);
  }

  @Test
  @DisplayName("EX-018-03 a Contact moved to the recycle bin meanwhile cannot be merged")
  void ex01803() {
    Team t = team();
    String[] ids = janezAndJ(t);
    moveToRecycleBin(t.ana(), t.groupId(), ids[1]);
    final String before = state(t.ana(), contactPath(t.groupId(), ids[0]));

    Res res =
        api.post(
            t.marko(),
            t.path() + "/merges",
            Json.obj(
                "primaryId",
                ids[0],
                "otherId",
                ids[1],
                "primaryVersionNumber",
                3,
                "otherVersionNumber",
                2));

    assertProblem(res, 409, "merge.notPossible");
    assertThat(state(t.ana(), contactPath(t.groupId(), ids[0]))).isEqualTo(before);
  }

  @Test
  @DisplayName("EX-018-04 a Contact changed by someone else meanwhile: rejected as BR-06")
  void ex01804() {
    Team t = team();
    String[] ids = janezAndJ(t);
    edited(t.ana(), t.groupId(), ids[1], field("city", "Celje"));
    final String before = state(t.ana(), contactPath(t.groupId(), ids[1]));

    Res res =
        api.post(
            t.marko(),
            t.path() + "/merges",
            Json.obj(
                "primaryId",
                ids[0],
                "otherId",
                ids[1],
                "primaryVersionNumber",
                3,
                "otherVersionNumber",
                1));

    assertProblem(res, 409, "version.conflict");
    assertThat(state(t.ana(), contactPath(t.groupId(), ids[1]))).isEqualTo(before);
  }

  @Test
  @DisplayName("EX-018-05 merging a Contact with itself is 400")
  void ex01805() {
    Team t = team();
    String[] ids = janezAndJ(t);
    final String before = state(t.ana(), contactPath(t.groupId(), ids[0]));

    Res res = merge(t.marko(), t.groupId(), ids[0], ids[0], null);

    assertProblem(res, 400, "merge.notPossible");
    assertThat(state(t.ana(), contactPath(t.groupId(), ids[0]))).isEqualTo(before);
  }

  @Test
  @DisplayName("EX-018-07 the same Tag on both is kept once")
  void ex01807() {
    Team t = team();
    ObjectNode first = contactBody("firstName", "Janez", "lastName", "Novak");
    first.set("tags", Json.strings("vip"));
    ObjectNode second = contactBody("firstName", "J.", "lastName", "Novak");
    second.set("tags", Json.strings("vip"));
    String a = contact(t.ana(), t.groupId(), first);
    String b = contact(t.ana(), t.groupId(), second);

    merge(t.marko(), t.groupId(), a, b, null);

    assertThat(Json.texts(getContact(t.ana(), t.groupId(), a).path("tags"), "name"))
        .containsExactly("vip");
  }
}
