package app.contacts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.contacts.domain.AuditEntry;
import app.contacts.domain.Contact;
import app.contacts.domain.ContactFields;
import app.contacts.domain.Role;
import app.contacts.repository.ContactRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class PermanentDeleteServiceTest {

  private static final Instant MOVED_AT = Instant.parse("2026-03-05T10:00:00Z");
  private static final UUID GROUP_ID = UUID.randomUUID();
  private static final UUID ANA = UUID.randomUUID();
  private static final UUID MARKO = UUID.randomUUID();

  private final AccessGuard guard = mock(AccessGuard.class);
  private final ContactRepository contacts = mock(ContactRepository.class);
  private final AuditService audit = mock(AuditService.class);
  private final PermanentDeleteService service = new PermanentDeleteService(guard, contacts, audit);

  private Contact janez(boolean inRecycleBin) {
    Contact janez =
        Contact.create(
            GROUP_ID,
            ContactFields.ofNames(Map.of("firstName", "Janez", "lastName", "Novak")),
            "",
            "",
            MARKO,
            Instant.EPOCH);
    if (inRecycleBin) {
      janez.moveToRecycleBin(MARKO, MOVED_AT);
    }
    when(contacts.findForUpdate(GROUP_ID, janez.getId())).thenReturn(Optional.of(janez));
    return janez;
  }

  @Test
  void r01301OwnerDeletesTheContactInTheRecycleBinWithAnAuditEntry() {
    when(guard.require(GROUP_ID, ANA, Permission.PERMANENTLY_DELETE)).thenReturn(Role.OWNER);
    Contact janez = janez(true);

    service.permanentlyDelete(GROUP_ID, ANA, janez.getId());

    verify(contacts).permanentlyDelete(GROUP_ID, janez.getId());
    verify(audit).record(GROUP_ID, ANA, AuditEntry.CONTACT_PERMANENTLY_DELETED, janez.getId());
  }

  @Test
  void ac01302EditorIsNotAllowedAndNothingChanges() {
    when(guard.require(GROUP_ID, MARKO, Permission.PERMANENTLY_DELETE))
        .thenThrow(new ForbiddenException());
    UUID id = UUID.randomUUID();

    assertThatThrownBy(() -> service.permanentlyDelete(GROUP_ID, MARKO, id))
        .isInstanceOf(ForbiddenException.class);
    verifyNoInteractions(contacts, audit);
  }

  @Test
  void r01303ActiveContactIsRejectedAndNothingChanges() {
    when(guard.require(GROUP_ID, ANA, Permission.PERMANENTLY_DELETE)).thenReturn(Role.OWNER);
    Contact maja = janez(false);

    assertThatThrownBy(() -> service.permanentlyDelete(GROUP_ID, ANA, maja.getId()))
        .isInstanceOfSatisfying(
            ConflictException.class,
            e -> assertThat(e.getCode()).isEqualTo("contact.notInRecycleBin"));
    verify(contacts, never()).permanentlyDelete(any(), any());
    verifyNoInteractions(audit);
  }

  @Test
  void ex01304UnknownContactIsNotFound() {
    when(guard.require(GROUP_ID, ANA, Permission.PERMANENTLY_DELETE)).thenReturn(Role.OWNER);
    UUID unknown = UUID.randomUUID();
    when(contacts.findForUpdate(GROUP_ID, unknown)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.permanentlyDelete(GROUP_ID, ANA, unknown))
        .isInstanceOf(NotFoundException.class);
    verifyNoInteractions(audit);
  }

  @Test
  void br08RemovesTheDueContactsWithAnAuditEntryEachByTheUserWhoMovedThem() {
    Contact janez = janez(true);
    Instant cutoff = MOVED_AT;
    when(contacts.findDueForRemoval(GROUP_ID, cutoff)).thenReturn(List.of(janez));
    when(contacts.permanentlyDeleteDue(GROUP_ID, cutoff)).thenReturn(1);

    assertThat(service.permanentlyDeleteDue(GROUP_ID, cutoff)).isOne();

    verify(audit).record(GROUP_ID, MARKO, AuditEntry.CONTACT_REMOVED, janez.getId());
    verify(contacts).permanentlyDeleteDue(GROUP_ID, cutoff);
    verifyNoInteractions(guard);
  }

  /** {@code merged} merged into {@code primary} by Marko. */
  private Contact mergedInto(Contact primary, String firstName) {
    Contact merged =
        Contact.create(
            GROUP_ID,
            ContactFields.ofNames(Map.of("firstName", firstName)),
            "",
            "",
            MARKO,
            Instant.EPOCH);
    merged.mergeInto(primary.getId(), MARKO, Instant.EPOCH);
    when(contacts.findByGroupIdAndMergedInto(GROUP_ID, primary.getId()))
        .thenReturn(List.of(merged));
    return merged;
  }

  @Test
  void d21DeletingPrimaryContactAlsoDeletesTheContactsMergedIntoItDeepestFirst() {
    when(guard.require(GROUP_ID, ANA, Permission.PERMANENTLY_DELETE)).thenReturn(Role.OWNER);
    Contact janez = janez(true);
    Contact other = mergedInto(janez, "J.");
    Contact older = mergedInto(other, "Jani");

    service.permanentlyDelete(GROUP_ID, ANA, janez.getId());

    InOrder deletes = inOrder(contacts);
    deletes.verify(contacts).permanentlyDelete(GROUP_ID, older.getId());
    deletes.verify(contacts).permanentlyDelete(GROUP_ID, other.getId());
    deletes.verify(contacts).permanentlyDelete(GROUP_ID, janez.getId());
    for (Contact deleted : List.of(janez, other, older)) {
      verify(audit).record(GROUP_ID, ANA, AuditEntry.CONTACT_PERMANENTLY_DELETED, deleted.getId());
    }
  }

  @Test
  void d21RemovingDuePrimaryContactAlsoRemovesTheContactsMergedIntoIt() {
    Contact janez = janez(true);
    Contact other = mergedInto(janez, "J.");
    Instant cutoff = MOVED_AT;
    when(contacts.findDueForRemoval(GROUP_ID, cutoff)).thenReturn(List.of(janez));

    service.permanentlyDeleteDue(GROUP_ID, cutoff);

    InOrder deletes = inOrder(contacts);
    deletes.verify(contacts).permanentlyDelete(GROUP_ID, other.getId());
    deletes.verify(contacts).permanentlyDeleteDue(GROUP_ID, cutoff);
    verify(audit).record(GROUP_ID, MARKO, AuditEntry.CONTACT_REMOVED, other.getId());
    verify(audit).record(GROUP_ID, MARKO, AuditEntry.CONTACT_REMOVED, janez.getId());
  }
}
