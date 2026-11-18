package app.contacts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.contacts.domain.Contact;
import app.contacts.domain.ContactFields;
import app.contacts.domain.Role;
import app.contacts.domain.User;
import app.contacts.repository.ContactRepository;
import app.contacts.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecycleBinServiceTest {

  private static final UUID GROUP_ID = UUID.randomUUID();
  private static final UUID CONTACT_ID = UUID.randomUUID();
  private static final UUID MARKO = UUID.randomUUID();
  private static final UUID PETRA = UUID.randomUUID();

  private final AccessGuard guard = mock(AccessGuard.class);
  private final ContactService contacts = mock(ContactService.class);
  private final ContactRepository contactRepository = mock(ContactRepository.class);
  private final UserRepository users = mock(UserRepository.class);
  private final RecycleBinService service =
      new RecycleBinService(guard, contacts, contactRepository, users);

  @Test
  void ac01201RecycleBinListsNameDeletedByDeletedAtAndRemovalDateNewestFirst() {
    final User marko =
        User.create("uid-marko", "marko.kranjc@gmail.com", "Marko Kranjc", Instant.EPOCH);
    Contact janez =
        Contact.create(
            GROUP_ID,
            ContactFields.ofNames(Map.of("firstName", "Janez", "lastName", "Novak")),
            "",
            "",
            marko.getId(),
            Instant.EPOCH);
    janez.moveToRecycleBin(marko.getId(), Instant.parse("2026-03-05T10:00:00Z"));
    when(guard.require(GROUP_ID, MARKO, Permission.VIEW_RECYCLE_BIN)).thenReturn(Role.EDITOR);
    when(contactRepository.findInRecycleBin(GROUP_ID)).thenReturn(List.of(janez));
    when(users.findAllById(List.of(marko.getId()))).thenReturn(List.of(marko));

    List<RecycleBinEntryView> entries = service.list(GROUP_ID, MARKO);

    assertThat(entries)
        .containsExactly(
            new RecycleBinEntryView(
                new ContactRefView(janez.getId(), "Janez Novak"),
                2,
                "Marko Kranjc",
                Instant.parse("2026-03-05T10:00:00Z"),
                Instant.parse("2026-04-04T10:00:00Z")));
  }

  @Test
  void ac01205ViewerCannotOpenTheRecycleBin() {
    when(guard.require(GROUP_ID, PETRA, Permission.VIEW_RECYCLE_BIN))
        .thenThrow(new ForbiddenException());

    assertThatThrownBy(() -> service.list(GROUP_ID, PETRA)).isInstanceOf(ForbiddenException.class);
    verifyNoInteractions(contactRepository);
  }

  @Test
  void r01201EditorMovesTheContactToTheRecycleBin() {
    when(guard.require(GROUP_ID, MARKO, Permission.CHANGE_CONTACT)).thenReturn(Role.EDITOR);
    ContactView moved = mock(ContactView.class);
    when(contacts.moveToRecycleBin(GROUP_ID, MARKO, CONTACT_ID, 3)).thenReturn(moved);

    assertThat(service.move(GROUP_ID, MARKO, CONTACT_ID, 3)).isSameAs(moved);
  }

  @Test
  void ac01204ViewerCannotMoveAndNothingIsRead() {
    when(guard.require(GROUP_ID, PETRA, Permission.CHANGE_CONTACT))
        .thenThrow(new ForbiddenException());

    assertThatThrownBy(() -> service.move(GROUP_ID, PETRA, CONTACT_ID, 3))
        .isInstanceOf(ForbiddenException.class);
    verifyNoInteractions(contacts);
  }

  @Test
  void r01201MoveWithoutVersionNumberIsRejected() {
    assertThatThrownBy(() -> service.move(GROUP_ID, MARKO, CONTACT_ID, null))
        .isInstanceOf(ValidationException.class);
    verifyNoInteractions(contacts);
  }

  @Test
  void r01203EditorRestoresTheContactFromTheRecycleBin() {
    when(guard.require(GROUP_ID, MARKO, Permission.CHANGE_CONTACT)).thenReturn(Role.EDITOR);
    ContactView restored = mock(ContactView.class);
    when(contacts.restoreFromRecycleBin(GROUP_ID, MARKO, CONTACT_ID, 4)).thenReturn(restored);

    assertThat(service.restore(GROUP_ID, MARKO, CONTACT_ID, 4)).isSameAs(restored);
  }

  @Test
  void br03ViewerCannotRestoreAndNothingIsRead() {
    when(guard.require(GROUP_ID, PETRA, Permission.CHANGE_CONTACT))
        .thenThrow(new ForbiddenException());

    assertThatThrownBy(() -> service.restore(GROUP_ID, PETRA, CONTACT_ID, 4))
        .isInstanceOf(ForbiddenException.class);
    assertThatThrownBy(() -> service.restore(GROUP_ID, MARKO, CONTACT_ID, null))
        .isInstanceOf(ValidationException.class);
    verifyNoInteractions(contacts);
  }

  @Test
  void r02103ContactDeletedByDeletedUserShowsTheCodeOfDeletedUser() {
    final User marko =
        User.create("uid-marko", "marko.kranjc@gmail.com", "Marko Kranjc", Instant.EPOCH);
    Contact janez =
        Contact.create(
            GROUP_ID,
            ContactFields.ofNames(Map.of("firstName", "Janez")),
            "",
            "",
            marko.getId(),
            Instant.EPOCH);
    janez.moveToRecycleBin(marko.getId(), Instant.parse("2026-03-05T10:00:00Z"));
    marko.anonymise(Instant.parse("2026-03-06T10:00:00Z"));
    when(guard.require(GROUP_ID, MARKO, Permission.VIEW_RECYCLE_BIN)).thenReturn(Role.EDITOR);
    when(contactRepository.findInRecycleBin(GROUP_ID)).thenReturn(List.of(janez));
    when(users.findAllById(List.of(marko.getId()))).thenReturn(List.of(marko));

    assertThat(service.list(GROUP_ID, MARKO).get(0).deletedBy()).isEqualTo("user.deleted");
  }
}
