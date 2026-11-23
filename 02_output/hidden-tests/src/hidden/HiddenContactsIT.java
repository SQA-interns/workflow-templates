package app.contacts.hidden;

import static org.assertj.core.api.Assertions.assertThat;

import app.contacts.hidden.support.HiddenTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** Hidden cases of US-006, US-007, US-008, US-009 and BR-11. */
class HiddenContactsIT extends HiddenTest {

  private Person owner;
  private Person editor;
  private String g;

  private void sejem() {
    owner = person("luka.zorman@example.org", "Luka Zorman");
    editor = person("tina.bergant@example.org", "Tina Bergant");
    g = group(owner, "Sejem Celje");
    join(g, owner, editor, "EDITOR");
  }

  private int total() {
    return page(owner, g, "").path("total").asInt();
  }

  private Answer create(String... fields) {
    return post(editor, "/groups/" + g + "/contacts", body(fields));
  }

  @Test
  @DisplayName("R-006-01 role the Owner adds a Contact: Version 1 created by her")
  void r006_01_role() {
    sejem();

    String c = contact(owner, g, "firstName", "Eva", "lastName", "Golob");

    JsonNode v = history(editor, g, c).get(0);
    assertThat(v.path("kind").asString()).isEqualTo("created");
    assertThat(v.path("changedBy").asString()).isEqualTo("Luka Zorman");
  }

  @Test
  @DisplayName("R-006-02 combination only a last name is enough; null and blank parts are not")
  void r006_02_combination() {
    sejem();

    assertThat(create("lastName", "Golob").status()).isEqualTo(201);
    problem(
        post(editor, "/groups/" + g + "/contacts", obj("fields", obj("firstName", null, "company", " "))),
        400,
        "contact.nameRequired");
    assertThat(total()).isEqualTo(1);
  }

  @Test
  @DisplayName("R-006-03 boundary maximum lengths of other Fields")
  void r006_03_boundary() {
    sejem();

    assertThat(create("company", "C".repeat(200)).status()).isEqualTo(201);
    assertThat(create("firstName", "P", "phone", "1".repeat(30)).status()).isEqualTo(201);
    problem(create("company", "C".repeat(201)), 400, "field.tooLong");
    problem(create("firstName", "P", "phone", "1".repeat(31)), 400, "field.tooLong");
    problem(create("firstName", "P", "postalCode", "1".repeat(21)), 400, "field.tooLong");
    problem(create("firstName", "P", "street", "s".repeat(201)), 400, "field.tooLong");
    problem(create("firstName", "P", "city", "c".repeat(101)), 400, "field.tooLong");
    problem(create("lastName", "l".repeat(101)), 400, "field.tooLong");
    problem(create("firstName", "P", "jobTitle", "j".repeat(101)), 400, "field.tooLong");
    assertThat(total()).isEqualTo(2);
  }

  @Test
  @DisplayName("R-006-04 invalid e-mails with a space, no local part, no dot in the domain")
  void r006_04_invalid() {
    sejem();

    for (String email : new String[] {"eva golob@example.org", "@example.org", "eva@example", "eva@@x.si"}) {
      problem(create("firstName", "Eva", "email", email), 400, "field.emailInvalid");
    }
    assertThat(create("firstName", "Eva", "email", "e@x.si").status()).isEqualTo(201);
    assertThat(total()).isEqualTo(1);
  }

  @Test
  @DisplayName("R-006-05 invalid lower-case or unknown country codes are 400")
  void r006_05_invalid() {
    sejem();

    assertThat(create("firstName", "Eva", "country", "si").status()).isEqualTo(400);
    assertThat(create("firstName", "Eva", "country", "ZZ").status()).isEqualTo(400);
    assertThat(create("firstName", "Eva", "country", "HR").status()).isEqualTo(201);
    assertThat(total()).isEqualTo(1);
  }

  @Test
  @DisplayName("R-006-06 other e-mail matches ignore case, the bin and the Contact itself")
  void r006_06_other() {
    sejem();
    String eva = contact(owner, g, "firstName", "Eva", "email", "eva@golob.si");
    String gone = contact(owner, g, "firstName", "Ema", "email", "EVA@golob.si");
    bin(owner, g, gone);

    JsonNode m = get(editor, "/groups/" + g + "/contacts/email-matches?email=Eva@Golob.SI").json();
    JsonNode self =
        get(editor, "/groups/" + g + "/contacts/email-matches?email=eva@golob.si&excludeContactId=" + eva)
            .json();

    assertThat(texts(m, "id")).containsExactly(eva);
    assertThat(self.size()).isZero();
  }

  @Test
  @DisplayName("R-006-07 other every text Field is trimmed")
  void r006_07_other() {
    sejem();

    JsonNode f = create("firstName", " Eva ", "company", "  Alfa  ", "city", " Kranj", "note", "x  ").json().path("fields");

    assertThat(f.path("firstName").asString()).isEqualTo("Eva");
    assertThat(f.path("company").asString()).isEqualTo("Alfa");
    assertThat(f.path("city").asString()).isEqualTo("Kranj");
    assertThat(f.path("note").asString()).isEqualTo("x");
  }

  @Test
  @DisplayName("R-007-01 combination three Fields in one save: one Version with three items")
  void r007_01_combination() {
    sejem();
    String c = contact(owner, g, "firstName", "Eva", "city", "Kranj");

    save(editor, g, c, set("city", "Koper").andThen(set("jobTitle", "Vodja")).andThen(set("lastName", "Golob")));

    JsonNode h = history(owner, g, c);
    assertThat(h.size()).isEqualTo(2);
    assertThat(changes(h.get(0)))
        .containsExactlyInAnyOrder("city: Kranj → Koper", "jobTitle: ∅ → Vodja", "lastName: ∅ → Golob");
    assertThat(h.get(0).path("kind").asString()).isEqualTo("edited");
  }

  @Test
  @DisplayName("R-007-02 invalid an edit to a wrong e-mail or country is rejected")
  void r007_02_invalid() {
    sejem();
    String c = contact(owner, g, "firstName", "Eva");
    String before = get(owner, path(g, c)).body();

    problem(save(editor, g, c, set("email", "eva@golob")), 400, "field.emailInvalid");
    assertThat(save(editor, g, c, set("country", "XX")).status()).isEqualTo(400);
    problem(save(editor, g, c, set("firstName", "")), 400, "contact.nameRequired");
    assertThat(get(owner, path(g, c)).body()).isEqualTo(before);
  }

  @Test
  @DisplayName("R-007-03 other the same Tags and Attributes in another order are no change")
  void r007_03_other() {
    sejem();
    ObjectNode b = body("firstName", "Eva");
    b.set("tags", arr("a", "b"));
    b.set("attributes", attrs("x", "1", "y", "2"));
    String c = contact(owner, g, b);

    Answer a =
        save(
            editor,
            g,
            c,
            f -> {
              f.set("tags", arr("b", "a"));
              f.set("attributes", attrs("y", "2", "x", "1"));
            });

    problem(a, 409, "contact.noChanges");
    assertThat(history(owner, g, c).size()).isEqualTo(1);
  }

  @Test
  @DisplayName("R-007-04 boundary a Version number one below current is rejected with the name")
  void r007_04_boundary() {
    sejem();
    String c = contact(owner, g, "firstName", "Eva");
    save(owner, g, c, set("city", "Kranj"));
    save(owner, g, c, set("city", "Koper"));
    ObjectNode f = form(read(editor, g, c));
    f.put("versionNumber", 2);
    ((ObjectNode) f.path("fields")).put("city", "Bled");

    Answer a = put(editor, path(g, c), f);

    problem(a, 409, "version.conflict");
    assertThat(a.json().path("params").path("changedBy").asString()).isEqualTo("Luka Zorman");
    assertThat(read(owner, g, c).path("fields").path("city").asString()).isEqualTo("Koper");
  }

  @Test
  @DisplayName("R-008-01 combination add two, change one, remove one Attribute in one Version")
  void r008_01_combination() {
    sejem();
    ObjectNode b = body("firstName", "Eva");
    b.set("attributes", attrs("velikost", "M", "barva", "rdeča"));
    String c = contact(owner, g, b);

    save(editor, g, c, f -> f.set("attributes", attrs("velikost", "L", "jezik", "en", "regija", "Gorenjska")));

    assertThat(changes(history(owner, g, c).get(0)))
        .containsExactlyInAnyOrder(
            "attribute:velikost: M → L",
            "attribute:barva: rdeča → ∅",
            "attribute:jezik: ∅ → en",
            "attribute:regija: ∅ → Gorenjska");
  }

  @Test
  @DisplayName("R-008-02 boundary a key of 1 character, and 50 after trimming, is saved")
  void r008_02_boundary() {
    sejem();
    String c = contact(owner, g, "firstName", "Eva");

    Answer a = save(editor, g, c, f -> f.set("attributes", attrs("k", "1", "  " + "q".repeat(50) + "  ", "2")));

    assertThat(a.status()).isEqualTo(200);
    assertThat(texts(read(owner, g, c).path("attributes"), "key")).containsExactly("k", "q".repeat(50));
    problem(save(editor, g, c, f -> f.set("attributes", attrs("", "x"))), 400, "attribute.keyLength");
  }

  @Test
  @DisplayName("R-008-03 boundary values of 500 and 501 letters with diacritics")
  void r008_03_boundary() {
    sejem();
    String c = contact(owner, g, "firstName", "Eva");

    assertThat(save(editor, g, c, f -> f.set("attributes", attrs("opis", "ž".repeat(500)))).status()).isEqualTo(200);
    problem(save(editor, g, c, f -> f.set("attributes", attrs("opis", "ž".repeat(501)))), 400, "attribute.valueTooLong");
    assertThat(read(owner, g, c).path("attributes").get(0).path("value").asString()).hasSize(500);
  }

  @Test
  @DisplayName("R-008-04 invalid keys equal after trimming and case are used twice")
  void r008_04_invalid() {
    sejem();
    String c = contact(owner, g, "firstName", "Eva");

    Answer a = save(editor, g, c, f -> f.set("attributes", attrs(" velikost", "M", "VELIKOST ", "L")));

    problem(a, 400, "attribute.keyTwice");
    assertThat(read(owner, g, c).path("attributes").size()).isZero();
  }

  @Test
  @DisplayName("R-008-05 boundary a new Contact with 51 Attributes is rejected, 50 accepted")
  void r008_05_boundary() {
    sejem();
    ArrayNode fifty = JSON.createArrayNode();
    for (int i = 0; i < 51; i++) {
      fifty.add(obj("key", "a" + i, "value", "v"));
    }
    ObjectNode tooMany = body("firstName", "Eva");
    tooMany.set("attributes", fifty);
    problem(post(editor, "/groups/" + g + "/contacts", tooMany), 400, "attribute.tooMany");
    fifty.remove(50);
    ObjectNode ok = body("firstName", "Eva");
    ok.set("attributes", fifty);

    assertThat(post(editor, "/groups/" + g + "/contacts", ok).status()).isEqualTo(201);
    assertThat(total()).isEqualTo(1);
  }

  @Test
  @DisplayName("R-008-06 other Attributes are returned by key A–Z with their spelling")
  void r008_06_other() {
    sejem();
    ObjectNode b = body("firstName", "Eva");
    b.set("attributes", attrs("zeta", "3", "Alfa", "1", "beta", "2"));

    String c = contact(owner, g, b);

    assertThat(texts(read(editor, g, c).path("attributes"), "key")).containsExactly("Alfa", "beta", "zeta");
  }

  @Test
  @DisplayName("R-009-01 combination two Tags added and one removed: one Version, three items")
  void r009_01_combination() {
    sejem();
    ObjectNode b = body("firstName", "Eva");
    b.set("tags", arr("staro"));
    String c = contact(owner, g, b);

    save(editor, g, c, f -> f.set("tags", arr("novo", "vip")));

    JsonNode h = history(owner, g, c);
    assertThat(h.size()).isEqualTo(2);
    assertThat(texts(h.get(0).path("changes"), "item"))
        .containsExactlyInAnyOrder("tag:staro", "tag:novo", "tag:vip");
  }

  @Test
  @DisplayName("R-009-02 other suggestions match inside the name, ignore case, skip the bin")
  void r009_02_other() {
    sejem();
    ObjectNode a = body("firstName", "Eva");
    a.set("tags", arr("razstavljavec"));
    contact(owner, g, a);
    ObjectNode b = body("firstName", "Ema");
    b.set("tags", arr("razpis"));
    String gone = contact(owner, g, b);
    bin(owner, g, gone);

    assertThat(texts(get(editor, "/groups/" + g + "/tags?q=STAVLJ").json(), "name"))
        .containsExactly("razstavljavec");
    assertThat(texts(get(editor, "/groups/" + g + "/tags?q=razp").json(), "name")).isEmpty();
  }

  @Test
  @DisplayName("R-009-03 other an upper-case Tag uses the existing lower-case Tag")
  void r009_03_other() {
    sejem();
    ObjectNode a = body("firstName", "Eva");
    a.set("tags", arr("vip"));
    contact(owner, g, a);
    String c = contact(owner, g, "firstName", "Ema");

    save(editor, g, c, f -> f.set("tags", arr("VIP")));

    assertThat(texts(read(owner, g, c).path("tags"), "name")).containsExactly("vip");
  }

  @Test
  @DisplayName("R-009-04 other a new Tag keeps its spelling with diacritics and case")
  void r009_04_other() {
    sejem();
    String c = contact(owner, g, "firstName", "Eva");

    save(editor, g, c, f -> f.set("tags", arr("NeObveščaj")));

    assertThat(texts(read(owner, g, c).path("tags"), "name")).containsExactly("NeObveščaj");
  }

  @Test
  @DisplayName("R-009-05 boundary a 1-character Tag is saved; a trailing comma is rejected")
  void r009_05_boundary() {
    sejem();
    String c = contact(owner, g, "firstName", "Eva");

    assertThat(save(editor, g, c, f -> f.set("tags", arr("  x  "))).status()).isEqualTo(200);
    assertThat(texts(read(owner, g, c).path("tags"), "name")).containsExactly("x");
    problem(save(editor, g, c, f -> f.set("tags", arr("x", "vip,"))), 400, "tag.invalid");
    assertThat(texts(read(owner, g, c).path("tags"), "name")).containsExactly("x");
  }

  @Test
  @DisplayName("R-009-06 other the same Tag in two spellings on a new Contact is stored once")
  void r009_06_other() {
    sejem();
    ObjectNode b = body("firstName", "Eva");
    b.set("tags", arr("vip", "VIP", " Vip "));

    String c = contact(editor, g, b);

    assertThat(read(owner, g, c).path("tags").size()).isEqualTo(1);
  }

  @Test
  @DisplayName("BR-11 other Attribute keys and e-mails compare case-insensitively in filters")
  void br11_other() {
    sejem();
    ObjectNode b = body("firstName", "Eva", "email", "Eva@Golob.si");
    b.set("attributes", attrs("Regija", "Gorenjska"));
    contact(owner, g, b);

    assertThat(names(page(editor, g, "attributeKey=REGIJA&attributeValue=gorenjska")))
        .containsExactly("Eva");
    assertThat(texts(get(editor, "/groups/" + g + "/contacts/email-matches?email=eva@golob.SI").json(), "name"))
        .hasSize(1);
  }
}
