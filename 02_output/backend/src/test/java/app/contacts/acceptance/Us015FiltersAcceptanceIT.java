package app.contacts.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import app.contacts.acceptance.support.AcceptanceTest;
import app.contacts.acceptance.support.Json;
import app.contacts.acceptance.support.Res;
import app.contacts.acceptance.support.TestUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** US-015 Filter the Contact list: API part. */
class Us015FiltersAcceptanceIT extends AcceptanceTest {

  private String person(Team t, String first, String last, String country, String... tags) {
    ObjectNode body = contactBody("firstName", first, "lastName", last, "country", country);
    body.set("tags", Json.strings(tags));
    return contact(t.ana(), t.groupId(), body);
  }

  /** Janez (udelezenec2025, SI), Maja (udelezenec2025, neObvescaj, AT), Žiga (no Tags). */
  private String threeContacts(Team t) {
    String janez = person(t, "Janez", "Novak", "SI", "udelezenec2025");
    person(t, "Maja", "Zupan", "AT", "udelezenec2025", "neObvescaj");
    person(t, "Žiga", "Čuk", null);
    return janez;
  }

  private String tag(Team t, String name) {
    return tagId(t.ana(), t.groupId(), name);
  }

  @Test
  @DisplayName("AC-015-01 has Tags shows only Contacts with the Tag")
  void ac01501() {
    Team t = team();
    threeContacts(t);

    JsonNode page = list(t.petra(), t.groupId(), "hasTagIds=" + tag(t, "udelezenec2025"));

    assertThat(page.path("total").asInt()).isEqualTo(2);
    assertThat(names(page)).containsExactly("Janez Novak", "Maja Zupan");
  }

  @Test
  @DisplayName("AC-015-02 without Tags excludes Contacts with the Tag, combined with AND")
  void ac01502() {
    Team t = team();
    threeContacts(t);

    JsonNode page =
        list(
            t.petra(),
            t.groupId(),
            "hasTagIds=" + tag(t, "udelezenec2025") + "&withoutTagIds=" + tag(t, "neObvescaj"));

    assertThat(page.path("total").asInt()).isEqualTo(1);
    assertThat(names(page)).containsExactly("Janez Novak");
  }

  @Test
  @DisplayName("AC-015-03 an Attribute filter matches key and value case-insensitively")
  void ac01503() {
    Team t = team();
    String janez = threeContacts(t);
    edited(
        t.ana(),
        t.groupId(),
        janez,
        u -> u.set("attributes", Json.attributes("kompetenca", "spletnoProgramiranje")));

    JsonNode page =
        list(t.petra(), t.groupId(), "attributeKey=Kompetenca&attributeValue=SPLETNOPROGRAMIRANJE");

    assertThat(names(page)).containsExactly("Janez Novak");
  }

  @Test
  @DisplayName("AC-015-04 without filters every Contact is shown again")
  void ac01504() {
    Team t = team();
    threeContacts(t);
    list(t.petra(), t.groupId(), "hasTagIds=" + tag(t, "udelezenec2025"));

    JsonNode page = list(t.petra(), t.groupId(), "");

    assertThat(page.path("total").asInt()).isEqualTo(3);
  }

  @Test
  @DisplayName("AC-015-05 a country filter and a search combine with AND")
  void ac01505() {
    Team t = team();
    threeContacts(t);

    JsonNode page = list(t.petra(), t.groupId(), "country=SI&q=maja");

    assertThat(page.path("total").asInt()).isZero();
    assertThat(names(list(t.petra(), t.groupId(), "country=SI"))).containsExactly("Janez Novak");
  }

  @Test
  @DisplayName("EX-015-01 has Tags with two Tags shows only Contacts with both")
  void ex01501() {
    Team t = team();
    threeContacts(t);

    JsonNode page =
        list(
            t.petra(),
            t.groupId(),
            "hasTagIds=" + tag(t, "udelezenec2025") + "&hasTagIds=" + tag(t, "neObvescaj"));

    assertThat(names(page)).containsExactly("Maja Zupan");
  }

  @Test
  @DisplayName("EX-015-02 the same Tag in has and without gives an empty list")
  void ex01502() {
    Team t = team();
    threeContacts(t);
    String udel = tag(t, "udelezenec2025");

    JsonNode page = list(t.petra(), t.groupId(), "hasTagIds=" + udel + "&withoutTagIds=" + udel);

    assertThat(page.path("total").asInt()).isZero();
  }

  @Test
  @DisplayName("EX-015-03 an Attribute key without value shows every Contact with the key")
  void ex01503() {
    Team t = team();
    String janez = threeContacts(t);
    edited(t.ana(), t.groupId(), janez, u -> u.set("attributes", Json.attributes("jezik", "")));
    String ziga = list(t.ana(), t.groupId(), "q=ziga").path("items").get(0).path("id").asString();
    edited(
        t.ana(), t.groupId(), ziga, u -> u.set("attributes", Json.attributes("Jezik", "nemščina")));

    assertThat(names(list(t.petra(), t.groupId(), "attributeKey=jezik")))
        .containsExactly("Žiga Čuk", "Janez Novak");
  }

  @Test
  @DisplayName("EX-015-04 a Tag id of another Group in a filter is 404")
  void ex01504() {
    Team t = team();
    threeContacts(t);
    TestUser nina = user("nina.kos@example.si", "Nina Kos");
    String beta = group(nina, "Podjetje Beta");
    ObjectNode body = contactBody("firstName", "Peter", "lastName", "Kos");
    body.set("tags", Json.strings("vip"));
    contact(nina, beta, body);
    String foreignTag = tagId(nina, beta, "vip");

    Res res = api.get(t.petra(), t.path() + "/contacts?hasTagIds=" + foreignTag);

    assertThat(res.status()).isEqualTo(404);
    assertThat(res.body()).doesNotContain("Peter").doesNotContain("Novak");
  }

  @Test
  @DisplayName("EX-015-05 a Contact in the recycle bin that matches a filter is not shown")
  void ex01505() {
    Team t = team();
    String janez = threeContacts(t);
    String udel = tag(t, "udelezenec2025");
    moveToRecycleBin(t.ana(), t.groupId(), janez);

    assertThat(names(list(t.petra(), t.groupId(), "hasTagIds=" + udel)))
        .containsExactly("Maja Zupan");
  }
}
