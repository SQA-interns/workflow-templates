package app.contacts.service;

import static app.contacts.domain.ContactFilters.NONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import app.contacts.domain.ContactExportRow;
import app.contacts.domain.ContactFields;
import app.contacts.domain.ContactSort;
import app.contacts.domain.Role;
import app.contacts.repository.ContactQueryRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DuplicateServiceTest {

  private static final UUID GROUP_ID = UUID.randomUUID();
  private static final UUID MARKO = UUID.randomUUID();

  private final ContactQueryRepository repository = mock(ContactQueryRepository.class);
  private final AccessGuard guard = mock(AccessGuard.class);
  private final DuplicateService service =
      new DuplicateService(repository, guard, new Normaliser());

  @BeforeEach
  void markoIsEditor() {
    when(guard.require(GROUP_ID, MARKO, Permission.READ)).thenReturn(Role.EDITOR);
  }

  /** A Contact from name/value pairs, e.g. {@code contact("firstName", "Janez")}. */
  private static ContactExportRow contact(String... values) {
    Map<String, String> fields = new HashMap<>();
    for (int i = 0; i < values.length; i += 2) {
      fields.put(values[i], values[i + 1]);
    }
    return new ContactExportRow(UUID.randomUUID(), ContactFields.ofNames(fields));
  }

  /** The active Contacts of the Group, in the order of the Contact list (R-014-02). */
  private void active(ContactExportRow... rows) {
    when(repository.findExport(GROUP_ID, "%", NONE, null, ContactSort.NAME, false))
        .thenReturn(List.of(rows));
  }

  private List<DuplicateCandidateView> find() {
    return service.find(GROUP_ID, MARKO);
  }

  private static DuplicateCandidateView candidate(
      ContactExportRow first, ContactExportRow second, String... reasons) {
    return new DuplicateCandidateView(ref(first), ref(second), List.of(reasons));
  }

  private static ContactRefView ref(ContactExportRow row) {
    return new ContactRefView(row.id(), row.fields().name());
  }

  @Test
  void ac01701SameEmailInAnotherCaseIsPaired() {
    ContactExportRow janez =
        contact("firstName", "Janez", "lastName", "Novak", "email", "janez.novak@example.si");
    ContactExportRow novak =
        contact("firstName", "J.", "lastName", "Novak", "email", "Janez.Novak@example.si");
    active(janez, novak);

    assertThat(find()).containsExactly(candidate(janez, novak, "sameEmail"));
  }

  @Test
  void ex01701ContactsWithoutEmailAreNoPair() {
    active(contact("firstName", "Janez"), contact("firstName", "Maja"));

    assertThat(find()).isEmpty();
  }

  @Test
  void ac01702PhonesEqualAfterNormalisingArePaired() {
    ContactExportRow janez = contact("firstName", "Janez", "phone", "+386 41 123 456");
    ContactExportRow novak = contact("firstName", "Novak", "phone", "+386-41-123-456");
    active(janez, novak);

    assertThat(find()).containsExactly(candidate(janez, novak, "samePhone"));
  }

  @Test
  void ex01702PhonesWithFiveDigitsAreNoPair() {
    active(
        contact("firstName", "Janez", "phone", "12345"),
        contact("lastName", "Zupan", "phone", "12345"));

    assertThat(find()).isEmpty();
  }

  @Test
  void ac01703SimilarNamesWithTheSameCompanyArePaired() {
    ContactExportRow novak =
        contact("firstName", "Janez", "lastName", "Novak", "company", "Podjetje d.o.o.");
    ContactExportRow novack =
        contact("firstName", "Janez", "lastName", "Novack", "company", "Podjetje d.o.o.");
    active(novak, novack);

    assertThat(find()).containsExactly(candidate(novak, novack, "similarNameSameCompany"));
  }

  @Test
  void ac01704TheSameNameWithAnotherCompanyIsNoPair() {
    active(
        contact("firstName", "Janez", "lastName", "Novak", "company", "Podjetje d.o.o."),
        contact("firstName", "Janez", "lastName", "Novak", "company", "Alfa d.o.o."));

    assertThat(find()).isEmpty();
  }

  @Test
  void r01704TheSameNameWithoutCompaniesIsNoPair() {
    active(
        contact("firstName", "Janez", "lastName", "Novak"),
        contact("firstName", "Janez", "lastName", "Novak"));

    assertThat(find()).isEmpty();
  }

  @Test
  void r01704DissimilarNamesWithTheSameCompanyAreNoPair() {
    active(
        contact("firstName", "Janez", "lastName", "Novak", "company", "Podjetje d.o.o."),
        contact("firstName", "Maja", "lastName", "Zupan", "company", "Podjetje d.o.o."));

    assertThat(find()).isEmpty();
  }

  @Test
  void r01704CompanyOnlyContactsOfTheSameCompanyArePaired() {
    // Two empty Normalised full names are equal, so similar ([ASSUMPTION] D-70).
    ContactExportRow alfa = contact("company", "Alfa d.o.o.");
    ContactExportRow alfaLower = contact("company", "ALFA d.o.o.");
    active(alfa, alfaLower);

    assertThat(find()).containsExactly(candidate(alfa, alfaLower, "similarNameSameCompany"));
  }

  @Test
  void ex01706NamesWithoutDiacriticsAndCompanyInAnotherCaseArePaired() {
    ContactExportRow ziga =
        contact("firstName", "Žiga", "lastName", "Čuk", "company", "Alfa d.o.o.");
    ContactExportRow zigaPlain =
        contact("firstName", "Ziga", "lastName", "Cuk", "company", "alfa d.o.o.");
    active(ziga, zigaPlain);

    assertThat(find()).containsExactly(candidate(ziga, zigaPlain, "similarNameSameCompany"));
  }

  @Test
  void ex01703SameEmailAndPhoneGiveOnePairWithBothReasons() {
    ContactExportRow janez =
        contact("firstName", "Janez", "email", "j@n.si", "phone", "041 123 456");
    ContactExportRow maja = contact("firstName", "Maja", "email", "J@N.si", "phone", "041123456");
    active(janez, maja);

    assertThat(find()).containsExactly(candidate(janez, maja, "sameEmail", "samePhone"));
  }

  @Test
  void r01701EveryPairInListOrderWithTheEarlierContactFirst() {
    ContactExportRow cuk = contact("lastName", "Čuk", "email", "j@n.si");
    ContactExportRow novak = contact("lastName", "Novak", "phone", "041 123 456");
    ContactExportRow zupan = contact("lastName", "Zupan", "email", "J@n.si", "phone", "041123456");
    active(cuk, novak, zupan);

    assertThat(find())
        .containsExactly(candidate(cuk, zupan, "sameEmail"), candidate(novak, zupan, "samePhone"));
  }

  @Test
  void r01705FindingOnlyReadsTheActiveContacts() {
    active(
        contact("firstName", "Janez", "email", "j@n.si"),
        contact("firstName", "J.", "email", "j@n.si"));

    find();

    verify(repository).findExport(GROUP_ID, "%", NONE, null, ContactSort.NAME, false);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void ex01705NonMemberGetsNotFound() {
    when(guard.require(GROUP_ID, MARKO, Permission.READ)).thenThrow(new NotFoundException());

    assertThatThrownBy(this::find).isInstanceOf(NotFoundException.class);
  }
}
