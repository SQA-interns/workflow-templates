package app.contacts.service;

import app.contacts.domain.AuditEntry;
import app.contacts.domain.ContactExportAttributeRow;
import app.contacts.domain.ContactExportRow;
import app.contacts.domain.ContactTagRow;
import app.contacts.repository.ContactQueryRepository;
import app.contacts.repository.GroupRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SequencedMap;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Export of the Contact list, or of its Checked contacts, to {@code .xlsx} (US-016, ADR-09). */
@Service
public class ExportService {

  /** Most Contacts in one Export (R-016-05). */
  static final int MAX_CONTACTS = 10_000;

  /** The time zone of dates shown to a User (project-wide facts). */
  private static final ZoneId LJUBLJANA = ZoneId.of("Europe/Ljubljana");

  /** The Field columns in Data-model order, by language (R-016-03, R-016-04). */
  private static final Map<String, List<String>> FIELD_HEADERS =
      Map.of(
          "sl",
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
              "Opomba"),
          "en",
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
              "Note"));

  /** The last column, by language (R-016-03, R-016-04). */
  private static final Map<String, String> TAGS_HEADER = Map.of("sl", "Značke", "en", "Tags");

  /** Characters not allowed in file names, and control characters (R-016-06, D-24 (c)). */
  private static final String NOT_IN_FILE_NAMES = "[\\\\/:*?\"<>|\\p{Cntrl}]";

  private final ContactQueryService queries;
  private final ContactQueryRepository repository;
  private final AccessGuard guard;
  private final GroupRepository groups;
  private final AuditService audit;
  private final Clock clock;

  /**
   * The service.
   *
   * @param queries checks the list's search, sort and filters
   * @param repository the Contact list query
   * @param guard the access check
   * @param groups reads the Group name
   * @param audit writes the Audit entry
   * @param clock the clock
   */
  public ExportService(
      ContactQueryService queries,
      ContactQueryRepository repository,
      AccessGuard guard,
      GroupRepository groups,
      AuditService audit,
      Clock clock) {
    this.queries = queries;
    this.repository = repository;
    this.guard = guard;
    this.groups = groups;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * The Export scope as an {@code .xlsx} file, with one Audit entry (R-016-01 … R-016-06, BR-10).
   *
   * @param groupId the Group
   * @param userId the signed-in User
   * @param language the User's language, {@code sl} or {@code en}, for the header row
   * @param query the list's search, sort and filters as sent; its Page is ignored
   * @param contactIds the Checked contacts for "Export checked"; {@code null} for the whole list
   * @return the file
   * @throws NotFoundException for a non-Member (BR-02), or a Tag id of another Group
   * @throws ValidationException for an invalid query, or more than 10,000 Checked contacts
   * @throws ConflictException {@code export.tooMany} above 10,000 Contacts, {@code export.empty}
   *     for none (R-016-05, R-016-07)
   */
  @Transactional
  public ExportFile export(
      UUID groupId, UUID userId, String language, ContactQuery query, List<UUID> contactIds) {
    guard.require(groupId, userId, Permission.EXPORT);
    if (contactIds != null && contactIds.size() > MAX_CONTACTS) {
      throw new ValidationException(List.of());
    }
    ContactListScope scope = queries.scope(groupId, query);
    requireSize(groupId, scope, contactIds);
    List<ContactExportRow> rows =
        repository.findExport(
            groupId,
            scope.pattern(),
            scope.filters(),
            contactIds,
            scope.sort(),
            scope.descending());
    byte[] content = workbook(groupId, language, rows);
    audit.record(groupId, userId, AuditEntry.CONTACTS_EXPORTED, groupId);
    return new ExportFile(fileName(groupId), content);
  }

  /**
   * The scope has 1 to 10,000 Contacts.
   *
   * @throws ConflictException otherwise (R-016-05, R-016-07)
   */
  private void requireSize(UUID groupId, ContactListScope scope, List<UUID> contactIds) {
    long size =
        contactIds != null && contactIds.isEmpty()
            ? 0
            : repository.countExport(groupId, scope.pattern(), scope.filters(), contactIds);
    if (size > MAX_CONTACTS) {
      throw new ConflictException("export.tooMany", Map.of());
    }
    if (size == 0) {
      throw new ConflictException("export.empty", Map.of());
    }
  }

  /**
   * The Fields, then one column per Attribute key A–Z, then the Tags A–Z joined by ", " (R-016-03).
   */
  private byte[] workbook(UUID groupId, String language, List<ContactExportRow> rows) {
    List<UUID> ids = rows.stream().map(ContactExportRow::id).toList();
    List<ContactExportAttributeRow> attributeRows = repository.findExportAttributes(groupId, ids);
    SequencedMap<String, String> keys = keys(attributeRows);
    Map<UUID, Map<String, String>> attributes = byContact(attributeRows);
    Map<UUID, String> tags = tags(groupId, ids);
    List<String> header = new ArrayList<>(FIELD_HEADERS.get(language));
    header.addAll(keys.sequencedValues());
    header.add(TAGS_HEADER.get(language));
    ExportWorkbook workbook = new ExportWorkbook(header);
    for (ContactExportRow row : rows) {
      List<String> values = new ArrayList<>(row.fields().byName().sequencedValues());
      Map<String, String> own = attributes.getOrDefault(row.id(), Map.of());
      keys.sequencedKeySet().forEach(key -> values.add(own.get(key)));
      values.add(tags.get(row.id()));
      workbook.addRow(values);
    }
    return workbook.toBytes();
  }

  /**
   * Each Attribute key once, compared case-insensitively (BR-11), in the order of the rows, written
   * as first found.
   */
  private static SequencedMap<String, String> keys(List<ContactExportAttributeRow> rows) {
    SequencedMap<String, String> keys = new LinkedHashMap<>();
    rows.forEach(row -> keys.putIfAbsent(caseless(row.key()), row.key()));
    return keys;
  }

  /** Contact to caseless key to value. */
  private static Map<UUID, Map<String, String>> byContact(List<ContactExportAttributeRow> rows) {
    Map<UUID, Map<String, String>> byContact = new HashMap<>();
    rows.forEach(
        row ->
            byContact
                .computeIfAbsent(row.contactId(), id -> new HashMap<>())
                .put(caseless(row.key()), row.value()));
    return byContact;
  }

  private static String caseless(String key) {
    return key.toLowerCase(Locale.ROOT);
  }

  /** Contact to its Tag names A–Z joined by ", ". */
  private Map<UUID, String> tags(UUID groupId, List<UUID> ids) {
    return repository.findTags(groupId, ids).stream()
        .collect(
            Collectors.groupingBy(
                ContactTagRow::contactId,
                Collectors.mapping(ContactTagRow::name, Collectors.joining(", "))));
  }

  /** {@code kontakti-<Group name>-<yyyy-mm-dd>.xlsx}, today in Europe/Ljubljana (R-016-06). */
  private String fileName(UUID groupId) {
    String name = groups.findById(groupId).orElseThrow(NotFoundException::new).getName();
    LocalDate today = LocalDate.now(clock.withZone(LJUBLJANA));
    return "kontakti-" + name.replaceAll(NOT_IN_FILE_NAMES, "-") + "-" + today + ".xlsx";
  }
}
