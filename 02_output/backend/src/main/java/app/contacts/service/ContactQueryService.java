package app.contacts.service;

import app.contacts.domain.ContactFilters;
import app.contacts.domain.ContactListRow;
import app.contacts.domain.ContactSort;
import app.contacts.domain.ContactTagRow;
import app.contacts.repository.ContactQueryRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The Contact list of a Group: one Page, sorted, searched and filtered (US-014, US-015). */
@Service
public class ContactQueryService {

  /** Contacts per Page (US-014 "Page"). */
  static final int PAGE_SIZE = 50;

  /** Longest search text after trimming (contract parameter {@code q}). */
  static final int SEARCH_MAX = 200;

  /** Longest Attribute key filter (contract parameter {@code attributeKey}, R-008-02). */
  static final int ATTRIBUTE_KEY_MAX = 50;

  /** Longest Attribute value filter (contract parameter {@code attributeValue}, R-008-03). */
  static final int ATTRIBUTE_VALUE_MAX = 500;

  /** Longest company filter (contract parameter {@code company}, Data model). */
  static final int COMPANY_MAX = 200;

  /** ISO 3166-1 alpha-2 codes (R-006-05). */
  private static final Set<String> COUNTRIES = Set.of(Locale.getISOCountries());

  private static final Map<String, ContactSort> SORTS =
      Map.of(
          "name", ContactSort.NAME,
          "company", ContactSort.COMPANY,
          "email", ContactSort.EMAIL,
          "updatedAt", ContactSort.UPDATED_AT);

  private final ContactQueryRepository repository;
  private final AccessGuard guard;
  private final Normaliser normaliser;

  /**
   * The service.
   *
   * @param repository the Contact list query
   * @param guard the access check
   * @param normaliser Normalises the search text
   */
  public ContactQueryService(
      ContactQueryRepository repository, AccessGuard guard, Normaliser normaliser) {
    this.repository = repository;
    this.guard = guard;
    this.normaliser = normaliser;
  }

  /**
   * One Page of the active Contacts of the Group that pass the filters (R-014-01 … R-014-04,
   * R-015-01 … R-015-05, BR-09).
   *
   * @param groupId the Group
   * @param userId the signed-in User
   * @param query search, sort, order, Page and filters as sent; {@code null} for the defaults
   * @return the Page with the total of the filtered list (R-015-07)
   * @throws NotFoundException for a non-Member (BR-02), or a Tag id of another Group (EX-015-04)
   * @throws ValidationException for an unknown sort or order, a Page below 1 or a search text of
   *     more than 200 characters, or an invalid filter
   */
  @Transactional(readOnly = true)
  public ContactPageView page(UUID groupId, UUID userId, ContactQuery query) {
    guard.require(groupId, userId, Permission.READ);
    ContactSort sort = SORTS.get(query.sort() == null ? "name" : query.sort());
    String dir = query.dir() == null ? "asc" : query.dir();
    int page = query.page() == null ? 1 : query.page();
    if (sort == null || !(dir.equals("asc") || dir.equals("desc")) || page < 1) {
      throw new ValidationException(List.of());
    }
    String pattern = pattern(query.search());
    ContactFilters filters = checked(query.filters());
    requireTagsOf(groupId, filters);
    List<ContactListRow> rows =
        repository.findPage(
            groupId, pattern, filters, sort, dir.equals("desc"), (page - 1) * PAGE_SIZE, PAGE_SIZE);
    long total = repository.count(groupId, pattern, filters);
    return new ContactPageView(withTags(groupId, rows), total, page, PAGE_SIZE);
  }

  /**
   * The LIKE pattern of the trimmed, Normalised search text (R-014-04).
   *
   * @throws ValidationException for a search text of more than 200 characters
   */
  private String pattern(String sent) {
    String search = sent == null ? "" : sent.strip();
    if (search.codePointCount(0, search.length()) > SEARCH_MAX) {
      throw new ValidationException(
          List.of(new FieldProblem("q", "field.tooLong", Map.of("field", "q", "max", SEARCH_MAX))));
    }
    return search.isEmpty() ? "%" : "%" + escaped(normaliser.text(search)) + "%";
  }

  /**
   * Every Tag of the filters, each once, belongs to the Group (EX-015-02, EX-015-04).
   *
   * @throws NotFoundException for a Tag id of another Group or of no Tag
   */
  private void requireTagsOf(UUID groupId, ContactFilters filters) {
    Set<UUID> ids = new LinkedHashSet<>(filters.hasTagIds());
    ids.addAll(filters.withoutTagIds());
    if (repository.countTags(groupId, List.copyOf(ids)) != ids.size()) {
      throw new NotFoundException();
    }
  }

  /**
   * The filters trimmed, blank texts unset (R-015-03, R-015-04).
   *
   * @throws ValidationException for an Attribute value without key, a text longer than the contract
   *     allows or a country that is not an ISO 3166-1 alpha-2 code
   */
  private static ContactFilters checked(ContactFilters sent) {
    ContactFilters filters =
        new ContactFilters(
            sent.hasTagIds(),
            sent.withoutTagIds(),
            trimmed(sent.attributeKey()),
            trimmed(sent.attributeValue()),
            trimmed(sent.company()),
            trimmed(sent.country()));
    List<FieldProblem> problems = problems(filters);
    if (!problems.isEmpty()) {
      throw new ValidationException(problems);
    }
    return filters;
  }

  private static List<FieldProblem> problems(ContactFilters filters) {
    String key = filters.attributeKey();
    List<FieldProblem> problems = new ArrayList<>();
    if ((key == null && filters.attributeValue() != null) || length(key) > ATTRIBUTE_KEY_MAX) {
      problems.add(new FieldProblem("attributeKey", "attribute.keyLength", Map.of()));
    }
    if (length(filters.attributeValue()) > ATTRIBUTE_VALUE_MAX) {
      problems.add(new FieldProblem("attributeValue", "attribute.valueTooLong", Map.of()));
    }
    if (length(filters.company()) > COMPANY_MAX) {
      problems.add(
          new FieldProblem(
              "company", "field.tooLong", Map.of("field", "company", "max", COMPANY_MAX)));
    }
    if (filters.country() != null && !COUNTRIES.contains(filters.country())) {
      problems.add(new FieldProblem("country", "field.countryInvalid", Map.of()));
    }
    return problems;
  }

  /** The text trimmed, or {@code null} when blank. */
  private static String trimmed(String text) {
    return text == null || text.isBlank() ? null : text.strip();
  }

  private static int length(String text) {
    return text == null ? 0 : text.codePointCount(0, text.length());
  }

  /** The search text taken literally in a LIKE pattern (EX-014-05). */
  private static String escaped(String text) {
    return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }

  private List<ContactListItemView> withTags(UUID groupId, List<ContactListRow> rows) {
    Map<UUID, List<TagView>> tags = new HashMap<>();
    List<UUID> ids = rows.stream().map(ContactListRow::id).toList();
    for (ContactTagRow tag : repository.findTags(groupId, ids)) {
      tags.computeIfAbsent(tag.contactId(), id -> new ArrayList<>())
          .add(new TagView(tag.tagId(), tag.name()));
    }
    return rows.stream()
        .map(
            row ->
                new ContactListItemView(
                    row.id(),
                    row.firstName(),
                    row.lastName(),
                    row.company(),
                    row.email(),
                    row.phone(),
                    tags.getOrDefault(row.id(), List.of()),
                    row.updatedAt()))
        .toList();
  }
}
