package app.contacts.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import app.contacts.acceptance.support.AcceptanceTest;
import app.contacts.acceptance.support.Json;
import app.contacts.acceptance.support.Res;
import app.contacts.acceptance.support.TestUser;
import app.contacts.acceptance.support.Xlsx;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.CellType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;

/** US-016 Export to Excel: API part (the buttons are in {@code frontend/acceptance}). */
class Us016ExportAcceptanceIT extends AcceptanceTest {

  static final List<String> FIELDS_EN =
      List.of(
          "First name",
          "Last name",
          "E-mail",
          "Phone",
          "Company",
          "Job title",
          "Street",
          "Postal code",
          "City",
          "Country",
          "Note");

  private void english(TestUser user) {
    assertThat(api.patch(user, "/me", Json.obj("language", "en")).status()).isEqualTo(200);
  }

  private String person(Team t, ObjectNode body, String[] attributes, String... tags) {
    body.set("attributes", Json.attributes(attributes));
    body.set("tags", Json.strings(tags));
    return contact(t.ana(), t.groupId(), body);
  }

  private Res export(TestUser user, Team t, ObjectNode request) {
    return api.post(user, t.path() + "/exports", request);
  }

  /** Inserts Contacts through the persistence contract, as {@code seed/seed.sql} does (D-38). */
  private void bulk(Team t, int count) {
    String owner = idOf(t.ana());
    db.update(
        "INSERT INTO contact (id, group_id, first_name, last_name, state, version_number,"
            + " search_text, sort_name, created_at, created_by, updated_at, updated_by)"
            + " SELECT gen_random_uuid(), ?::uuid, 'Oseba', 'Priimek' || lpad(n::text, 5, '0'),"
            + " 'active', 1, 'oseba priimek' || lpad(n::text, 5, '0'),"
            + " 'Priimek' || lpad(n::text, 5, '0'), now(), ?::uuid, now(), ?::uuid"
            + " FROM generate_series(1, ?) AS n",
        t.groupId(),
        owner,
        owner,
        count);
  }

  @Test
  @DisplayName("AC-016-01 Export of the filtered list: file name, columns, Tags cell, audited")
  void ac01601() {
    Team t = team();
    english(t.petra());
    final Instant now = clock.setLjubljana("2026-03-05T12:00");
    person(
        t,
        contactBody("firstName", "Janez", "lastName", "Novak"),
        new String[] {"kompetenca", "spletnoProgramiranje"},
        "udelezenec2025",
        "vip");
    person(
        t,
        contactBody("firstName", "Maja", "lastName", "Zupan"),
        new String[] {"jezik", "nemščina"},
        "udelezenec2025");
    person(t, contactBody("firstName", "Žiga", "lastName", "Čuk"), new String[] {});
    String udel = tagId(t.petra(), t.groupId(), "udelezenec2025");
    final List<Map<String, Object>> before = auditEntries(t.groupId());

    Res res = export(t.petra(), t, Json.obj("hasTagIds", Json.strings(udel)));

    assertThat(res.status()).isEqualTo(200);
    assertThat(res.fileName()).isEqualTo("kontakti-Konferenca 2025-2026-03-05.xlsx");
    Xlsx file = Xlsx.read(res.bytes());
    assertThat(file.sheetCount()).isEqualTo(1);
    assertThat(file.header().subList(0, 11)).containsExactlyElementsOf(FIELDS_EN);
    assertThat(file.header().subList(11, file.header().size()))
        .containsExactly("jezik", "kompetenca", "Tags");
    assertThat(file.column("First name")).containsExactly("Janez", "Maja");
    assertThat(file.column("Tags")).containsExactly("udelezenec2025, vip", "udelezenec2025");
    assertThat(file.column("kompetenca")).containsExactly("spletnoProgramiranje", "");
    assertThat(file.column("jezik")).containsExactly("", "nemščina");
    assertAudited(t.groupId(), t.petra(), now, before);
  }

  @Test
  @DisplayName("AC-016-02 Export checked exports only the Checked contacts")
  void ac01602() {
    Team t = team();
    contact(t.ana(), t.groupId(), "firstName", "Janez", "lastName", "Novak");
    String maja = contact(t.ana(), t.groupId(), "firstName", "Maja", "lastName", "Zupan");
    contact(t.ana(), t.groupId(), "firstName", "Žiga", "lastName", "Čuk");

    Res res = export(t.petra(), t, Json.obj("contactIds", Json.strings(maja)));

    assertThat(res.status()).isEqualTo(200);
    Xlsx file = Xlsx.read(res.bytes());
    assertThat(file.rows()).hasSize(1);
    assertThat(file.rows().get(0).subList(0, 2)).containsExactly("Maja", "Zupan");
  }

  @Test
  @DisplayName("AC-016-03 more than 10,000 Contacts in scope: no file")
  void ac01603() {
    Team t = team();
    bulk(t, 10_001);

    Res res = export(t.petra(), t, Json.obj());

    assertProblem(res, 409, "export.tooMany");
    assertThat(res.headers().firstValue("Content-Type").orElse("")).doesNotContain("spreadsheetml");
  }

  @Test
  @DisplayName("AC-016-04 the header row is in the User's language (English)")
  void ac01604() {
    Team t = team();
    english(t.petra());
    contact(t.ana(), t.groupId(), "firstName", "Janez", "lastName", "Novak", "country", "SI");

    Xlsx file = Xlsx.read(export(t.petra(), t, Json.obj()).bytes());

    assertThat(file.header().subList(0, 11)).containsExactlyElementsOf(FIELDS_EN);
    assertThat(file.column("Country")).containsExactly("SI");
  }

  @Test
  @DisplayName("EX-016-01 exactly 10,000 Contacts give a file with 10,000 rows")
  void ex01601() {
    Team t = team();
    bulk(t, 10_000);

    Res res = export(t.petra(), t, Json.obj());

    assertThat(res.status()).isEqualTo(200);
    assertThat(Xlsx.read(res.bytes()).rows()).hasSize(10_000);
  }

  @Test
  @DisplayName("EX-016-02 an empty Export scope gives no file")
  void ex01602() {
    Team t = team();

    Res res = export(t.petra(), t, Json.obj());

    assertProblem(res, 409, "export.empty");
  }

  @Test
  @DisplayName("EX-016-03 values starting with = + - @ are written as text (SR-07)")
  void ex01603() {
    Team t = team();
    english(t.petra());
    contact(
        t.ana(),
        t.groupId(),
        "firstName",
        "Janez",
        "note",
        "=1+1",
        "jobTitle",
        "+1+1",
        "street",
        "-1+1",
        "city",
        "@SUM(1)");

    Xlsx file = Xlsx.read(export(t.petra(), t, Json.obj()).bytes());

    assertThat(file.column("Note")).containsExactly("=1+1");
    assertThat(file.column("Job title")).containsExactly("+1+1");
    assertThat(file.column("Street")).containsExactly("-1+1");
    assertThat(file.column("City")).containsExactly("@SUM(1)");
    List<CellType> types = file.types().get(0);
    for (String column : List.of("Note", "Job title", "Street", "City")) {
      assertThat(types.get(file.header().indexOf(column))).as(column).isEqualTo(CellType.STRING);
    }
  }

  @Test
  @DisplayName("EX-016-04 without Attributes the Tags column follows the Fields")
  void ex01604() {
    Team t = team();
    english(t.petra());
    contact(t.ana(), t.groupId(), "firstName", "Janez", "lastName", "Novak");

    Xlsx file = Xlsx.read(export(t.petra(), t, Json.obj()).bytes());

    List<String> expected = new ArrayList<>(FIELDS_EN);
    expected.add("Tags");
    assertThat(file.header()).containsExactlyElementsOf(expected);
  }

  @Test
  @DisplayName("EX-016-05 characters not allowed in file names become -")
  void ex01605() {
    TestUser ana = ana();
    String groupId = group(ana, "A/B: test");
    contact(ana, groupId, "firstName", "Janez");
    clock.setLjubljana("2026-03-05T12:00");

    Res res = api.post(ana, "/groups/" + groupId + "/exports", Json.obj());

    assertThat(res.fileName()).isEqualTo("kontakti-A-B- test-2026-03-05.xlsx");
  }

  @Test
  @DisplayName("EX-016-06 names with diacritics are written correctly")
  void ex01606() {
    Team t = team();
    contact(t.ana(), t.groupId(), "firstName", "Žiga", "lastName", "Čuk");

    Xlsx file = Xlsx.read(export(t.petra(), t, Json.obj()).bytes());

    assertThat(file.rows().get(0).subList(0, 2)).containsExactly("Žiga", "Čuk");
  }

  @Test
  @DisplayName("EX-016-07 a checked Contact moved to the recycle bin meanwhile is not exported")
  void ex01607() {
    Team t = team();
    String janez = contact(t.ana(), t.groupId(), "firstName", "Janez", "lastName", "Novak");
    String maja = contact(t.ana(), t.groupId(), "firstName", "Maja", "lastName", "Zupan");
    moveToRecycleBin(t.marko(), t.groupId(), janez);

    Res res = export(t.petra(), t, Json.obj("contactIds", Json.strings(janez, maja)));

    Xlsx file = Xlsx.read(res.bytes());
    assertThat(file.rows()).hasSize(1);
    assertThat(file.rows().get(0).subList(0, 2)).containsExactly("Maja", "Zupan");
  }
}
