package app.contacts.web;

import app.contacts.auth.CurrentUser;
import app.contacts.service.GroupService;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The Owner's Groups in the recycle bin ({@code /deleted-groups}, US-003). */
@RestController
@RequestMapping("/api/v1/deleted-groups")
public class DeletedGroupController {

  private final GroupService groups;

  /**
   * The controller.
   *
   * @param groups the Group service
   */
  public DeletedGroupController(GroupService groups) {
    this.groups = groups;
  }

  /**
   * The caller's Groups in the recycle bin with their removal date (R-003-04).
   *
   * @param current the signed-in User
   * @return the Groups, newest first
   */
  @GetMapping
  public List<DeletedGroupResponse> list(@AuthenticationPrincipal CurrentUser current) {
    return groups.listDeletedGroups(current.id()).stream().map(DeletedGroupResponse::of).toList();
  }

  /**
   * Restores the Group before its removal date (R-003-05, R-003-06).
   *
   * @param current the signed-in User
   * @param groupId the Group
   * @return the restored Group
   */
  @PostMapping("/{groupId}/restore")
  public GroupResponse restore(
      @AuthenticationPrincipal CurrentUser current, @PathVariable UUID groupId) {
    return GroupResponse.of(groups.restore(groupId, current.id()));
  }
}
