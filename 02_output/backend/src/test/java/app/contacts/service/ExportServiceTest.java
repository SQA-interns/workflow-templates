package app.contacts.service;

import static app.contacts.domain.ContactFilters.NONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.contacts.domain.AuditEntry;
import app.contacts.domain.ContactExportAttributeRow;
import app.contacts.domain.ContactExportRow;
import app.contacts.domain.ContactFields;
import app.contacts.domain.ContactSort;
import app.contacts.domain.ContactTagRow;
import app.contacts.domain.Group;
import app.contacts.domain.Role;
import app.contacts.repository.ContactQueryRepository;
import app.contacts.repository.GroupRepository;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExportServiceTest {

  /** 00:30 on 05.03.2026 in Europe/Ljubljana, still 04.03.2026 in UTC (R-016-06). */
  private static final Instant NOW = Instant.parse("2026-03-04T23:30:00Z");

  private static final UUID GROUP_ID = UUID.randomUUID();
  private static final UUID PETRA = UUID.randomUUID();
  private static final String ALL = "%";
  private static final List<String> FIELDS_EN =
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
  private static final List<String> FIELDS_SL =
      List.of(
          "Ime",
          "Priimek",
          "E-pošta",
          "Telefon",
          "Podjetje",
          "Delovno mesto",
          "Ulica",
          "Poštna številka",
          "Kraj",
          "Država",
          "Opomba");

  private final ContactQueryRepository repository = mock(ContactQueryRepository.class);
  private final AccessGuard guard = mock(AccessGuard.class);
  private final GroupRepository groups = mock(GroupRepository.class);
  private final AuditService audit = mock(AuditService.class);
  private final ExportService service =
      new ExportService(
          new ContactQueryService(repository, guard, new Normaliser()),
          repository,
          guard,
          groups,
          audit,
          Clock.fixed(NOW, ZoneOffset.UTC));

  private final ContactExportRow janez = row("Janez", "Novak", null);
  private final ContactExportRow maja = row("Maja", "Zupan", null);

  @BeforeEach
  void petraIsViewerOfKonferenca() {
    when(guard.require(GROUP_ID, PETRA, Permission.EXPORT)).thenReturn(Role.VIEWER);
    group("Konferenca 2025");
  }

  private void group(String name) {
    when(groups.findById(GROUP_ID)).thenReturn(Optional.of(Group.create(name, PETRA, NOW)));
  }

  private static ContactExportRow row(String first, String last, String country) {
    return new ContactExportRow(
        UUID.randomUUID(),
        new ContactFields(first, last, null, null, null, null, null, null, null, country, null));
  }

  private void scope(List<UUID> contactIds, ContactExportRow... rows) {
    when(repository.countExport(GROUP_ID, ALL, NONE, contactIds)).thenReturn((long) rows.length);
    when(repository.findExport(GROUP_ID, ALL, NONE, contactIds, ContactSort.NAME, false))
        .thenReturn(List.of(rows));
  }

  private ExportFile export(String language, List<UUID> contactIds) {
    return service.export(GROUP_ID, PETRA, language, all(), contactIds);
  }

  private static ContactQuery all() {
    return new ContactQuery(null, null, null, null, ContactQuery.Filters.NONE);
  }

  private static List<List<String>> sheet(ExportFile file) throws IOException {
    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(file.content()))) {
      Sheet sheet = workbook.getSheetAt(0);
      List<List<String>> rows = new ArrayList<>();
      for (Row row : sheet) {
        List<String> cells = new ArrayList<>();
        row.forEach(cell -> cells.add(cell.getStringCellValue()));
        rows.add(cells);
      }
      return rows;
    }
  }

  private void assertConflict(List<UUID> contactIds, String code) {
    assertThatThrownBy(() -> export("en", contactIds))
        .isInstanceOfSatisfying(
            ConflictException.class, e -> assertThat(e.getCode()).isEqualTo(code));
    verify(repository, never()).findExport(any(), anyString(), any(), any(), any(), anyBoolean());
    verifyNoInteractions(audit);
  }

  @Test
  void ac01601R01603FieldsThenAttributeKeysAzThenTagsJoined() throws IOException {
    scope(null, janez, maja);
    when(repository.findExportAttributes(GROUP_ID, List.of(janez.id(), maja.id())))
        .thenReturn(
            List.of(
                new ContactExportAttributeRow(maja.id(), "jezik", "nemščina"),
                new ContactExportAttributeRow(janez.id(), "kompetenca", "spletnoProgramiranje")));
    UUID udel = UUID.randomUUID();
    when(repository.findTags(GROUP_ID, List.of(janez.id(), maja.id())))
        .thenReturn(
            List.of(
                new ContactTagRow(janez.id(), udel, "udelezenec2025"),
                new ContactTagRow(maja.id(), udel, "udelezenec2025"),
                new ContactTagRow(janez.id(), UUID.randomUUID(), "vip")));

    List<List<String>> sheet = sheet(export("en", null));

    List<String> header = new ArrayList<>(FIELDS_EN);
    header.addAll(List.of("jezik", "kompetenca", "Tags"));
    assertThat(sheet.get(0)).containsExactlyElementsOf(header);
    assertThat(sheet.get(1).subList(0, 2)).containsExactly("Janez", "Novak");
    assertThat(sheet.get(1).subList(11, 14))
        .containsExactly("", "spletnoProgramiranje", "udelezenec2025, vip");
    assertThat(sheet.get(2).subList(11, 14)).containsExactly("nemščina", "", "udelezenec2025");
    assertThat(sheet).hasSize(3);
  }

  @Test
  void r01606FileNameWithTheGroupNameAndTheDateInLjubljana() {
    scope(null, janez);

    assertThat(export("en", null).fileName()).isEqualTo("kontakti-Konferenca 2025-2026-03-05.xlsx");
  }

  @Test
  void ex01605CharactersNotAllowedInFileNamesBecomeDash() {
    group("A/B: test\\*?\"<>|\u0007x");
    scope(null, janez);

    assertThat(export("en", null).fileName())
        .isEqualTo("kontakti-A-B- test" + "-".repeat(8) + "x-2026-03-05.xlsx");
  }

  @Test
  void br10OneAuditEntryOfTheExportForTheGroup() {
    scope(null, janez);

    export("en", null);

    verify(audit).record(GROUP_ID, PETRA, AuditEntry.CONTACTS_EXPORTED, GROUP_ID);
  }

  @Test
  void r01602ExportCheckedAsksForTheCheckedContactsOnly() throws IOException {
    List<UUID> checked = List.of(maja.id());
    scope(checked, maja);

    List<List<String>> sheet = sheet(export("en", checked));

    assertThat(sheet).hasSize(2);
    assertThat(sheet.get(1).subList(0, 2)).containsExactly("Maja", "Zupan");
  }

  @Test
  void r01604HeaderInSlovenianAndCountryAsIsoCode() throws IOException {
    scope(null, row("Janez", "Novak", "SI"));

    List<List<String>> sheet = sheet(export("sl", null));

    List<String> header = new ArrayList<>(FIELDS_SL);
    header.add("Značke");
    assertThat(sheet.get(0)).containsExactlyElementsOf(header);
    assertThat(sheet.get(1).get(9)).isEqualTo("SI");
  }

  @Test
  void ex01604WithoutAttributesTagsFollowsTheFields() throws IOException {
    scope(null, janez);

    assertThat(sheet(export("en", null)).get(0)).hasSize(12).endsWith("Tags");
  }

  @Test
  void br11AttributeKeysDifferingInCaseShareOneColumn() throws IOException {
    scope(null, janez, maja);
    when(repository.findExportAttributes(GROUP_ID, List.of(janez.id(), maja.id())))
        .thenReturn(
            List.of(
                new ContactExportAttributeRow(janez.id(), "Jezik", "angleščina"),
                new ContactExportAttributeRow(maja.id(), "jezik", "nemščina")));

    List<List<String>> sheet = sheet(export("en", null));

    assertThat(sheet.get(0).subList(11, 13)).containsExactly("Jezik", "Tags");
    assertThat(sheet.get(1).get(11)).isEqualTo("angleščina");
    assertThat(sheet.get(2).get(11)).isEqualTo("nemščina");
  }

  @Test
  void r01605MoreThan10000ContactsGiveNoFile() {
    when(repository.countExport(GROUP_ID, ALL, NONE, null)).thenReturn(10_001L);

    assertConflict(null, "export.tooMany");
  }

  @Test
  void ex01601Exactly10000ContactsAreExported() {
    when(repository.countExport(GROUP_ID, ALL, NONE, null)).thenReturn(10_000L);
    when(repository.findExport(GROUP_ID, ALL, NONE, null, ContactSort.NAME, false))
        .thenReturn(List.of(janez));

    assertThat(export("en", null).content()).isNotEmpty();
  }

  @Test
  void r01607EmptyScopeGivesNoFile() {
    when(repository.countExport(GROUP_ID, ALL, NONE, null)).thenReturn(0L);

    assertConflict(null, "export.empty");
  }

  @Test
  void r01607NoCheckedContactIsAnEmptyScope() {
    assertConflict(List.of(), "export.empty");
    verify(repository, never()).countExport(any(), anyString(), any(), any());
  }

  @Test
  void contractAtMost10000CheckedContacts() {
    List<UUID> checked = Collections.nCopies(10_001, janez.id());

    assertThatThrownBy(() -> export("en", checked)).isInstanceOf(ValidationException.class);
    verifyNoInteractions(audit);
  }

  @Test
  void br02NonMemberGetsNotFoundAndNoAudit() {
    when(guard.require(GROUP_ID, PETRA, Permission.EXPORT)).thenThrow(new NotFoundException());

    assertThatThrownBy(() -> export("en", null)).isInstanceOf(NotFoundException.class);
    verifyNoInteractions(repository, audit);
  }

  @Test
  void r01601ExportUsesTheSortOfTheList() {
    when(repository.countExport(eq(GROUP_ID), eq(ALL), eq(NONE), eq(null))).thenReturn(1L);
    when(repository.findExport(GROUP_ID, ALL, NONE, null, ContactSort.COMPANY, true))
        .thenReturn(List.of(janez));

    ExportFile file =
        service.export(
            GROUP_ID,
            PETRA,
            "en",
            new ContactQuery(null, "company", "desc", null, ContactQuery.Filters.NONE),
            null);

    assertThat(file.content()).isNotEmpty();
  }
}
