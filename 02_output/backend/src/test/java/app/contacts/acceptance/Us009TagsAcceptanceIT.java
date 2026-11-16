package app.contacts.acceptance;

import static app.contacts.acceptance.support.Versions.items;
import static org.assertj.core.api.Assertions.assertThat;

import app.contacts.acceptance.support.AcceptanceTest;
import app.contacts.acceptance.support.Json;
import app.contacts.acceptance.support.Res;
import app.contacts.acceptance.support.TestUser;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** US-009 Tags of a Contact: API part. */
class Us009TagsAcceptanceIT extends AcceptanceTest {

  private String person(Team t, String first, String last, String... tags) {
    ObjectNode body = contactBody("firstName", first, "lastName", last);
    body.set("tags", Json.strings(tags));
    return contact(t.ana(), t.groupId(), body);
  }

  private Res withTags(Team t, String contactId, String... tags) {
    return edit(t.marko(), t.groupId(), contactId, u -> u.set("tags", Json.strings(tags)));
  }

  private List<String> tagsOf(TestUser user, Team t, String contactId) {
    return Json.texts(getContact(user, t.groupId(), contactId).path("tags"), "name");
  }

  private List<String> suggestions(TestUser user, String groupId, String q) {
    Res res = api.get(user, "/groups/" + groupId + "/tags?q=" + q);
    assertThat(res.status()).as("suggestions: %s", res).isEqualTo(200);
    return Json.texts(res.json(), "name");
  }

  @Test
  @DisplayName("AC-009-01 an added Tag is created and listed as tag:<name> in Version 2")
  void ac00901() {
    Team t = team();
    String janez = person(t, "Janez", "Novak");

    Res res = withTags(t, janez, "udelezenec2025");

    assertThat(res.status()).isEqualTo(200);
    assertThat(tagsOf(t.petra(), t, janez)).containsExactly("udelezenec2025");
    JsonNode latest = history(t.petra(), t.groupId(), janez).get(0);
    assertThat(latest.path("number").asInt()).isEqualTo(2);
    assertThat(items(latest)).containsExactly("tag:udelezenec2025");
  }

  @Test
  @DisplayName("AC-009-02 typing part of a Tag suggests the Tag")
  void ac00902() {
    Team t = team();
    person(t, "Janez", "Novak", "udelezenec2025");
    person(t, "Maja", "Zupan");

    assertThat(suggestions(t.marko(), t.groupId(), "udel")).containsExactly("udelezenec2025");
  }

  @Test
  @DisplayName("AC-009-03 a Tag added in other case uses the existing Tag and spelling")
  void ac00903() {
    Team t = team();
    person(t, "Janez", "Novak", "udelezenec2025");
    String maja = person(t, "Maja", "Zupan");

    Res res = withTags(t, maja, "Udelezenec2025");

    assertThat(res.status()).isEqualTo(200);
    assertThat(tagsOf(t.ana(), t, maja)).containsExactly("udelezenec2025");
    assertThat(suggestions(t.ana(), t.groupId(), "udelezenec2025"))
        .containsExactly("udelezenec2025");
  }

  @Test
  @DisplayName("AC-009-04 a removed Tag is listed as tag:<name> in the Version")
  void ac00904() {
    Team t = team();
    String janez = person(t, "Janez", "Novak", "udelezenec2025", "neObvescaj");

    withTags(t, janez, "udelezenec2025");

    assertThat(tagsOf(t.ana(), t, janez)).containsExactly("udelezenec2025");
    assertThat(items(history(t.ana(), t.groupId(), janez).get(0)))
        .containsExactly("tag:neObvescaj");
  }

  @Test
  @DisplayName("AC-009-05 a Tag with a comma is rejected and nothing stored")
  void ac00905() {
    Team t = team();
    String janez = person(t, "Janez", "Novak");
    final String before = state(t.ana(), contactPath(t.groupId(), janez));

    Res res = withTags(t, janez, "vip, 2025");

    assertProblem(res, 400, "tag.invalid");
    assertThat(state(t.ana(), contactPath(t.groupId(), janez))).isEqualTo(before);
  }

  @Test
  @DisplayName("EX-009-01 a Tag of 50 characters is saved, of 51 rejected")
  void ex00901() {
    Team t = team();
    String janez = person(t, "Janez", "Novak");

    Res ok = withTags(t, janez, "t".repeat(50));
    final String before = state(t.ana(), contactPath(t.groupId(), janez));
    Res tooLong = withTags(t, janez, "t".repeat(51));

    assertThat(ok.status()).isEqualTo(200);
    assertProblem(tooLong, 400, "tag.invalid");
    assertThat(state(t.ana(), contactPath(t.groupId(), janez))).isEqualTo(before);
  }

  @Test
  @DisplayName("EX-009-02 a Tag of only spaces is rejected")
  void ex00902() {
    Team t = team();
    String janez = person(t, "Janez", "Novak");
    final String before = state(t.ana(), contactPath(t.groupId(), janez));

    Res res = withTags(t, janez, "   ");

    assertProblem(res, 400, "tag.invalid");
    assertThat(state(t.ana(), contactPath(t.groupId(), janez))).isEqualTo(before);
  }

  @Test
  @DisplayName("EX-009-03 the same Tag added twice in one save is stored once")
  void ex00903() {
    Team t = team();
    String janez = person(t, "Janez", "Novak");

    withTags(t, janez, "vip", "vip");

    assertThat(tagsOf(t.ana(), t, janez)).containsExactly("vip");
  }

  @Test
  @DisplayName("EX-009-04 a Tag of another Group is not suggested; a separate Tag is created")
  void ex00904() {
    Team t = team();
    TestUser nina = user("nina.kos@example.si", "Nina Kos");
    String beta = group(nina, "Podjetje Beta");
    ObjectNode body = contactBody("firstName", "Maja", "lastName", "Zupan");
    body.set("tags", Json.strings("udelezenec2025"));
    contact(nina, beta, body);
    String janez = person(t, "Janez", "Novak");

    assertThat(suggestions(t.marko(), t.groupId(), "udel")).isEmpty();
    withTags(t, janez, "udelezenec2025");

    String ours = tagId(t.marko(), t.groupId(), "udelezenec2025");
    String theirs = tagId(nina, beta, "udelezenec2025");
    assertThat(ours).isNotEqualTo(theirs);
  }

  @Test
  @DisplayName("EX-009-05 a Tag on no active Contact is not suggested")
  void ex00905() {
    Team t = team();
    String janez = person(t, "Janez", "Novak", "udelezenec2025");
    withTags(t, janez);

    assertThat(suggestions(t.marko(), t.groupId(), "udel")).isEmpty();
  }

  @Test
  @DisplayName("EX-009-06 a Tag with diacritics is saved as entered")
  void ex00906() {
    Team t = team();
    String janez = person(t, "Janez", "Novak");

    withTags(t, janez, "udeležba");

    assertThat(tagsOf(t.ana(), t, janez)).containsExactly("udeležba");
  }

  @Test
  @DisplayName("EX-009-07 a Tag save with a stale Version number is rejected as BR-06")
  void ex00907() {
    Team t = team();
    String janez = person(t, "Janez", "Novak");
    ObjectNode stale = updateOf(getContact(t.marko(), t.groupId(), janez));
    edited(t.ana(), t.groupId(), janez, field("city", "Maribor"));
    stale.set("tags", Json.strings("vip"));
    final String before = state(t.ana(), contactPath(t.groupId(), janez));

    Res res = api.put(t.marko(), contactPath(t.groupId(), janez), stale);

    assertProblem(res, 409, "version.conflict");
    assertThat(state(t.ana(), contactPath(t.groupId(), janez))).isEqualTo(before);
  }
}
