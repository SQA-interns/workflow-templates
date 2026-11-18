package app.contacts.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.contacts.auth.CurrentUser;
import app.contacts.service.GroupService;
import app.contacts.service.GroupView;
import app.contacts.service.UserView;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class GroupControllerTest {

  private static final UUID ANA_ID = UUID.randomUUID();
  private static final UUID GROUP_ID = UUID.randomUUID();
  private static final CurrentUser ANA =
      new CurrentUser(new UserView(ANA_ID, "ana.kovac@example.si", "Ana Kovač", "sl"), true);

  private final GroupService groups = mock(GroupService.class);
  private final GroupController controller = new GroupController(groups);

  @Test
  void r00201PostGroupsAnswers201WithTheNewGroup() {
    when(groups.create(ANA_ID, "Konferenca 2025"))
        .thenReturn(new GroupView(GROUP_ID, "Konferenca 2025", "OWNER"));

    GroupResponse created = controller.create(ANA, new GroupNameRequest("Konferenca 2025"));

    assertThat(ResponseStatuses.of(GroupController.class, "create")).isEqualTo(HttpStatus.CREATED);
    assertThat(created).isEqualTo(new GroupResponse(GROUP_ID, "Konferenca 2025", "OWNER"));
  }

  @Test
  void r00203GetGroupsListsTheCallersGroups() {
    when(groups.listGroups(ANA_ID))
        .thenReturn(List.of(new GroupView(GROUP_ID, "Podjetje Beta", "VIEWER")));

    assertThat(controller.list(ANA))
        .containsExactly(new GroupResponse(GROUP_ID, "Podjetje Beta", "VIEWER"));
  }

  @Test
  void ex00205GetGroupAnswersTheGroupOfTheCaller() {
    when(groups.getGroup(GROUP_ID, ANA_ID))
        .thenReturn(new GroupView(GROUP_ID, "Konferenca 2025", "OWNER"));

    assertThat(controller.get(ANA, GROUP_ID))
        .isEqualTo(new GroupResponse(GROUP_ID, "Konferenca 2025", "OWNER"));
  }

  @Test
  void r00301PatchGroupAnswersTheRenamedGroup() {
    when(groups.rename(GROUP_ID, ANA_ID, "Konferenca 2026"))
        .thenReturn(new GroupView(GROUP_ID, "Konferenca 2026", "OWNER"));

    assertThat(controller.rename(ANA, GROUP_ID, new GroupNameRequest("Konferenca 2026")))
        .isEqualTo(new GroupResponse(GROUP_ID, "Konferenca 2026", "OWNER"));
  }

  @Test
  void r00302MoveToRecycleBinAnswers204() {
    controller.moveToRecycleBin(ANA, GROUP_ID);

    assertThat(ResponseStatuses.of(GroupController.class, "moveToRecycleBin"))
        .isEqualTo(HttpStatus.NO_CONTENT);
    verify(groups).moveToRecycleBin(GROUP_ID, ANA_ID);
  }
}
