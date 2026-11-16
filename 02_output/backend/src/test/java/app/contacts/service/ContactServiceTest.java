package app.contacts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.contacts.domain.Contact;
import app.contacts.domain.ContactFields;
import app.contacts.domain.ContactVersion;
import app.contacts.domain.Role;
import app.contacts.repository.ContactRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ContactServiceTest {

  private static final Instant NOW = Instant.parse("2026-03-05T10:00:00Z");
  private static final UUID GROUP_ID = UUID.randomUUID();
  private static final UUID MARKO = UUID.randomUUID();
  private static final UUID PETRA = UUID.randomUUID();

  private final ContactRepository contacts = mock(ContactRepository.class);
  private final VersionWriter versionWriter = mock(VersionWriter.class);
  private final AccessGuard guard = mock(AccessGuard.class);
  private final ContactService service =
      new ContactService(
          contacts,
          versionWriter,
          guard,
          new ContactValidator(),
          new Normaliser(),
          Clock.fixed(NOW, ZoneOffset.UTC));

  @BeforeEach
  void markoIsEditor() {
    when(guard.require(GROUP_ID, MARKO, Permission.CHANGE_CONTACT)).thenReturn(Role.EDITOR);
    when(contacts.add(eq(GROUP_ID), any(Contact.class))).thenAnswer(call -> call.getArgument(1));
  }

  @Test
  void r00601EditorCreatesTheContactWithVersionOneCreated() {
    ContactView created =
        service.create(
            GROUP_ID, MARKO, Map.of("firstName", "Janez", "lastName", "Novak", "country", "SI"));

    ArgumentCaptor<Contact> stored = ArgumentCaptor.forClass(Contact.class);
    verify(contacts).add(eq(GROUP_ID), stored.capture());
    verify(versionWriter).record(stored.getValue(), ContactVersion.CREATED, ContactFields.EMPTY);
    assertThat(created.id()).isEqualTo(stored.getValue().getId());
    assertThat(created.versionNumber()).isEqualTo(1);
    assertThat(created.state()).isEqualTo("active");
    assertThat(created.fields()).containsEntry("lastName", "Novak").containsEntry("email", null);
    assertThat(created.createdAt()).isEqualTo(NOW);
    assertThat(created.updatedAt()).isEqualTo(NOW);
  }

  @Test
  void r00601StoresTheDerivedSearchTextAndSortName() {
    service.create(GROUP_ID, MARKO, Map.of("firstName", "Žiga", "lastName", "Čuk"));

    ArgumentCaptor<Contact> stored = ArgumentCaptor.forClass(Contact.class);
    verify(contacts).add(eq(GROUP_ID), stored.capture());
    assertThat(stored.getValue().getSearchText()).isEqualTo("ziga cuk");
    assertThat(stored.getValue().getSortName()).isEqualTo("Čuk");
  }

  @Test
  void r00607StoresTheTrimmedFields() {
    ContactView created = service.create(GROUP_ID, MARKO, Map.of("firstName", "  Janez "));

    assertThat(created.fields()).containsEntry("firstName", "Janez");
  }

  @Test
  void r00602InvalidFieldsStoreNothing() {
    assertThatThrownBy(() -> service.create(GROUP_ID, MARKO, Map.of("email", "info@example.si")))
        .isInstanceOf(ValidationException.class);
    verifyNoInteractions(contacts, versionWriter);
  }

  @Test
  void r00601UnknownFieldNamesAreRejected() {
    assertThatThrownBy(
            () -> service.create(GROUP_ID, MARKO, Map.of("firstName", "Janez", "nickname", "J")))
        .isInstanceOf(ValidationException.class);
    verifyNoInteractions(contacts, versionWriter);
  }

  @Test
  void r00601MissingFieldsAreRejected() {
    assertThatThrownBy(() -> service.create(GROUP_ID, MARKO, null))
        .isInstanceOf(ValidationException.class);
    verifyNoInteractions(contacts, versionWriter);
  }

  @Test
  void r00601NullFieldValuesAreEmptyFields() {
    Map<String, String> fields = new HashMap<>();
    fields.put("company", "Podjetje d.o.o.");
    fields.put("firstName", null);

    assertThat(service.create(GROUP_ID, MARKO, fields).fields())
        .containsEntry("company", "Podjetje d.o.o.");
  }

  @Test
  void ac00606ViewerIsForbiddenBeforeAnyCheckAndNothingIsStored() {
    when(guard.require(GROUP_ID, PETRA, Permission.CHANGE_CONTACT))
        .thenThrow(new ForbiddenException());

    assertThatThrownBy(() -> service.create(GROUP_ID, PETRA, Map.of()))
        .isInstanceOf(ForbiddenException.class);
    verifyNoInteractions(contacts, versionWriter);
  }

  @Test
  void ex00609NonMemberGetsNotFoundAndNothingIsStored() {
    UUID other = UUID.randomUUID();
    when(guard.require(other, MARKO, Permission.CHANGE_CONTACT)).thenThrow(new NotFoundException());

    assertThatThrownBy(() -> service.create(other, MARKO, Map.of("firstName", "Janez")))
        .isInstanceOf(NotFoundException.class);
    verifyNoInteractions(contacts, versionWriter);
  }

  @Test
  void r00606ListsTheActiveContactsWithTheEmailForAnyMember() {
    when(guard.require(GROUP_ID, PETRA, Permission.READ)).thenReturn(Role.VIEWER);
    Contact janez = contact(Map.of("firstName", "Janez", "lastName", "Novak"));
    Contact company = contact(Map.of("company", "Podjetje d.o.o."));
    Contact first = contact(Map.of("firstName", "Janez"));
    when(contacts.findActiveByEmail(GROUP_ID, "Janez.Novak@example.si"))
        .thenReturn(List.of(janez, company, first));

    assertThat(service.findByEmail(GROUP_ID, PETRA, " Janez.Novak@example.si "))
        .containsExactly(
            new ContactRefView(janez.getId(), "Janez Novak"),
            new ContactRefView(company.getId(), "Podjetje d.o.o."),
            new ContactRefView(first.getId(), "Janez"));
  }

  @Test
  void r00606BlankEmailMatchesNothing() {
    when(guard.require(GROUP_ID, PETRA, Permission.READ)).thenReturn(Role.VIEWER);

    assertThat(service.findByEmail(GROUP_ID, PETRA, "  ")).isEmpty();
    verifyNoInteractions(contacts);
  }

  @Test
  void br02NonMemberGetsNotFoundForEmailMatches() {
    UUID other = UUID.randomUUID();
    when(guard.require(other, PETRA, Permission.READ)).thenThrow(new NotFoundException());

    assertThatThrownBy(() -> service.findByEmail(other, PETRA, "janez.novak@example.si"))
        .isInstanceOf(NotFoundException.class);
    verifyNoInteractions(contacts);
  }

  private static Contact contact(Map<String, String> fields) {
    return Contact.create(GROUP_ID, ContactFields.ofNames(fields), "", "", MARKO, NOW);
  }
}
