package app.contacts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.contacts.domain.Role;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecycleBinServiceTest {

  private static final UUID GROUP_ID = UUID.randomUUID();
  private static final UUID CONTACT_ID = UUID.randomUUID();
  private static final UUID MARKO = UUID.randomUUID();
  private static final UUID PETRA = UUID.randomUUID();

  private final AccessGuard guard = mock(AccessGuard.class);
  private final ContactService contacts = mock(ContactService.class);
  private final RecycleBinService service = new RecycleBinService(guard, contacts);

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
}
