package app.contacts.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.contacts.auth.CurrentUser;
import app.contacts.service.DeletedGroupView;
import app.contacts.service.GroupService;
import app.contacts.service.GroupView;
import app.contacts.service.UserView;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeletedGroupControllerTest {

  private static final UUID ANA_ID = UUID.randomUUID();
  private static final UUID GROUP_ID = UUID.randomUUID();
  private static final CurrentUser ANA =
      new CurrentUser(new UserView(ANA_ID, "ana.kovac@example.si", "Ana Kovač", "sl"), true);
  private static final Instant MOVED = Instant.parse("2026-03-05T10:00:00Z");
  private static final Instant REMOVAL = Instant.parse("2026-04-04T10:00:00Z");

  private final GroupService groups = mock(GroupService.class);
  private final DeletedGroupController controller = new DeletedGroupController(groups);

  @Test
  void r00304GetDeletedGroupsListsNameDeletedAtAndRemovalAt() {
    when(groups.listDeletedGroups(ANA_ID))
        .thenReturn(List.of(new DeletedGroupView(GROUP_ID, "Konferenca 2025", MOVED, REMOVAL)));

    assertThat(controller.list(ANA))
        .containsExactly(new DeletedGroupResponse(GROUP_ID, "Konferenca 2025", MOVED, REMOVAL));
  }

  @Test
  void r00305RestoreAnswersTheRestoredGroup() {
    when(groups.restore(GROUP_ID, ANA_ID))
        .thenReturn(new GroupView(GROUP_ID, "Konferenca 2025", "OWNER"));

    assertThat(controller.restore(ANA, GROUP_ID))
        .isEqualTo(new GroupResponse(GROUP_ID, "Konferenca 2025", "OWNER"));
  }
}
