package app.contacts.service;

import static app.contacts.domain.ContactFilters.NONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.contacts.domain.ContactListRow;
import app.contacts.domain.ContactSort;
import app.contacts.domain.ContactTagRow;
import app.contacts.domain.Role;
import app.contacts.repository.ContactQueryRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ContactQueryServiceTest {

  private static final Instant NOW = Instant.parse("2026-03-05T10:00:00Z");
  private static final UUID GROUP_ID = UUID.randomUUID();
  private static final UUID PETRA = UUID.randomUUID();

  private final ContactQueryRepository repository = mock(ContactQueryRepository.class);
  private final AccessGuard guard = mock(AccessGuard.class);
  private final ContactQueryService service =
      new ContactQueryService(repository, guard, new Normaliser());

  @BeforeEach
  void petraIsViewer() {
    when(guard.require(GROUP_ID, PETRA, Permission.READ)).thenReturn(Role.VIEWER);
  }

  private static ContactQuery query(String search, String sort, String dir, Integer page) {
    return new ContactQuery(search, sort, dir, page);
  }

  @Test
  void r01401FirstPageByNameWithTotalTagsAndPageSize() {
    UUID janez = UUID.randomUUID();
    UUID tag = UUID.randomUUID();
    ContactListRow row =
        new ContactListRow(janez, "Janez", "Novak", "Podjetje d.o.o.", null, null, NOW);
    when(repository.findPage(GROUP_ID, "%", NONE, ContactSort.NAME, false, 0, 50))
        .thenReturn(List.of(row));
    when(repository.count(GROUP_ID, "%", NONE)).thenReturn(1L);
    when(repository.findTags(GROUP_ID, List.of(janez)))
        .thenReturn(List.of(new ContactTagRow(janez, tag, "udelezenec2025")));

    ContactPageView page = service.page(GROUP_ID, PETRA, query(null, null, null, null));

    assertThat(page.total()).isEqualTo(1);
    assertThat(page.page()).isEqualTo(1);
    assertThat(page.pageSize()).isEqualTo(50);
    assertThat(page.items())
        .containsExactly(
            new ContactListItemView(
                janez,
                "Janez",
                "Novak",
                "Podjetje d.o.o.",
                null,
                null,
                List.of(new TagView(tag, "udelezenec2025")),
                NOW));
  }

  @Test
  void ac01405PageThreeStartsAfterOneHundredContacts() {
    service.page(GROUP_ID, PETRA, query(null, "name", "asc", 3));

    verify(repository).findPage(GROUP_ID, "%", NONE, ContactSort.NAME, false, 100, 50);
  }

  @ParameterizedTest
  @CsvSource({
    "name, asc, NAME, false",
    "name, desc, NAME, true",
    "company, asc, COMPANY, false",
    "email, desc, EMAIL, true",
    "updatedAt, desc, UPDATED_AT, true"
  })
  void r01403SortAndOrderAreChosenByTheUser(
      String sort, String dir, ContactSort expected, boolean descending) {
    service.page(GROUP_ID, PETRA, query(null, sort, dir, 1));

    verify(repository).findPage(GROUP_ID, "%", NONE, expected, descending, 0, 50);
  }

  @Test
  void r01404Ex01403SearchIsTrimmedAndNormalised() {
    service.page(GROUP_ID, PETRA, query("  Čuk ", null, null, null));

    verify(repository).findPage(GROUP_ID, "%cuk%", NONE, ContactSort.NAME, false, 0, 50);
    verify(repository).count(GROUP_ID, "%cuk%", NONE);
  }

  @Test
  void ex01405PercentUnderscoreAndBackslashAreEscaped() {
    service.page(GROUP_ID, PETRA, query("100%_a\\b", null, null, null));

    verify(repository).count(GROUP_ID, "%100\\%\\_a\\\\b%", NONE);
  }

  @ParameterizedTest
  @CsvSource({"sort, size, asc, 1", "dir, name, up, 1", "page, name, asc, 0"})
  void r01403InvalidSortOrderOrPageIsRejected(String field, String sort, String dir, int page) {
    assertThatThrownBy(() -> service.page(GROUP_ID, PETRA, query(null, sort, dir, page)))
        .as(field)
        .isInstanceOf(ValidationException.class);
    verifyNoInteractions(repository);
  }

  @Test
  void r01404SearchOfMoreThan200CharactersIsRejected() {
    assertThatThrownBy(() -> service.page(GROUP_ID, PETRA, query("a".repeat(201), null, null, 1)))
        .isInstanceOf(ValidationException.class);
    verifyNoInteractions(repository);
  }

  @Test
  void br02NonMemberGetsNotFound() {
    UUID other = UUID.randomUUID();
    when(guard.require(other, PETRA, Permission.READ)).thenThrow(new NotFoundException());

    assertThatThrownBy(() -> service.page(other, PETRA, query(null, null, null, null)))
        .isInstanceOf(NotFoundException.class);
    verifyNoInteractions(repository);
  }

  @Test
  void ex01406PageBeyondTheLastIsEmptyWithItsTotal() {
    when(repository.count(eq(GROUP_ID), anyString(), eq(NONE))).thenReturn(3L);
    when(repository.findPage(
            eq(GROUP_ID),
            anyString(),
            eq(NONE),
            any(ContactSort.class),
            anyBoolean(),
            anyInt(),
            anyInt()))
        .thenReturn(List.of());

    ContactPageView page = service.page(GROUP_ID, PETRA, query(null, null, null, 9));

    assertThat(page.items()).isEmpty();
    assertThat(page.total()).isEqualTo(3);
    assertThat(page.page()).isEqualTo(9);
  }
}
