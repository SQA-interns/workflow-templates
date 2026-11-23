package app.contacts.hidden;

import static org.assertj.core.api.Assertions.assertThat;

import app.contacts.hidden.support.HiddenTest;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** Hidden cases of US-010 … US-013 and BR-05 … BR-08. */
class HiddenHistoryIT extends HiddenTest {

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

  private String eva(String... tags) {
    ObjectNode b = body("firstName", "Eva", "lastName", "Golob", "city", "Kranj");
    b.set("tags", arr(tags));
    b.set("attributes", attrs("velikost", "M"));
    return contact(owner, g, b);
  }

  private Answer restore(Person p, String c, int n) {
    return post(p, path(g, c) + "/versions/" + n + "/restore", obj("versionNumber", version(p, g, c)));
  }

  private Answer restoreFromBin(Person p, String c, int versionNumber) {
    return post(p, "/groups/" + g + "/recycle-bin/" + c + "/restore", obj("versionNumber", versionNumber));
  }

  @Test
  @DisplayName("R-010-01 other three Versions newest first with names and instants")
  void r010_01_other() {
    sejem();
    clock.at(Instant.parse("2026-02-01T07:00:00Z"));
    String c = eva();
    clock.at(Instant.parse("2026-06-15T21:59:00Z"));
    save(editor, g, c, set("city", "Koper"));
    clock.at(Instant.parse("2026-10-25T00:30:00Z"));
    save(owner, g, c, set("city", "Bled"));

    JsonNode h = history(viewer, g, c);

    assertThat(kinds(h)).containsExactly("3 edited", "2 edited", "1 created");
    assertThat(texts(h, "changedBy")).containsExactly("Luka Zorman", "Tina Bergant", "Luka Zorman");
    assertThat(Instant.parse(h.get(1).path("changedAt").asString()))
        .isEqualTo(Instant.parse("2026-06-15T21:59:00Z"));
    assertThat(changes(h.get(0))).containsExactly("city: Koper → Bled");
  }

  @Test
  @DisplayName("R-010-02 other Version 2 shows Fields, Attributes and Tags as after Version 2")
  void r010_02_other() {
    sejem();
    String c = eva("vip");
    save(editor, g, c, f -> {
      f.set("tags", arr("vip", "novo"));
      f.set("attributes", attrs("velikost", "L"));
    });
    save(editor, g, c, set("city", "Bled").andThen(f -> f.set("tags", arr())));

    JsonNode s = get(viewer, path(g, c) + "/versions/2").json().path("snapshot");

    assertThat(s.path("fields").path("city").asString()).isEqualTo("Kranj");
    assertThat(s.path("attributes").get(0).path("value").asString()).isEqualTo("L");
    List<String> tags = new java.util.ArrayList<>();
    s.path("tags").forEach(t -> tags.add(t.asString()));
    assertThat(tags).containsExactlyInAnyOrder("vip", "novo");
  }

  @Test
  @DisplayName("R-010-03 other later changes and restores leave earlier Versions unchanged")
  void r010_03_other() {
    sejem();
    String c = eva();
    String v1 = get(owner, path(g, c) + "/versions/1").body();
    save(editor, g, c, set("city", "Koper"));
    restore(editor, c, 1);
    save(editor, g, c, set("city", "Bled"));

    assertThat(get(owner, path(g, c) + "/versions/1").body()).isEqualTo(v1);
    assertThat(delete(owner, path(g, c) + "/versions/2", null).status()).isEqualTo(405);
    assertThat(history(owner, g, c).size()).isEqualTo(4);
  }

  @Test
  @DisplayName("R-011-01 combination restoring brings back Fields, Attributes and Tags at once")
  void r011_01_combination() {
    sejem();
    String c = eva("vip");
    save(editor, g, c, set("city", "Koper").andThen(f -> {
      f.set("tags", arr());
      f.set("attributes", attrs());
    }));

    assertThat(restore(editor, c, 1).status()).isEqualTo(200);

    JsonNode now = read(owner, g, c);
    assertThat(now.path("fields").path("city").asString()).isEqualTo("Kranj");
    assertThat(texts(now.path("tags"), "name")).containsExactly("vip");
    assertThat(texts(now.path("attributes"), "value")).containsExactly("M");
    JsonNode v3 = history(owner, g, c).get(0);
    assertThat(v3.path("kind").asString()).isEqualTo("restored");
    assertThat(v3.path("restoredFrom").asInt()).isEqualTo(1);
    assertThat(texts(v3.path("changes"), "item"))
        .containsExactlyInAnyOrder("city", "tag:vip", "attribute:velikost");
  }

  @Test
  @DisplayName("R-011-02 other every Version stays readable after two restores")
  void r011_02_other() {
    sejem();
    String c = eva();
    save(editor, g, c, set("city", "Koper"));
    save(editor, g, c, set("city", "Bled"));
    restore(editor, c, 1);
    restore(editor, c, 2);

    for (int n = 1; n <= 5; n++) {
      assertThat(get(viewer, path(g, c) + "/versions/" + n).status()).as("Version %s", n).isEqualTo(200);
    }
    assertThat(read(owner, g, c).path("fields").path("city").asString()).isEqualTo("Koper");
  }

  @Test
  @DisplayName("R-011-03 other a Version equal to the current content after edits creates nothing")
  void r011_03_other() {
    sejem();
    String c = eva();
    save(editor, g, c, set("city", "Koper"));
    save(editor, g, c, set("city", "Kranj"));

    problem(restore(editor, c, 1), 409, "version.sameAsCurrent");
    assertThat(history(owner, g, c).size()).isEqualTo(3);
  }

  @Test
  @DisplayName("R-011-04 other a Tag removed from every Contact exists again after a restore")
  void r011_04_other() {
    sejem();
    String c = eva("sejem2026");
    save(editor, g, c, f -> f.set("tags", arr()));
    assertThat(get(owner, "/groups/" + g + "/tags?q=sejem2026").json().size()).isZero();

    restore(editor, c, 1);

    assertThat(texts(get(owner, "/groups/" + g + "/tags?q=sejem2026").json(), "name"))
        .containsExactly("sejem2026");
  }

  @Test
  @DisplayName("R-012-01 role the Owner moves a Contact: Version movedToRecycleBin at that time")
  void r012_01_role() {
    sejem();
    String c = eva();
    Instant now = clock.at("2026-09-09T09:09");

    assertThat(bin(owner, g, c).status()).isEqualTo(200);

    assertThat(kinds(history(owner, g, c)).get(0)).isEqualTo("2 movedToRecycleBin");
    JsonNode e = get(editor, "/groups/" + g + "/recycle-bin").json().get(0);
    assertThat(Instant.parse(e.path("deletedAt").asString())).isEqualTo(now);
    assertThat(e.path("deletedBy").asString()).isEqualTo("Luka Zorman");
  }

  @Test
  @DisplayName("R-012-02 combination the recycle bin is sorted newest first")
  void r012_02_combination() {
    sejem();
    String a = contact(owner, g, "firstName", "Ana");
    String b = contact(owner, g, "firstName", "Bor");
    String c = contact(owner, g, "firstName", "Cene");
    clock.at("2026-03-01T10:00");
    bin(editor, g, b);
    clock.at("2026-03-03T10:00");
    bin(editor, g, a);
    clock.at("2026-03-02T10:00");
    bin(editor, g, c);

    JsonNode entries = get(editor, "/groups/" + g + "/recycle-bin").json();
    List<String> order = new java.util.ArrayList<>();
    entries.forEach(e -> order.add(e.path("contact").path("name").asString()));
    assertThat(order).containsExactly("Ana", "Cene", "Bor");
  }

  @Test
  @DisplayName("R-012-03 boundary restored one second before the removal date with its History")
  void r012_03_boundary() {
    sejem();
    String c = eva("vip");
    Instant moved = clock.at("2026-03-01T10:00");
    bin(editor, g, c);
    clock.at(moved.plus(Duration.ofDays(30)).minusSeconds(1));

    assertThat(restoreFromBin(editor, c, 2).status()).isEqualTo(200);

    assertThat(kinds(history(owner, g, c))).containsExactly("3 restoredFromRecycleBin", "2 movedToRecycleBin", "1 created");
    assertThat(texts(read(owner, g, c).path("tags"), "name")).containsExactly("vip");
  }

  @Test
  @DisplayName("R-012-04 boundary a day after the removal date it cannot be restored")
  void r012_04_boundary() {
    sejem();
    String c = eva();
    Instant moved = clock.at("2026-03-01T10:00");
    bin(editor, g, c);
    clock.at(moved.plus(Duration.ofDays(31)));

    problem(restoreFromBin(owner, c, 2), 409, "contact.noLongerRestorable");
    assertThat(page(owner, g, "").path("total").asInt()).isZero();
  }

  @Test
  @DisplayName("R-012-05 other the removal job erases what is due and keeps what is not")
  void r012_05_other() {
    sejem();
    String old = contact(owner, g, "firstName", "Ana");
    String recent = contact(owner, g, "firstName", "Bor");
    clock.at("2026-03-01T10:00");
    bin(editor, g, old);
    clock.at("2026-03-05T10:00");
    bin(editor, g, recent);

    clock.at("2026-04-02T10:00");
    runScheduledJobs();

    JsonNode entries = get(editor, "/groups/" + g + "/recycle-bin").json();
    assertThat(entries.size()).isEqualTo(1);
    assertThat(entries.get(0).path("contact").path("name").asString()).isEqualTo("Bor");
  }

  @Test
  @DisplayName("R-013-01 other permanent delete removes the Contact's rows but not shared Tags")
  void r013_01_other() {
    sejem();
    String c = eva("vip");
    eva("vip");
    bin(editor, g, c);

    assertThat(delete(owner, "/groups/" + g + "/recycle-bin/" + c, null).status()).isEqualTo(204);

    assertThat(count("SELECT count(*) FROM contact_version WHERE contact_id = ?::uuid", c)).isZero();
    assertThat(count("SELECT count(*) FROM attribute WHERE contact_id = ?::uuid", c)).isZero();
    assertThat(count("SELECT count(*) FROM contact_tag WHERE contact_id = ?::uuid", c)).isZero();
    assertThat(texts(get(owner, "/groups/" + g + "/tags?q=vip").json(), "name")).containsExactly("vip");
  }

  @Test
  @DisplayName("R-013-02 other the Audit entry names the Contact id, the Owner and the time only")
  void r013_02_other() {
    sejem();
    String c = eva();
    bin(editor, g, c);
    List<Map<String, Object>> before = audit(g);
    Instant now = clock.at("2026-11-30T23:59");

    delete(owner, "/groups/" + g + "/recycle-bin/" + c, null);

    List<Map<String, Object>> added = audit(g);
    added.removeAll(before);
    assertThat(added).hasSize(1);
    assertThat(added.get(0).get("target_id")).isEqualTo(c);
    assertThat(added.get(0).get("user_id")).isEqualTo(id(owner));
    assertThat(((java.sql.Timestamp) added.get(0).get("at")).toInstant()).isEqualTo(now);
  }

  @Test
  @DisplayName("R-013-03 other a Contact restored from the bin is active and cannot be erased")
  void r013_03_other() {
    sejem();
    String c = eva();
    bin(editor, g, c);
    restoreFromBin(editor, c, 2);

    problem(delete(owner, "/groups/" + g + "/recycle-bin/" + c, null), 409, "contact.notInRecycleBin");
    assertThat(read(owner, g, c).path("state").asString()).isEqualTo("active");
  }

  @Test
  @DisplayName("R-013-04 other (D-21) the removal job also erases Contacts merged into a due one")
  void r013_04_other() {
    sejem();
    String a = contact(owner, g, "firstName", "Eva", "email", "eva@golob.si");
    String b = contact(owner, g, "firstName", "E.", "email", "eva@golob.si");
    assertThat(merge(editor, g, a, b, null).status()).isEqualTo(201);
    Instant moved = clock.at("2026-03-01T10:00");
    bin(editor, g, a);

    clock.at(moved.plus(Duration.ofDays(30)).plusSeconds(1));
    runScheduledJobs();

    assertThat(count("SELECT count(*) FROM contact WHERE id IN (?::uuid, ?::uuid)", a, b)).isZero();
  }

  @Test
  @DisplayName("BR-05 other each saved change makes exactly one Version")
  void br05_other() {
    sejem();
    String c = eva();
    save(editor, g, c, set("city", "Koper"));
    bin(editor, g, c);
    restoreFromBin(editor, c, 3);
    restore(editor, c, 1);

    assertThat(kinds(history(owner, g, c)))
        .containsExactly("5 restored", "4 restoredFromRecycleBin", "3 movedToRecycleBin", "2 edited", "1 created");
  }

  @Test
  @DisplayName("BR-06 other stale Version numbers on move and restore from the bin are rejected")
  void br06_other() {
    sejem();
    String c = eva();
    save(owner, g, c, set("city", "Koper"));

    Answer move = post(editor, path(g, c) + "/move-to-recycle-bin", obj("versionNumber", 1));
    problem(move, 409, "version.conflict");
    assertThat(move.json().path("params").path("changedBy").asString()).isEqualTo("Luka Zorman");
    bin(editor, g, c);
    problem(restoreFromBin(owner, c, 2), 409, "version.conflict");
  }

  @Test
  @DisplayName("BR-07 other time passing short of the period erases and merges nothing")
  void br07_other() {
    sejem();
    String a = contact(owner, g, "firstName", "Eva", "email", "eva@golob.si");
    contact(owner, g, "firstName", "E.", "email", "eva@golob.si");
    Instant moved = clock.at("2026-03-01T10:00");
    bin(editor, g, a);

    clock.at(moved.plus(Duration.ofDays(29)));
    runScheduledJobs();
    get(owner, "/groups/" + g + "/duplicates");

    assertThat(get(editor, "/groups/" + g + "/recycle-bin").json().size()).isEqualTo(1);
    assertThat(page(owner, g, "").path("total").asInt()).isEqualTo(1);
  }

  @Test
  @DisplayName("BR-08 combination a Group due for removal is erased with everything it owns")
  void br08_combination() {
    sejem();
    String a = contact(owner, g, body("firstName", "Eva", "email", "eva@golob.si"));
    String b = contact(owner, g, "firstName", "E.", "email", "eva@golob.si");
    merge(editor, g, a, b, null);
    save(owner, g, a, f -> f.set("tags", arr("vip")));
    Instant moved = clock.at("2026-03-01T10:00");
    post(owner, "/groups/" + g + "/move-to-recycle-bin", null);

    clock.at(moved.plus(Duration.ofDays(30)).plusSeconds(1));
    runScheduledJobs();

    for (String t : new String[] {"contact", "contact_version", "contact_merge", "audit_entry", "membership", "tag"}) {
      assertThat(count("SELECT count(*) FROM " + t + " WHERE group_id = ?::uuid", g)).as(t).isZero();
    }
    assertThat(get(owner, "/deleted-groups").json().size()).isZero();
  }
}
