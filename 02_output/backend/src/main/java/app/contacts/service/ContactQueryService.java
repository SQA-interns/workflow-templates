package app.contacts.service;

import app.contacts.domain.ContactListRow;
import app.contacts.domain.ContactSort;
import app.contacts.domain.ContactTagRow;
import app.contacts.repository.ContactQueryRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The Contact list of a Group: one Page, sorted and searched (US-014). */
@Service
public class ContactQueryService {

  /** Contacts per Page (US-014 "Page"). */
  static final int PAGE_SIZE = 50;

  /** Longest search text after trimming (contract parameter {@code q}). */
  static final int SEARCH_MAX = 200;

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
   * One Page of the active Contacts of the Group (R-014-01 … R-014-04, BR-09).
   *
   * @param groupId the Group
   * @param userId the signed-in User
   * @param query search, sort, order and Page as sent; {@code null} for the defaults
   * @return the Page with the total
   * @throws NotFoundException for a non-Member (BR-02)
   * @throws ValidationException for an unknown sort or order, a Page below 1 or a search text of
   *     more than 200 characters
   */
  @Transactional(readOnly = true)
  public ContactPageView page(UUID groupId, UUID userId, ContactQuery query) {
    guard.require(groupId, userId, Permission.READ);
    ContactSort sort = SORTS.get(query.sort() == null ? "name" : query.sort());
    String dir = query.dir() == null ? "asc" : query.dir();
    int page = query.page() == null ? 1 : query.page();
    String search = query.search() == null ? "" : query.search().strip();
    if (sort == null || !(dir.equals("asc") || dir.equals("desc")) || page < 1) {
      throw new ValidationException(List.of());
    }
    if (search.codePointCount(0, search.length()) > SEARCH_MAX) {
      throw new ValidationException(
          List.of(new FieldProblem("q", "field.tooLong", Map.of("field", "q", "max", SEARCH_MAX))));
    }
    String pattern = search.isEmpty() ? "%" : "%" + escaped(normaliser.text(search)) + "%";
    List<ContactListRow> rows =
        repository.findPage(
            groupId, pattern, sort, dir.equals("desc"), (page - 1) * PAGE_SIZE, PAGE_SIZE);
    long total = repository.count(groupId, pattern);
    return new ContactPageView(withTags(groupId, rows), total, page, PAGE_SIZE);
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
