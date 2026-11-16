package app.contacts.acceptance;

import static app.contacts.acceptance.support.Versions.changes;
import static org.assertj.core.api.Assertions.assertThat;

import app.contacts.acceptance.support.AcceptanceTest;
import app.contacts.acceptance.support.Json;
import app.contacts.acceptance.support.Res;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** US-008 Attributes of a Contact: API part. */
class Us008AttributesAcceptanceIT extends AcceptanceTest {

  private String janez(Team t, String... attributes) {
    ObjectNode body = contactBody("firstName", "Janez", "lastName", "Novak");
    body.set("attributes", Json.attributes(attributes));
    return contact(t.ana(), t.groupId(), body);
  }

  private Res withAttributes(Team t, String contactId, ArrayNode attributes) {
    return edit(t.marko(), t.groupId(), contactId, u -> u.set("attributes", attributes));
  }

  private static ArrayNode numbered(int count) {
    ArrayNode attributes = Json.MAPPER.createArrayNode();
    for (int i = 1; i <= count; i++) {
      attributes.add(Json.obj("key", String.format("kljuc%02d", i), "value", "v"));
    }
    return attributes;
  }

  @Test
  @DisplayName("AC-008-01 an added Attribute is stored and listed as attribute:<key> in Version 2")
  void ac00801() {
    Team t = team();
    String janez = janez(t);

    Res res = withAttributes(t, janez, Json.attributes("kompetenca", "spletnoProgramiranje"));

    assertThat(res.status()).isEqualTo(200);
    JsonNode attributes = getContact(t.petra(), t.groupId(), janez).path("attributes");
    assertThat(Json.texts(attributes, "key")).containsExactly("kompetenca");
    assertThat(Json.texts(attributes, "value")).containsExactly("spletnoProgramiranje");
    JsonNode latest = history(t.petra(), t.groupId(), janez).get(0);
    assertThat(latest.path("number").asInt()).isEqualTo(2);
    assertThat(changes(latest)).containsExactly("attribute:kompetenca: ∅ → spletnoProgramiranje");
  }

  @Test
  @DisplayName("AC-008-02 a changed value is listed with old and new value")
  void ac00802() {
    Team t = team();
    String janez = janez(t);
    withAttributes(t, janez, Json.attributes("kompetenca", "spletnoProgramiranje"));

    withAttributes(t, janez, Json.attributes("kompetenca", "mobilnoProgramiranje"));

    JsonNode latest = history(t.ana(), t.groupId(), janez).get(0);
    assertThat(latest.path("number").asInt()).isEqualTo(3);
    assertThat(changes(latest))
        .containsExactly("attribute:kompetenca: spletnoProgramiranje → mobilnoProgramiranje");
  }

  @Test
  @DisplayName("AC-008-03 the same key twice (case-insensitive) is rejected, nothing stored")
  void ac00803() {
    Team t = team();
    String janez = janez(t, "kompetenca", "mobilnoProgramiranje");
    final String before = state(t.ana(), contactPath(t.groupId(), janez));

    Res res =
        withAttributes(
            t, janez, Json.attributes("kompetenca", "mobilnoProgramiranje", "Kompetenca", "java"));

    assertProblem(res, 400, "attribute.keyTwice");
    assertThat(state(t.ana(), contactPath(t.groupId(), janez))).isEqualTo(before);
  }

  @Test
  @DisplayName("AC-008-04 a removed Attribute is listed with its old value")
  void ac00804() {
    Team t = team();
    String janez = janez(t, "kompetenca", "mobilnoProgramiranje");

    withAttributes(t, janez, Json.attributes());

    assertThat(getContact(t.ana(), t.groupId(), janez).path("attributes").size()).isZero();
    assertThat(changes(history(t.ana(), t.groupId(), janez).get(0)))
        .containsExactly("attribute:kompetenca: mobilnoProgramiranje → ∅");
  }

  @Test
  @DisplayName("EX-008-01 a key of 50 characters is saved, of 51 rejected")
  void ex00801() {
    Team t = team();
    String janez = janez(t);

    Res ok = withAttributes(t, janez, Json.attributes("k".repeat(50), "v"));
    final String before = state(t.ana(), contactPath(t.groupId(), janez));
    Res tooLong = withAttributes(t, janez, Json.attributes("k".repeat(51), "v"));

    assertThat(ok.status()).isEqualTo(200);
    assertProblem(tooLong, 400, "attribute.keyLength");
    assertThat(state(t.ana(), contactPath(t.groupId(), janez))).isEqualTo(before);
  }

  @Test
  @DisplayName("EX-008-02 a key of only spaces is rejected")
  void ex00802() {
    Team t = team();
    String janez = janez(t);
    final String before = state(t.ana(), contactPath(t.groupId(), janez));

    Res res = withAttributes(t, janez, Json.attributes("   ", "v"));

    assertProblem(res, 400, "attribute.keyLength");
    assertThat(state(t.ana(), contactPath(t.groupId(), janez))).isEqualTo(before);
  }

  @Test
  @DisplayName("EX-008-03 a value of 500 characters is saved, of 501 rejected")
  void ex00803() {
    Team t = team();
    String janez = janez(t);

    Res ok = withAttributes(t, janez, Json.attributes("opis", "v".repeat(500)));
    final String before = state(t.ana(), contactPath(t.groupId(), janez));
    Res tooLong = withAttributes(t, janez, Json.attributes("opis", "v".repeat(501)));

    assertThat(ok.status()).isEqualTo(200);
    assertProblem(tooLong, 400, "attribute.valueTooLong");
    assertThat(state(t.ana(), contactPath(t.groupId(), janez))).isEqualTo(before);
  }

  @Test
  @DisplayName("EX-008-04 an empty value is saved")
  void ex00804() {
    Team t = team();
    String janez = janez(t);

    Res res = withAttributes(t, janez, Json.attributes("kompetenca", ""));

    assertThat(res.status()).isEqualTo(200);
    JsonNode attributes = getContact(t.ana(), t.groupId(), janez).path("attributes");
    assertThat(Json.texts(attributes, "key")).containsExactly("kompetenca");
    assertThat(Json.texts(attributes, "value")).containsExactly("");
  }

  @Test
  @DisplayName("EX-008-05 the 50th Attribute is saved, the 51st rejected")
  void ex00805() {
    Team t = team();
    String janez = janez(t);
    withAttributes(t, janez, numbered(49));

    Res fiftieth = withAttributes(t, janez, numbered(50));
    final String before = state(t.ana(), contactPath(t.groupId(), janez));
    Res fiftyFirst = withAttributes(t, janez, numbered(51));

    assertThat(fiftieth.status()).isEqualTo(200);
    assertProblem(fiftyFirst, 400, "attribute.tooMany");
    assertThat(getContact(t.ana(), t.groupId(), janez).path("attributes").size()).isEqualTo(50);
    assertThat(state(t.ana(), contactPath(t.groupId(), janez))).isEqualTo(before);
  }

  @Test
  @DisplayName("EX-008-06 a Field and an Attribute changed in one save give one Version")
  void ex00806() {
    Team t = team();
    String janez = janez(t);

    edited(
        t.marko(),
        t.groupId(),
        janez,
        field("city", "Maribor")
            .andThen(u -> u.set("attributes", Json.attributes("kompetenca", "java"))));

    JsonNode history = history(t.ana(), t.groupId(), janez);
    assertThat(history.size()).isEqualTo(2);
    assertThat(changes(history.get(0)))
        .containsExactlyInAnyOrder("city: ∅ → Maribor", "attribute:kompetenca: ∅ → java");
  }

  @Test
  @DisplayName("EX-008-07 a save with a stale Version number is rejected as BR-06")
  void ex00807() {
    Team t = team();
    String janez = janez(t);
    ObjectNode stale = updateOf(getContact(t.marko(), t.groupId(), janez));
    edited(t.ana(), t.groupId(), janez, field("city", "Maribor"));
    stale.set("attributes", Json.attributes("kompetenca", "java"));
    final String before = state(t.ana(), contactPath(t.groupId(), janez));

    Res res = api.put(t.marko(), contactPath(t.groupId(), janez), stale);

    assertProblem(res, 409, "version.conflict");
    assertThat(Json.text(res.json().path("params").path("changedBy"))).isEqualTo("Ana Kovač");
    assertThat(state(t.ana(), contactPath(t.groupId(), janez))).isEqualTo(before);
  }
}
