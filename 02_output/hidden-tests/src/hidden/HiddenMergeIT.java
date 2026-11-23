package app.contacts.hidden;

import static org.assertj.core.api.Assertions.assertThat;

import app.contacts.hidden.support.HiddenTest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** Hidden cases of US-017, US-018, US-019, US-021 and BR-03. */
class HiddenMergeIT extends HiddenTest {

  private Person owner;
  private Person editor;
  private Person viewer;
  private String g;

  private void sejem() {
    owner = person("luka.zorman@example.org", "Luka Zorman");
    editor = person("tina.bergant@example.org", "Tina Bergant");
    viewer = person("rok.mlakar@example.org", "Rok Mlakar");
    g = group(owner, "Sejem Celje");
    join(g, owner, editor, "EDITOR");
    join(g, owner, viewer, "VIEWER");
  }

  /** "idA+idB: reasons" per pair, ids and reasons sorted. */
  private List<String> pairs() {
    List<String> r = new ArrayList<>();
    for (JsonNode p : get(viewer, "/groups/" + g + "/duplicates").json()) {
      List<String> ids = new ArrayList<>(List.of(p.path("first").path("id").asString(), p.path("second").path("id").asString()));
      ids.sort(null);
      List<String> reasons = new ArrayList<>();
      p.path("reasons").forEach(x -> reasons.add(x.asString()));
      reasons.sort(null);
      r.add(ids.get(0) + "+" + ids.get(1) + ": " + String.join(", ", reasons));
    }
    return r;
  }

  private static String pair(String a, String b, String reasons) {
    return (a.compareTo(b) < 0 ? a + "+" + b : b + "+" + a) + ": " + reasons;
  }

  private Answer undo(Person p, String mergeId, String primary) {
    return post(p, "/groups/" + g + "/merges/" + mergeId + "/undo", obj("versionNumber", version(p, g, primary)));
  }

  @Test
  @DisplayName("R-017-01 combination three Contacts with one e-mail give three pairs")
  void r017_01_combination() {
    sejem();
    String a = contact(owner, g, "firstName", "Eva", "email", "info@alfa.si");
    String b = contact(owner, g, "firstName", "Tim", "email", "INFO@alfa.si");
    String c = contact(owner, g, "firstName", "Rok", "email", "info@ALFA.SI");

    assertThat(pairs())
        .containsExactlyInAnyOrder(pair(a, b, "sameEmail"), pair(a, c, "sameEmail"), pair(b, c, "sameEmail"));
  }

  @Test
  @DisplayName("R-017-02 other e-mails entered with spaces and other case are the same")
  void r017_02_other() {
    sejem();
    String a = contact(owner, g, "firstName", "Eva", "email", "  eva@golob.si ");
    String b = contact(owner, g, "firstName", "Tim", "email", "EVA@GOLOB.SI");

    assertThat(pairs()).containsExactly(pair(a, b, "sameEmail"));
  }

  @Test
  @DisplayName("R-017-03 boundary phones of exactly 6 digits in other notation are a pair")
  void r017_03_boundary() {
    sejem();
    String a = contact(owner, g, "firstName", "Eva", "phone", "12-34-56");
    String b = contact(owner, g, "firstName", "Tim", "phone", "(123) 456");
    contact(owner, g, "firstName", "Rok", "phone", "1 2345");
    contact(owner, g, "firstName", "Ana", "phone", "12345");

    assertThat(pairs()).containsExactly(pair(a, b, "samePhone"));
  }

  @Test
  @DisplayName("R-017-04 boundary equal names and companies in other case pair; different names not")
  void r017_04_boundary() {
    sejem();
    String a = contact(owner, g, "firstName", "Eva", "lastName", "Golob", "company", "ALFA d.o.o.");
    String b = contact(owner, g, "firstName", "eva", "lastName", "golob", "company", "alfa D.O.O.");
    contact(owner, g, "firstName", "Tim", "lastName", "Hribar", "company", "Alfa d.o.o.");
    contact(owner, g, "firstName", "Eva", "lastName", "Golob");

    assertThat(pairs()).containsExactly(pair(a, b, "similarNameSameCompany"));
  }

  @Test
  @DisplayName("R-017-05 other listing duplicates leaves every Contact as it was")
  void r017_05_other() {
    sejem();
    String a = contact(owner, g, "firstName", "Eva", "email", "eva@golob.si");
    String b = contact(owner, g, "firstName", "Tim", "email", "eva@golob.si");
    String before = get(owner, path(g, a)).body() + get(owner, path(g, b)).body();

    pairs();
    pairs();

    assertThat(get(owner, path(g, a)).body() + get(owner, path(g, b)).body()).isEqualTo(before);
  }

  @Test
  @DisplayName("R-018-01 role the Owner may merge too")
  void r018_01_role() {
    sejem();
    String a = contact(owner, g, "firstName", "Eva", "email", "eva@golob.si");
    String b = contact(owner, g, "firstName", "E.", "email", "eva@golob.si");

    assertThat(merge(owner, g, a, b, null).status()).isEqualTo(201);
  }

  @Test
  @DisplayName("R-018-02 combination chosen Fields from both, all Attributes and Tags")
  void r018_02_combination() {
    sejem();
    ObjectNode pa = body("firstName", "Eva", "email", "eva@golob.si", "phone", "041 111 111");
    pa.set("attributes", attrs("regija", "Gorenjska"));
    pa.set("tags", arr("vip"));
    ObjectNode pb = body("firstName", "Eva", "email", "eva.golob@alfa.si", "phone", "041 222 222");
    pb.set("attributes", attrs("jezik", "en"));
    pb.set("tags", arr("novo"));
    String a = contact(owner, g, pa);
    String b = contact(owner, g, pb);

    merge(editor, g, a, b, obj("fieldChoices", obj("email", "other", "phone", "primary")));

    JsonNode m = read(owner, g, a);
    assertThat(m.path("fields").path("email").asString()).isEqualTo("eva.golob@alfa.si");
    assertThat(m.path("fields").path("phone").asString()).isEqualTo("041 111 111");
    assertThat(texts(m.path("attributes"), "key")).containsExactly("jezik", "regija");
    assertThat(texts(m.path("tags"), "name")).containsExactlyInAnyOrder("vip", "novo");
    assertThat(kinds(history(owner, g, a)).get(0)).isEqualTo("2 merged");
  }

  @Test
  @DisplayName("R-018-03 other the chosen value of a conflicting Attribute is kept")
  void r018_03_other() {
    sejem();
    ObjectNode pa = body("firstName", "Eva");
    pa.set("attributes", attrs("velikost", "M"));
    ObjectNode pb = body("firstName", "E.");
    pb.set("attributes", attrs("Velikost", "L"));
    String a = contact(owner, g, pa);
    String b = contact(owner, g, pb);

    Answer r = merge(editor, g, a, b, obj("attributeChoices", obj("velikost", "other")));

    assertThat(r.status()).isEqualTo(201);
    assertThat(texts(read(owner, g, a).path("attributes"), "value")).containsExactly("L");
  }

  @Test
  @DisplayName("R-018-04 other the Other contact is merged into the Primary; audited")
  void r018_04_other() {
    sejem();
    String a = contact(owner, g, "firstName", "Eva", "email", "eva@golob.si");
    String b = contact(owner, g, "firstName", "E.", "email", "eva@golob.si");
    List<Map<String, Object>> before = audit(g);

    merge(editor, g, a, b, null);

    JsonNode other = read(owner, g, b);
    assertThat(other.path("state").asString()).isEqualTo("merged");
    assertThat(other.path("mergedInto").path("id").asString()).isEqualTo(a);
    assertThat(kinds(history(owner, g, b))).containsExactly("2 merged", "1 created");
    List<Map<String, Object>> added = audit(g);
    added.removeAll(before);
    assertThat(added.stream().map(e -> e.get("user_id"))).contains(id(editor));
  }

  @Test
  @DisplayName("R-018-05 other the Primary contact lists the Merge with the Other contact")
  void r018_05_other() {
    sejem();
    String a = contact(owner, g, "firstName", "Eva", "lastName", "Golob");
    String b = contact(owner, g, "company", "Golob s.p.");

    merge(editor, g, a, b, null);

    JsonNode from = read(viewer, g, a).path("mergedFrom");
    assertThat(from.size()).isEqualTo(1);
    assertThat(from.get(0).path("contact").path("name").asString()).isEqualTo("Golob s.p.");
  }

  @Test
  @DisplayName("R-018-06 boundary 50 Attributes after shared keys merge; 51 distinct do not")
  void r018_06_boundary() {
    sejem();
    ArrayNode first = JSON.createArrayNode();
    ArrayNode second = JSON.createArrayNode();
    for (int i = 0; i < 30; i++) {
      first.add(obj("key", "k" + i, "value", "v"));
      second.add(obj("key", "k" + (i + 20), "value", "v"));
    }
    ObjectNode pa = body("firstName", "Eva");
    pa.set("attributes", first);
    ObjectNode pb = body("firstName", "E.");
    pb.set("attributes", second);
    String a = contact(owner, g, pa);
    String b = contact(owner, g, pb);
    ObjectNode pc = body("firstName", "Tim");
    pc.set("attributes", first);
    ArrayNode third = JSON.createArrayNode();
    for (int i = 0; i < 21; i++) {
      third.add(obj("key", "x" + i, "value", "v"));
    }
    ObjectNode pd = body("firstName", "T.");
    pd.set("attributes", third);
    String c = contact(owner, g, pc);
    String d = contact(owner, g, pd);

    assertThat(merge(editor, g, a, b, null).status()).isEqualTo(201);
    assertThat(read(owner, g, a).path("attributes").size()).isEqualTo(50);
    problem(merge(editor, g, c, d, null), 409, "merge.tooManyAttributes");
    assertThat(read(owner, g, d).path("state").asString()).isEqualTo("active");
  }

  @Test
  @DisplayName("R-018-07 invalid a Contact of another Group or an already merged one is refused")
  void r018_07_invalid() {
    sejem();
    String a = contact(owner, g, "firstName", "Eva");
    String b = contact(owner, g, "firstName", "E.");
    String c = contact(owner, g, "firstName", "Ema");
    String other = group(owner, "Sejem Kranj");
    String x = contact(owner, other, "firstName", "Eva");

    Answer foreign =
        post(editor, "/groups/" + g + "/merges",
            obj("primaryId", a, "otherId", x, "primaryVersionNumber", 1, "otherVersionNumber", 1));
    assertThat(foreign.status()).isBetween(400, 499);
    merge(editor, g, a, b, null);
    Answer again =
        post(editor, "/groups/" + g + "/merges",
            obj("primaryId", c, "otherId", b, "primaryVersionNumber", 1, "otherVersionNumber", 2));

    assertThat(again.status()).isEqualTo(409);
    assertThat(read(owner, g, c).path("versionNumber").asInt()).isEqualTo(1);
    assertThat(read(owner, other, x).path("state").asString()).isEqualTo("active");
  }

  @Test
  @DisplayName("R-019-01 other the Owner undoes: Attributes of both return to their Contacts")
  void r019_01_other() {
    sejem();
    ObjectNode pa = body("firstName", "Eva");
    pa.set("attributes", attrs("regija", "Gorenjska"));
    ObjectNode pb = body("firstName", "E.");
    pb.set("attributes", attrs("jezik", "en"));
    String a = contact(owner, g, pa);
    String b = contact(owner, g, pb);
    String mergeId = merge(editor, g, a, b, null).json().path("id").asString();

    assertThat(undo(owner, mergeId, a).status()).isEqualTo(200);

    assertThat(texts(read(owner, g, a).path("attributes"), "key")).containsExactly("regija");
    assertThat(texts(read(owner, g, b).path("attributes"), "key")).containsExactly("jezik");
    assertThat(read(owner, g, b).path("state").asString()).isEqualTo("active");
  }

  @Test
  @DisplayName("R-019-02 other a Tag added after the Merge blocks the undo")
  void r019_02_other() {
    sejem();
    String a = contact(owner, g, "firstName", "Eva");
    String b = contact(owner, g, "firstName", "E.");
    String mergeId = merge(editor, g, a, b, null).json().path("id").asString();
    save(editor, g, a, f -> f.set("tags", arr("vip")));

    problem(undo(editor, mergeId, a), 409, "merge.changedAfter");
    assertThat(read(owner, g, b).path("state").asString()).isEqualTo("merged");
  }

  @Test
  @DisplayName("R-019-03 combination in the bin with a stale Version number: restore first")
  void r019_03_combination() {
    sejem();
    String a = contact(owner, g, "firstName", "Eva");
    String b = contact(owner, g, "firstName", "E.");
    String mergeId = merge(editor, g, a, b, null).json().path("id").asString();
    bin(editor, g, a);

    Answer r = post(editor, "/groups/" + g + "/merges/" + mergeId + "/undo", obj("versionNumber", 2));

    problem(r, 409, "merge.restoreFirst");
  }

  @Test
  @DisplayName("R-021-01 other a Viewer deletes the account: Membership and account are gone")
  void r021_01_other() {
    sejem();

    assertThat(delete(viewer, "/me", obj("confirmEmail", "rok.mlakar@example.org")).status()).isEqualTo(204);

    assertThat(texts(get(owner, "/groups/" + g + "/members").json(), "email"))
        .doesNotContain("rok.mlakar@example.org");
    JsonNode found =
        emulator("POST", "/identitytoolkit.googleapis.com/v1/projects/demo-contacts/accounts:lookup", "owner",
            obj("email", arr("rok.mlakar@example.org")));
    assertThat(found.path("users").size()).isZero();
    assertThat(get(viewer, "/groups").status()).isEqualTo(401);
  }

  @Test
  @DisplayName("R-021-02 combination the Owner of an active and a deleted Group sees both names")
  void r021_02_combination() {
    sejem();
    String gone = group(owner, "Sejem Kranj");
    post(owner, "/groups/" + gone + "/move-to-recycle-bin", null);

    Answer r = delete(owner, "/me", obj("confirmEmail", "luka.zorman@example.org"));

    problem(r, 409, "account.ownsGroups");
    assertThat(r.body()).contains("Sejem Celje").contains("Sejem Kranj");
    assertThat(get(owner, "/me").status()).isEqualTo(200);
  }

  @Test
  @DisplayName("R-021-03 other every Version of a deleted User shows the deleted-user name")
  void r021_03_other() {
    sejem();
    String a = contact(editor, g, "firstName", "Eva");
    String b = contact(owner, g, "firstName", "Tim");
    save(editor, g, b, set("city", "Kranj"));

    delete(editor, "/me", obj("confirmEmail", "tina.bergant@example.org"));

    assertThat(history(owner, g, a).get(0).path("changedBy").asString()).isEqualTo("user.deleted");
    assertThat(texts(history(owner, g, b), "changedBy")).containsExactly("user.deleted", "Luka Zorman");
    assertThat(get(owner, "/groups/" + g + "/members").body()).doesNotContain("Tina Bergant");
  }

  @Test
  @DisplayName("BR-03 role a Viewer changes nothing: Tags, restore, merge, undo, bin restore")
  void br03_role() {
    sejem();
    String a = contact(owner, g, "firstName", "Eva");
    String b = contact(owner, g, "firstName", "E.");
    String c = contact(owner, g, "firstName", "Tim");
    save(owner, g, a, set("city", "Kranj"));
    String mergeId = merge(editor, g, a, b, null).json().path("id").asString();
    bin(editor, g, c);
    String before = get(owner, path(g, a)).body();

    assertThat(save(viewer, g, a, f -> f.set("tags", arr("vip"))).status()).isEqualTo(403);
    assertThat(post(viewer, path(g, a) + "/versions/1/restore", obj("versionNumber", 3)).status()).isEqualTo(403);
    assertThat(post(viewer, "/groups/" + g + "/merges/" + mergeId + "/undo", obj("versionNumber", 3)).status())
        .isEqualTo(403);
    assertThat(post(viewer, "/groups/" + g + "/recycle-bin/" + c + "/restore", obj("versionNumber", 2)).status())
        .isEqualTo(403);
    assertThat(get(owner, path(g, a)).body()).isEqualTo(before);
    assertThat(get(editor, "/groups/" + g + "/recycle-bin").json().size()).isEqualTo(1);
  }
}
