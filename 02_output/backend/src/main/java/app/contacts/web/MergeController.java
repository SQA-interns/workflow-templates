package app.contacts.web;

import app.contacts.auth.CurrentUser;
import app.contacts.service.MergeService;
import app.contacts.service.MergeView;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Merges of a Group's Contacts (US-018) and their undo (US-019). */
@RestController
@RequestMapping("/api/v1/groups/{groupId}/merges")
public class MergeController {

  private final MergeService merges;

  /**
   * The controller.
   *
   * @param merges the Merge service
   */
  public MergeController(MergeService merges) {
    this.merges = merges;
  }

  /**
   * Merges the Other contact into the Primary contact (R-018-02 … R-018-07); the service view is
   * the contract's {@code Merge}.
   *
   * @param current the signed-in User
   * @param groupId the Group
   * @param request the Merge as confirmed
   * @return 201 with the Merge
   */
  @PostMapping
  public ResponseEntity<MergeView> merge(
      @AuthenticationPrincipal CurrentUser current,
      @PathVariable UUID groupId,
      @RequestBody MergeRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(merges.merge(groupId, current.id(), request.toCommand()));
  }

  /**
   * Undoes a Merge (R-019-01 … R-019-03); the service view is the contract's {@code Merge}.
   *
   * @param current the signed-in User
   * @param groupId the Group
   * @param mergeId the Merge
   * @param body the Primary contact's Version number read
   * @return 200 with the undone Merge
   */
  @PostMapping("/{mergeId}/undo")
  public ResponseEntity<MergeView> undo(
      @AuthenticationPrincipal CurrentUser current,
      @PathVariable UUID groupId,
      @PathVariable UUID mergeId,
      @RequestBody VersionNumberRequest body) {
    return ResponseEntity.ok(
        merges.undoMerge(groupId, current.id(), mergeId, body.versionNumber()));
  }
}
