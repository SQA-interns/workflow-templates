package app.contacts.web;

import app.contacts.auth.CurrentUser;
import app.contacts.service.GroupService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Groups of the signed-in User ({@code /groups}, US-002, US-003). */
@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {

  private final GroupService groups;

  /**
   * The controller.
   *
   * @param groups the Group service
   */
  public GroupController(GroupService groups) {
    this.groups = groups;
  }

  /**
   * Creates a Group; the caller becomes its Owner (R-002-01).
   *
   * @param current the signed-in User
   * @param body the name
   * @return the new Group (201)
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public GroupResponse create(
      @AuthenticationPrincipal CurrentUser current, @RequestBody GroupNameRequest body) {
    return GroupResponse.of(groups.create(current.id(), body.name()));
  }

  /**
   * The active Groups of the caller, by name A–Z (R-002-03).
   *
   * @param current the signed-in User
   * @return the Groups
   */
  @GetMapping
  public List<GroupResponse> list(@AuthenticationPrincipal CurrentUser current) {
    return groups.listGroups(current.id()).stream().map(GroupResponse::of).toList();
  }

  /**
   * One Group of the caller; 404 for others (BR-02, EX-002-05).
   *
   * @param current the signed-in User
   * @param groupId the Group
   * @return the Group
   */
  @GetMapping("/{groupId}")
  public GroupResponse get(
      @AuthenticationPrincipal CurrentUser current, @PathVariable UUID groupId) {
    return GroupResponse.of(groups.getGroup(groupId, current.id()));
  }

  /**
   * Renames the Group; Owner only (R-003-01, BR-04).
   *
   * @param current the signed-in User
   * @param groupId the Group
   * @param body the new name
   * @return the renamed Group
   */
  @PatchMapping("/{groupId}")
  public GroupResponse rename(
      @AuthenticationPrincipal CurrentUser current,
      @PathVariable UUID groupId,
      @RequestBody GroupNameRequest body) {
    return GroupResponse.of(groups.rename(groupId, current.id(), body.name()));
  }

  /**
   * Moves the Group to the recycle bin; Owner only (R-003-02, BR-04).
   *
   * @param current the signed-in User
   * @param groupId the Group
   */
  @PostMapping("/{groupId}/move-to-recycle-bin")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void moveToRecycleBin(
      @AuthenticationPrincipal CurrentUser current, @PathVariable UUID groupId) {
    groups.moveToRecycleBin(groupId, current.id());
  }
}
