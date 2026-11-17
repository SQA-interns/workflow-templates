package app.contacts.service;

import app.contacts.domain.ContactExportRow;
import app.contacts.domain.ContactFields;
import app.contacts.domain.ContactFilters;
import app.contacts.domain.ContactSort;
import app.contacts.repository.ContactQueryRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Finds the Possible duplicates among a Group's active Contacts; it only reads (R-017-05, ADR-12):
 * Contacts are bucketed by e-mail, phone and company, and compared only inside a bucket.
 */
@Service
public class DuplicateService {

  private static final String SAME_EMAIL = "sameEmail";
  private static final String SAME_PHONE = "samePhone";
  private static final String SIMILAR_NAME_SAME_COMPANY = "similarNameSameCompany";

  /** Digits a Normalised phone needs to be compared (R-017-03, [ASSUMPTION] of the story). */
  private static final int MIN_PHONE_DIGITS = 6;

  /** Name similarity from which names are similar (R-017-04, [ASSUMPTION] of the story). */
  private static final double MIN_NAME_SIMILARITY = 0.8;

  private final ContactQueryRepository repository;
  private final AccessGuard guard;
  private final Normaliser normaliser;

  /**
   * The service.
   *
   * @param repository reads the active Contacts in the Contact list order
   * @param guard the access check
   * @param normaliser Normalises phones and full names
   */
  public DuplicateService(
      ContactQueryRepository repository, AccessGuard guard, Normaliser normaliser) {
    this.repository = repository;
    this.guard = guard;
    this.normaliser = normaliser;
  }

  /**
   * Every pair of active Contacts of the Group that is a Possible duplicate, with every reason it
   * meets (R-017-01 … R-017-04), ordered by the first Contact, then the second, in the Contact list
   * order (R-014-02, D-25 (b)); every Member may see it.
   *
   * @param groupId the Group
   * @param userId the signed-in User
   * @return the pairs
   * @throws NotFoundException for a non-Member (BR-01, BR-02)
   */
  @Transactional(readOnly = true)
  public List<DuplicateCandidateView> find(UUID groupId, UUID userId) {
    guard.require(groupId, userId, Permission.READ);
    List<ContactExportRow> contacts =
        repository.findExport(groupId, "%", ContactFilters.NONE, null, ContactSort.NAME, false);
    SortedMap<Pair, List<String>> pairs = new TreeMap<>();
    addPairs(pairs, contacts, SAME_EMAIL, this::emailKey, (a, b) -> true);
    addPairs(pairs, contacts, SAME_PHONE, this::phoneKey, (a, b) -> true);
    addPairs(pairs, contacts, SIMILAR_NAME_SAME_COMPANY, this::companyKey, this::similarNames);
    return pairs.entrySet().stream()
        .map(
            pair ->
                new DuplicateCandidateView(
                    ref(contacts.get(pair.getKey().first())),
                    ref(contacts.get(pair.getKey().second())),
                    pair.getValue()))
        .toList();
  }

  /**
   * Adds the reason to every pair of Contacts with the same key that also passes the test; a
   * Contact without a key is in no pair for this reason.
   */
  private static void addPairs(
      SortedMap<Pair, List<String>> pairs,
      List<ContactExportRow> contacts,
      String reason,
      Function<ContactFields, String> key,
      BiPredicate<ContactFields, ContactFields> test) {
    Map<String, List<Integer>> buckets = new LinkedHashMap<>();
    for (int index = 0; index < contacts.size(); index++) {
      String value = key.apply(contacts.get(index).fields());
      if (value != null) {
        buckets.computeIfAbsent(value, k -> new ArrayList<>()).add(index);
      }
    }
    for (List<Integer> bucket : buckets.values()) {
      for (int i = 0; i < bucket.size(); i++) {
        for (int j = i + 1; j < bucket.size(); j++) {
          int first = bucket.get(i);
          int second = bucket.get(j);
          if (test.test(contacts.get(first).fields(), contacts.get(second).fields())) {
            pairs.computeIfAbsent(new Pair(first, second), k -> new ArrayList<>()).add(reason);
          }
        }
      }
    }
  }

  /** The e-mail compared case-insensitively (R-017-02, BR-11); blank e-mails are stored empty. */
  private String emailKey(ContactFields fields) {
    return fields.email() == null ? null : fields.email().toLowerCase(Locale.ROOT);
  }

  /** The Normalised phone, if it has enough digits (R-017-03). */
  private String phoneKey(ContactFields fields) {
    if (fields.phone() == null) {
      return null;
    }
    String phone = normaliser.phone(fields.phone());
    return phone.replace("+", "").length() < MIN_PHONE_DIGITS ? null : phone;
  }

  /** The company compared case-insensitively (R-017-04). */
  private String companyKey(ContactFields fields) {
    return fields.company() == null ? null : fields.company().toLowerCase(Locale.ROOT);
  }

  private boolean similarNames(ContactFields first, ContactFields second) {
    return NameSimilarity.of(
            normaliser.fullName(first.firstName(), first.lastName()),
            normaliser.fullName(second.firstName(), second.lastName()))
        >= MIN_NAME_SIMILARITY;
  }

  private static ContactRefView ref(ContactExportRow contact) {
    return new ContactRefView(contact.id(), contact.fields().name());
  }

  /** Two Contacts by their positions in the Contact list order, the earlier first. */
  private record Pair(int first, int second) implements Comparable<Pair> {

    @Override
    public int compareTo(Pair other) {
      return first == other.first
          ? Integer.compare(second, other.second)
          : Integer.compare(first, other.first);
    }
  }
}
