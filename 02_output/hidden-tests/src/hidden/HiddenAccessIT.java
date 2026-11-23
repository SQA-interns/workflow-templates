package app.contacts.hidden;

import static org.assertj.core.api.Assertions.assertThat;

import app.contacts.hidden.support.HiddenTest;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Hidden cases of US-001, US-002, US-020 and BR-01, BR-02. */
class HiddenAccessIT extends HiddenTest {

  @Test
  @DisplayName("R-001-01 other Google and e-mail accounts both reach the API")
  void r001_01_other() {
    Person luka = account("luka.zorman@example.org", "Luka Zorman", true);
    Person tina = google("tina.bergant@gmail.com", "Tina Bergant");

    assertThat(get(luka, "/me").json().path("email").asString()).isEqualTo("luka.zorman@example.org");
    assertThat(get(tina, "/me").json().path("email").asString()).isEqualTo("tina.bergant@gmail.com");
  }

  @Test
  @DisplayName("R-001-02 other a valid token is accepted on a change request")
  void r001_02_other() {
    Person luka = account("luka.zorman@example.org", "Luka Zorman", true);

    assertThat(post(luka, "/groups", obj("name", "Sejem Celje")).status()).isEqualTo(201);
  }

  @Test
  @DisplayName("R-001-03 invalid garbage, empty and just-expired tokens are 401")
  void r001_03_invalid() {
    Person luka = person("luka.zorman@example.org", "Luka Zorman");
    group(luka, "Sejem Celje");
    long now = Instant.now().getEpochSecond();
    String justExpired =
        claims(
            luka.token(),
            c -> {
              c.put("iat", now - 3900);
              c.put("auth_time", now - 3900);
              c.put("exp", now - 600);
            });

    for (String token : new String[] {"abc.def.ghi", "", justExpired}) {
      Answer a = call("GET", "/groups", token, null);
      assertThat(a.status()).as(token).isEqualTo(401);
      assertThat(a.body()).doesNotContain("Sejem Celje");
    }
  }

  @Test
  @DisplayName("R-001-04 other the first call creates the User; name from the e-mail if none")
  void r001_04_other() {
    Person named = account("luka.zorman@example.org", "Luka Zorman", true);
    Person unnamed = account("tina.bergant@example.org", null, true);

    assertThat(post(named, "/groups", obj("name", "Sejem Celje")).status()).isEqualTo(201);
    var me = get(named, "/me").json();
    assertThat(me.path("displayName").asString()).isEqualTo("Luka Zorman");
    assertThat(me.path("language").asString()).isEqualTo("sl");
    assertThat(get(unnamed, "/me").json().path("displayName").asString()).isEqualTo("tina.bergant");
  }

  @Test
  @DisplayName("R-001-05 invalid an unverified account may only read /me")
  void r001_05_invalid() {
    Person p = account("luka.zorman@example.org", null, false);

    assertThat(get(p, "/me").status()).isEqualTo(200);
    problem(patch(p, "/me", obj("language", "en")), 403, "auth.emailNotVerified");
    problem(post(p, "/groups", obj("name", "Sejem")), 403, "auth.emailNotVerified");
    assertThat(get(p, "/me").json().path("language").asString()).isEqualTo("sl");
  }

  @Test
  @DisplayName("R-001-06 other without a token even /me is 401")
  void r001_06_other() {
    person("luka.zorman@example.org", "Luka Zorman");

    assertThat(call("GET", "/me", null, null).status()).isEqualTo(401);
  }

  @Test
  @DisplayName("R-001-07 other a User removed from his only Group has no Groups")
  void r001_07_other() {
    Person owner = person("luka.zorman@example.org", "Luka Zorman");
    Person tina = person("tina.bergant@example.org", "Tina Bergant");
    String g = group(owner, "Sejem Celje");
    join(g, owner, tina, "VIEWER");

    delete(owner, "/groups/" + g + "/members/" + id(tina), null);

    assertThat(get(tina, "/groups").json().size()).isZero();
  }

  @Test
  @DisplayName("R-002-01 other a Group with diacritics: creator is its only Member, Owner")
  void r002_01_other() {
    Person p = person("luka.zorman@example.org", "Luka Zorman");

    String g = group(p, "Društvo Šola");

    var members = get(p, "/groups/" + g + "/members").json();
    assertThat(texts(members, "role")).containsExactly("OWNER");
    assertThat(groups(p)).containsExactly("Društvo Šola");
  }

  @Test
  @DisplayName("R-002-02 boundary names of 1 character, 100 after trimming, and only spaces")
  void r002_02_boundary() {
    Person p = person("luka.zorman@example.org", "Luka Zorman");

    assertThat(post(p, "/groups", obj("name", "A")).status()).isEqualTo(201);
    assertThat(post(p, "/groups", obj("name", "  " + "B".repeat(100) + "  ")).status())
        .isEqualTo(201);
    problem(post(p, "/groups", obj("name", " ".repeat(100))), 400, "group.nameLength");
    problem(post(p, "/groups", obj("name", "")), 400, "group.nameLength");
    assertThat(groups(p)).containsExactly("A", "B".repeat(100));
  }

  @Test
  @DisplayName("R-002-03 combination sorted A–Z in Slovenian order, without Groups in the bin")
  void r002_03_combination() {
    Person p = person("luka.zorman@example.org", "Luka Zorman");
    group(p, "Društvo");
    group(p, "Čebelarji");
    group(p, "Cvetličarna");
    String gone = group(p, "Avtošola");
    post(p, "/groups/" + gone + "/move-to-recycle-bin", null);

    assertThat(groups(p)).containsExactly("Cvetličarna", "Čebelarji", "Društvo");
  }

  @Test
  @DisplayName("R-002-04 other each Group's list holds only its own Contacts")
  void r002_04_other() {
    Person p = person("luka.zorman@example.org", "Luka Zorman");
    String a = group(p, "Sejem Celje");
    String b = group(p, "Sejem Gornja Radgona");
    contact(p, a, "firstName", "Rok", "lastName", "Mlakar");
    contact(p, b, "firstName", "Eva", "lastName", "Golob");
    contact(p, b, "firstName", "Tim", "lastName", "Hribar");

    assertThat(names(page(p, a, ""))).containsExactly("Rok Mlakar");
    assertThat(names(page(p, b, ""))).containsExactly("Eva Golob", "Tim Hribar");
  }

  @Test
  @DisplayName("R-002-05 role a Group in the bin is not available to its Editor either")
  void r002_05_role() {
    Person owner = person("luka.zorman@example.org", "Luka Zorman");
    Person ed = person("tina.bergant@example.org", "Tina Bergant");
    String g = group(owner, "Sejem Celje");
    String h = group(owner, "Avtošola");
    join(g, owner, ed, "EDITOR");
    join(h, owner, ed, "EDITOR");

    post(owner, "/groups/" + g + "/move-to-recycle-bin", null);

    assertThat(groups(ed)).containsExactly("Avtošola");
  }

  @Test
  @DisplayName("R-020-01 other a new Google User has the language sl")
  void r020_01_other() {
    Person tina = google("tina.bergant@gmail.com", "Tina Bergant");

    assertThat(get(tina, "/me").json().path("language").asString()).isEqualTo("sl");
  }

  @Test
  @DisplayName("R-020-02 other English and back to Slovenian is kept")
  void r020_02_other() {
    Person p = person("luka.zorman@example.org", "Luka Zorman");

    patch(p, "/me", obj("language", "en"));
    patch(p, "/me", obj("language", "sl"));

    Person again = new Person(p.email(), p.uid(), p.name(), signIn(p.email()));
    assertThat(get(again, "/me").json().path("language").asString()).isEqualTo("sl");
    assertThat(patch(p, "/me", obj("language", "SL")).status()).isEqualTo(400);
  }

  @Test
  @DisplayName("R-020-03 other timestamps are exact UTC instants for any display language")
  void r020_03_other() {
    Person p = person("luka.zorman@example.org", "Luka Zorman");
    patch(p, "/me", obj("language", "en"));
    String g = group(p, "Sejem Celje");
    clock.at(Instant.parse("2026-08-14T22:05:00Z"));
    String c = contact(p, g, "firstName", "Rok");

    String at = history(p, g, c).get(0).path("changedAt").asString();
    assertThat(Instant.parse(at)).isEqualTo(Instant.parse("2026-08-14T22:05:00Z"));
  }

  @Test
  @DisplayName("BR-01 role a non-Member sees no Tags, Versions or Contacts of the Group")
  void br01_role() {
    Person owner = person("luka.zorman@example.org", "Luka Zorman");
    Person out = person("tina.bergant@example.org", "Tina Bergant");
    String g = group(owner, "Sejem Celje");
    String c = contact(owner, g, body("firstName", "Rok"));
    save(owner, g, c, f -> f.set("tags", arr("razstavljavec")));

    for (String p :
        new String[] {"/groups/" + g + "/tags?q=raz", path(g, c) + "/versions", path(g, c)}) {
      Answer a = get(out, p);
      assertThat(a.status()).as(p).isEqualTo(404);
      assertThat(a.body()).doesNotContain("razstavljavec").doesNotContain("Rok");
    }
  }

  @Test
  @DisplayName("BR-02 other every Group endpoint answers a non-Member 404 without data")
  void br02_other() {
    Person owner = person("luka.zorman@example.org", "Luka Zorman");
    Person out = person("tina.bergant@example.org", "Tina Bergant");
    String g = group(owner, "Sejem Celje");
    String c = contact(owner, g, "firstName", "Rok", "lastName", "Mlakar", "email", "rok@m.si");

    String[] gets = {
      "/groups/" + g + "/members",
      "/groups/" + g + "/invitations",
      "/groups/" + g + "/recycle-bin",
      "/groups/" + g + "/duplicates",
      path(g, c) + "/versions/1",
      "/groups/" + g + "/contacts/email-matches?email=rok@m.si"
    };
    for (String p : gets) {
      Answer a = get(out, p);
      assertThat(a.status()).as(p).isEqualTo(404);
      assertThat(a.body()).doesNotContain("Mlakar").doesNotContain("rok@m.si");
    }
    assertThat(post(out, "/groups/" + g + "/exports", obj()).status()).isEqualTo(404);
    assertThat(patch(out, "/groups/" + g, obj("name", "X")).status()).isEqualTo(404);
  }
}
