package app.contacts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.contacts.domain.AuditEntry;
import app.contacts.domain.Contact;
import app.contacts.domain.ContactFields;
import app.contacts.domain.Merge;
import app.contacts.domain.Role;
import app.contacts.repository.MergeRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

class MergeServiceTest {

  private static final Instant NOW = Instant.parse("2026-03-05T10:00:00Z");
  private static final UUID GROUP_ID = UUID.randomUUID();
  private static final UUID MARKO = UUID.randomUUID();
  private static final UUID PETRA = UUID.randomUUID();

  private final AccessGuard guard = mock(AccessGuard.class);
  private final ContactService contacts = mock(ContactService.class);
  private final MergeRepository merges = mock(MergeRepository.class);
  private final AuditService audit = mock(AuditService.class);
  private final MergeService service =
      new MergeService(
          guard, contacts, merges, audit, new ContactValidator(), Clock.fixed(NOW, ZoneOffset.UTC));

  private Contact janez;
  private Contact other;

  @BeforeEach
  void pair() {
    when(guard.require(GROUP_ID, MARKO, Permission.CHANGE_CONTACT)).thenReturn(Role.EDITOR);
    when(merges.add(eq(GROUP_ID), any(Merge.class))).thenAnswer(call -> call.getArgument(1));
    janez = locked(Map.of("firstName", "Janez", "lastName", "Novak"), 3);
    other = locked(Map.of("firstName", "J.", "lastName", "Novak", "phone", "+386 41 123 456"), 1);
  }

  private Contact locked(Map<String, String> fields, int versionNumber) {
    Contact contact =
        Contact.create(GROUP_ID, ContactFields.ofNames(fields), "", "", MARKO, Instant.EPOCH);
    when(contacts.lockedCurrent(GROUP_ID, contact.getId(), versionNumber)).thenReturn(contact);
    when(contacts.contentOf(contact))
        .thenReturn(new ContactContent(contact.getFields(), List.of(), List.of()));
    return contact;
  }

  private MergeCommand command(UUID primaryId, UUID otherId, Map<String, String> fieldChoices) {
    return new MergeCommand(primaryId, otherId, 3, 1, fieldChoices, null);
  }

  @Test
  void r01804MergeSavesThePrimaryMarksTheOtherAndWritesTheMergeAndAnAuditEntry() {
    MergeView merge =
        service.merge(
            GROUP_ID, MARKO, command(janez.getId(), other.getId(), Map.of("phone", "other")));

    ArgumentCaptor<ContactContent> merged = ArgumentCaptor.forClass(ContactContent.class);
    verify(contacts).saveMerged(eq(janez), eq(MARKO), merged.capture());
    assertThat(merged.getValue().fields().phone()).isEqualTo("+386 41 123 456");
    assertThat(merged.getValue().fields().firstName()).isEqualTo("Janez");
    verify(contacts).markMerged(other, janez.getId(), MARKO);
    assertThat(merge.primaryId()).isEqualTo(janez.getId());
    assertThat(merge.otherId()).isEqualTo(other.getId());
    assertThat(merge.mergedAt()).isEqualTo(NOW);
    assertThat(merge.undone()).isFalse();
    verify(audit).record(GROUP_ID, MARKO, AuditEntry.CONTACTS_MERGED, merge.id());
  }

  @Test
  void r01807BothContactsAreLockedInIdOrderSoTwoMergesOfThePairRunOneAfterTheOther() {
    service.merge(GROUP_ID, MARKO, command(janez.getId(), other.getId(), null));

    boolean janezFirst = janez.getId().compareTo(other.getId()) < 0;
    InOrder locks = inOrder(contacts);
    locks
        .verify(contacts)
        .lockedCurrent(GROUP_ID, (janezFirst ? janez : other).getId(), janezFirst ? 3 : 1);
    locks
        .verify(contacts)
        .lockedCurrent(GROUP_ID, (janezFirst ? other : janez).getId(), janezFirst ? 1 : 3);
  }

  @Test
  void ac01804ViewerIsNotAllowedAndNothingChanges() {
    when(guard.require(GROUP_ID, PETRA, Permission.CHANGE_CONTACT))
        .thenThrow(new ForbiddenException());

    assertThatThrownBy(
            () -> service.merge(GROUP_ID, PETRA, command(janez.getId(), other.getId(), null)))
        .isInstanceOf(ForbiddenException.class);
    verifyNoInteractions(contacts, merges, audit);
  }

  @Test
  void ex01805MergingOneContactWithItselfIsInvalidAndNothingChanges() {
    assertThatThrownBy(
            () -> service.merge(GROUP_ID, MARKO, command(janez.getId(), janez.getId(), null)))
        .isInstanceOfSatisfying(
            InvalidRequestException.class,
            invalid -> assertThat(invalid.getCode()).isEqualTo("merge.notPossible"));
    verifyNoInteractions(contacts, merges, audit);
  }

  @Test
  void r01807MissingIdOrVersionNumberIsInvalid() {
    MergeCommand noVersion = new MergeCommand(janez.getId(), other.getId(), null, 1, null, null);

    assertThatThrownBy(() -> service.merge(GROUP_ID, MARKO, noVersion))
        .isInstanceOf(ValidationException.class);
    verifyNoInteractions(contacts, merges, audit);
  }

  @Test
  void ex01803ContactThatIsNotActiveCannotBeMergedAndNothingChanges() {
    other.moveToRecycleBin(MARKO, NOW);
    when(contacts.lockedCurrent(GROUP_ID, other.getId(), 2)).thenReturn(other);

    assertThatThrownBy(
            () ->
                service.merge(
                    GROUP_ID,
                    MARKO,
                    new MergeCommand(janez.getId(), other.getId(), 3, 2, null, null)))
        .isInstanceOfSatisfying(
            ConflictException.class,
            conflict -> assertThat(conflict.getCode()).isEqualTo("merge.notPossible"));
    verify(contacts, never()).saveMerged(any(), any(), any());
    verifyNoInteractions(merges, audit);
  }

  @Test
  void r01802MergedFieldsWithoutNameOrCompanyAreInvalidAndNothingChanges() {
    MergeCommand blankNames =
        command(janez.getId(), other.getId(), Map.of("firstName", "other", "lastName", "other"));
    when(contacts.contentOf(other))
        .thenReturn(new ContactContent(ContactFields.EMPTY, List.of(), List.of()));

    assertThatThrownBy(() -> service.merge(GROUP_ID, MARKO, blankNames))
        .isInstanceOf(ValidationException.class);
    verify(contacts, never()).saveMerged(any(), any(), any());
    verifyNoInteractions(merges, audit);
  }
}
