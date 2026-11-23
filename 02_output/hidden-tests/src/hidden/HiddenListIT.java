package app.contacts.hidden;

import static org.assertj.core.api.Assertions.assertThat;

import app.contacts.hidden.support.HiddenTest;
import java.io.ByteArrayInputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** Hidden cases of US-014, US-015, US-016 and BR-09. */
class HiddenListIT extends HiddenTest {

  private Person owner;
  private Person viewer;
  private String g;

  private void sejem() {
    owner = person("luka.zorman@example.org", "Luka Zorman");
    viewer = person("rok.mlakar@example.org", "Rok Mlakar");
    g = group(owner, "Sejem Celje");
    join(g, owner, viewer, "VIEWER");
  }

  private String add(String first, String last, String company, String... tags) {
    ObjectNode b = body("firstName", first, "lastName", last, "company", company);
    b.set("tags", arr(tags));
    return contact(owner, g, b);
  }

  private static List<List<String>> rows(byte[] xlsx) {
    try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
      DataFormatter f = new DataFormatter();
      List<List<String>> rows = new ArrayList<>();
      int width = wb.getSheetAt(0).getRow(0).getLastCellNum();
      for (Row r : wb.getSheetAt(0)) {
        List<String> cells = new ArrayList<>();
        for (int i = 0; i < width; i++) {
          cells.add(r.getCell(i) == null ? "" : f.formatCellValue(r.getCell(i)));
        }
        rows.add(cells);
      }
      return rows;
    } catch (java.io.IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static String fileName(Answer a) {
    String d = a.headers().firstValue("Content-Disposition").orElse("");
    int star = d.indexOf("filename*=UTF-8''");
    if (star >= 0) {
      String v = d.substring(star + 17).split(";")[0];
      return URLDecoder.decode(v.replace("+", "%2B"), StandardCharsets.UTF_8);
    }
    int plain = d.indexOf("filename=");
    return d.substring(plain + 9).split(";")[0].replace("\"", "");
  }

  private void bulk(int count) {
    String o = id(owner);
    db.update(
        "INSERT INTO contact (id, group_id, first_name, last_name, state, version_number,"
            + " search_text, sort_name, created_at, created_by, updated_at, updated_by)"
            + " SELECT gen_random_uuid(), ?::uuid, 'Clan', 'Seznam' || lpad(n::text, 5, '0'),"
            + " 'active', 1, 'clan seznam' || lpad(n::text, 5, '0'),"
            + " 'Seznam' || lpad(n::text, 5, '0'), now(), ?::uuid, now(), ?::uuid"
            + " FROM generate_series(1, ?) AS n",
        g, o, o, count);
  }

  @Test
  @DisplayName("R-014-01 boundary 100 Contacts are exactly two Pages")
  void r014_01_boundary() {
    sejem();
    for (int i = 1; i <= 100; i++) {
      contact(owner, g, "firstName", "Clan", "lastName", String.format("Seznam%03d", i));
    }

    assertThat(page(viewer, g, "page=2").path("items").size()).isEqualTo(50);
    assertThat(page(viewer, g, "page=3").path("items").size()).isZero();
    assertThat(page(viewer, g, "page=3").path("total").asInt()).isEqualTo(100);
  }

  @Test
  @DisplayName("R-014-02 other same last name: first name next, Slovenian letters in place")
  void r014_02_other() {
    sejem();
    add("Žan", "Kos", null);
    add("Zala", "Kos", null);
    add("Ana", "Kos", null);
    add("Čarli", "Kos", null);
    add(null, null, "Kos d.o.o.");

    assertThat(names(page(viewer, g, "")))
        .containsExactly("Ana Kos", "Čarli Kos", "Zala Kos", "Žan Kos", "Kos d.o.o.");
  }

  @Test
  @DisplayName("R-014-03 other sort by company descending and by last change")
  void r014_03_other() {
    sejem();
    clock.at(Instant.parse("2026-01-01T10:00:00Z"));
    add("Eva", "Golob", "Beta");
    clock.at(Instant.parse("2026-01-03T10:00:00Z"));
    add("Tim", "Hribar", "Alfa");
    clock.at(Instant.parse("2026-01-02T10:00:00Z"));
    add("Rok", "Mlakar", "Gama");

    assertThat(names(page(viewer, g, "sort=company&dir=desc")))
        .containsExactly("Rok Mlakar", "Eva Golob", "Tim Hribar");
    assertThat(names(page(viewer, g, "sort=updatedAt&dir=desc")))
        .containsExactly("Tim Hribar", "Rok Mlakar", "Eva Golob");
    assertThat(names(page(viewer, g, "sort=name&dir=desc")))
        .containsExactly("Rok Mlakar", "Tim Hribar", "Eva Golob");
  }

  @Test
  @DisplayName("R-014-04 other search covers city, phone and e-mail, ignores diacritics")
  void r014_04_other() {
    sejem();
    contact(owner, g, "firstName", "Eva", "city", "Škofja Loka");
    contact(owner, g, "firstName", "Tim", "phone", "041 555 777");
    contact(owner, g, "firstName", "Rok", "email", "rok@mlakar.si");
    contact(owner, g, "firstName", "Žiga", "lastName", "Šuštar");

    assertThat(names(page(viewer, g, "q=skofja"))).containsExactly("Eva");
    assertThat(names(page(viewer, g, "q=555%20777"))).containsExactly("Tim");
    assertThat(names(page(viewer, g, "q=MLAKAR.SI"))).containsExactly("Rok");
    assertThat(names(page(viewer, g, "q=%C5%A0U%C5%A0TAR"))).containsExactly("Žiga Šuštar");
  }

  @Test
  @DisplayName("R-014-05 other an unmatched search gives no rows and total 0")
  void r014_05_other() {
    sejem();
    add("Eva", "Golob", null);

    JsonNode p = page(viewer, g, "q=qqq");
    assertThat(p.path("total").asInt()).isZero();
    assertThat(p.path("items").size()).isZero();
  }

  @Test
  @DisplayName("R-014-06 other the details hold Attributes and Tags")
  void r014_06_other() {
    sejem();
    ObjectNode b = body("firstName", "Eva");
    b.set("attributes", attrs("regija", "Gorenjska"));
    b.set("tags", arr("vip"));
    String c = contact(owner, g, b);

    JsonNode d = read(viewer, g, c);
    assertThat(texts(d.path("attributes"), "value")).containsExactly("Gorenjska");
    assertThat(texts(d.path("tags"), "name")).containsExactly("vip");
  }

  @Test
  @DisplayName("R-015-01 boundary has Tags with three Tags excludes a Contact with two of them")
  void r015_01_boundary() {
    sejem();
    add("Eva", "Golob", null, "a", "b", "c");
    add("Tim", "Hribar", null, "a", "b");

    String q = "hasTagIds=" + tag(owner, g, "a") + "&hasTagIds=" + tag(owner, g, "b") + "&hasTagIds=" + tag(owner, g, "c");
    assertThat(names(page(viewer, g, q))).containsExactly("Eva Golob");
  }

  @Test
  @DisplayName("R-015-02 combination without two Tags excludes Contacts with either")
  void r015_02_combination() {
    sejem();
    add("Eva", "Golob", null, "a");
    add("Tim", "Hribar", null, "b");
    add("Rok", "Mlakar", null, "c");

    String q = "withoutTagIds=" + tag(owner, g, "a") + "&withoutTagIds=" + tag(owner, g, "b");
    assertThat(names(page(viewer, g, q))).containsExactly("Rok Mlakar");
  }

  @Test
  @DisplayName("R-015-03 other value equal ignoring case only; a key alone needs the key")
  void r015_03_other() {
    sejem();
    ObjectNode a = body("firstName", "Eva");
    a.set("attributes", attrs("regija", "Gorenjska"));
    ObjectNode b = body("firstName", "Tim");
    b.set("attributes", attrs("regija", "Gorenjska regija"));
    contact(owner, g, a);
    contact(owner, g, b);
    contact(owner, g, "firstName", "Rok");

    assertThat(names(page(viewer, g, "attributeKey=regija&attributeValue=GORENJSKA"))).containsExactly("Eva");
    assertThat(names(page(viewer, g, "attributeKey=regija"))).containsExactly("Eva", "Tim");
  }

  @Test
  @DisplayName("R-015-04 other company equals ignoring case, not contains; country by code")
  void r015_04_other() {
    sejem();
    add("Eva", "Golob", "Podjetje d.o.o.");
    add("Tim", "Hribar", "Podjetje d.o.o. Maribor");
    contact(owner, g, "firstName", "Rok", "country", "AT");

    assertThat(names(page(viewer, g, "company=PODJETJE%20D.O.O."))).containsExactly("Eva Golob");
    assertThat(names(page(viewer, g, "country=AT"))).containsExactly("Rok");
  }

  @Test
  @DisplayName("R-015-05 combination Tag, Attribute, company and search together")
  void r015_05_combination() {
    sejem();
    ObjectNode a = body("firstName", "Eva", "lastName", "Golob", "company", "Alfa");
    a.set("tags", arr("vip"));
    a.set("attributes", attrs("regija", "Gorenjska"));
    ObjectNode b = body("firstName", "Ema", "lastName", "Golob", "company", "Alfa");
    b.set("tags", arr("vip"));
    contact(owner, g, a);
    contact(owner, g, b);

    String q = "hasTagIds=" + tag(owner, g, "vip") + "&attributeKey=regija&company=alfa&q=golob";
    assertThat(names(page(viewer, g, q))).containsExactly("Eva Golob");
  }

  @Test
  @DisplayName("R-015-06 other without a filter parameter the list is whole again")
  void r015_06_other() {
    sejem();
    add("Eva", "Golob", null, "vip");
    add("Tim", "Hribar", null);
    String vip = tag(owner, g, "vip");

    assertThat(page(viewer, g, "hasTagIds=" + vip + "&q=golob").path("total").asInt()).isEqualTo(1);
    assertThat(page(viewer, g, "q=golob").path("total").asInt()).isEqualTo(1);
    assertThat(page(viewer, g, "").path("total").asInt()).isEqualTo(2);
  }

  @Test
  @DisplayName("R-015-07 other a filtered list keeps the chosen sort and counts the filtered rows")
  void r015_07_other() {
    sejem();
    add("Eva", "Golob", null, "vip");
    add("Tim", "Hribar", null, "vip");
    add("Rok", "Mlakar", null);

    JsonNode p = page(viewer, g, "hasTagIds=" + tag(owner, g, "vip") + "&sort=name&dir=desc");
    assertThat(p.path("total").asInt()).isEqualTo(2);
    assertThat(names(p)).containsExactly("Tim Hribar", "Eva Golob");
  }

  @Test
  @DisplayName("R-016-01 other the Export follows the chosen sort order")
  void r016_01_other() {
    sejem();
    add("Eva", "Golob", null);
    add("Tim", "Hribar", null);

    Answer a = post(viewer, "/groups/" + g + "/exports", obj("sort", "name", "dir", "desc"));

    List<List<String>> r = rows(a.bytes());
    assertThat(r.get(1).get(1)).isEqualTo("Hribar");
    assertThat(r.get(2).get(1)).isEqualTo("Golob");
  }

  @Test
  @DisplayName("R-016-02 combination checked Contacts outside the filter are not exported")
  void r016_02_combination() {
    sejem();
    String eva = add("Eva", "Golob", null, "vip");
    String tim = add("Tim", "Hribar", null);

    Answer a =
        post(viewer, "/groups/" + g + "/exports", obj("hasTagIds", arr(tag(owner, g, "vip")), "contactIds", arr(eva, tim)));

    List<List<String>> r = rows(a.bytes());
    assertThat(r).hasSize(2);
    assertThat(r.get(1).get(0)).isEqualTo("Eva");
  }

  @Test
  @DisplayName("R-016-03 other Attribute columns A–Z across Contacts, Tags A–Z joined by comma")
  void r016_03_other() {
    sejem();
    patch(viewer, "/me", obj("language", "en"));
    ObjectNode a = body("firstName", "Eva");
    a.set("attributes", attrs("zeta", "1"));
    a.set("tags", arr("c", "a", "b"));
    ObjectNode b = body("firstName", "Tim");
    b.set("attributes", attrs("alfa", "2"));
    contact(owner, g, a);
    contact(owner, g, b);

    List<List<String>> r = rows(post(viewer, "/groups/" + g + "/exports", obj()).bytes());

    assertThat(r.get(0).subList(11, r.get(0).size())).containsExactly("alfa", "zeta", "Tags");
    assertThat(r.get(1).get(13)).isEqualTo("a, b, c");
  }

  @Test
  @DisplayName("R-016-04 other a Slovenian header and the country as its code")
  void r016_04_other() {
    sejem();
    contact(owner, g, "firstName", "Eva", "country", "HR");

    List<List<String>> r = rows(post(viewer, "/groups/" + g + "/exports", obj()).bytes());

    assertThat(r.get(0))
        .containsExactly("Ime", "Priimek", "E-pošta", "Telefon", "Podjetje", "Delovno mesto", "Ulica",
            "Poštna številka", "Kraj", "Država", "Opomba", "Značke");
    assertThat(r.get(1).get(9)).isEqualTo("HR");
  }

  @Test
  @DisplayName("R-016-05 combination 10,001 in the Group but 10,000 in the filtered scope export")
  void r016_05_combination() {
    sejem();
    bulk(10_000);
    add("Eva", "Golob", null);

    Answer narrowed = post(viewer, "/groups/" + g + "/exports", obj("q", "seznam"));
    Answer whole = post(viewer, "/groups/" + g + "/exports", obj());

    assertThat(narrowed.status()).isEqualTo(200);
    assertThat(rows(narrowed.bytes())).hasSize(10_001);
    problem(whole, 409, "export.tooMany");
  }

  @Test
  @DisplayName("R-016-06 boundary file name date in Ljubljana after midnight UTC, characters replaced")
  void r016_06_boundary() {
    owner = person("luka.zorman@example.org", "Luka Zorman");
    g = group(owner, "Ura*Čas?");
    contact(owner, g, "firstName", "Eva");
    clock.at(Instant.parse("2026-12-31T23:30:00Z"));

    Answer a = post(owner, "/groups/" + g + "/exports", obj());

    assertThat(fileName(a)).isEqualTo("kontakti-Ura-Čas--2027-01-01.xlsx");
  }

  @Test
  @DisplayName("R-016-07 other a filter that matches nobody gives no file")
  void r016_07_other() {
    sejem();
    add("Eva", "Golob", null);

    problem(post(viewer, "/groups/" + g + "/exports", obj("company", "Nobena")), 409, "export.empty");
  }

  @Test
  @DisplayName("BR-09 other a merged Contact is not in list, search, Export or duplicates")
  void br09_other() {
    sejem();
    String a = contact(owner, g, "firstName", "Eva", "lastName", "Golob", "email", "eva@golob.si");
    String b = contact(owner, g, "firstName", "Ema", "lastName", "Golobič", "email", "eva@golob.si");
    contact(owner, g, "firstName", "Tim", "email", "eva@golob.si");
    merge(owner, g, a, b, null);

    assertThat(names(page(viewer, g, ""))).doesNotContain("Ema Golobič");
    assertThat(page(viewer, g, "q=golobic").path("total").asInt()).isZero();
    assertThat(rows(post(viewer, "/groups/" + g + "/exports", obj()).bytes()).stream().map(r -> r.get(0)))
        .doesNotContain("Ema");
    assertThat(get(viewer, "/groups/" + g + "/duplicates").body()).doesNotContain(b);
  }
}
