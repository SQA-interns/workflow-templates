package app.contacts.web;

import app.contacts.auth.CurrentUser;
import app.contacts.service.PermanentDeleteService;
import app.contacts.service.RecycleBinEntryView;
import app.contacts.service.RecycleBinService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The recycle bin of a Group's Contacts: move a Contact there, list and restore (US-012), delete
 * permanently (US-013).
 */
@RestController
@RequestMapping("/api/v1/groups/{groupId}")
public class RecycleBinController {

  private final RecycleBinService recycleBin;
  private final PermanentDeleteService permanentDelete;

  /**
   * The controller.
   *
   * @param recycleBin the recycle bin service
   * @param permanentDelete the permanent delete service
   */
  public RecycleBinController(
      RecycleBinService recycleBin, PermanentDeleteService permanentDelete) {
    this.recycleBin = recycleBin;
    this.permanentDelete = permanentDelete;
  }

  /**
   * Moves an active Contact to the recycle bin as a new Version (R-012-01, BR-06).
   *
   * @param current the signed-in User
   * @param groupId the Group
   * @param contactId the Contact
   * @param body the current Version number read
   * @return the Contact in the recycle bin
   */
  @PostMapping("/contacts/{contactId}/move-to-recycle-bin")
  public ContactResponse move(
      @AuthenticationPrincipal CurrentUser current,
      @PathVariable UUID groupId,
      @PathVariable UUID contactId,
      @RequestBody VersionNumberRequest body) {
    return ContactResponse.of(
        recycleBin.move(groupId, current.id(), contactId, body.versionNumber()));
  }

  /**
   * The Contacts in the recycle bin, newest first (R-012-02); the service views are the contract's
   * {@code RecycleBinEntry}.
   *
   * @param current the signed-in User
   * @param groupId the Group
   * @return the entries
   */
  @GetMapping("/recycle-bin")
  public List<RecycleBinEntryView> list(
      @AuthenticationPrincipal CurrentUser current, @PathVariable UUID groupId) {
    return recycleBin.list(groupId, current.id());
  }

  /**
   * Restores a Contact from the recycle bin before its removal date (R-012-03, R-012-04, BR-06).
   *
   * @param current the signed-in User
   * @param groupId the Group
   * @param contactId the Contact
   * @param body the current Version number read
   * @return the active Contact
   */
  @PostMapping("/recycle-bin/{contactId}/restore")
  public ContactResponse restore(
      @AuthenticationPrincipal CurrentUser current,
      @PathVariable UUID groupId,
      @PathVariable UUID contactId,
      @RequestBody VersionNumberRequest body) {
    return ContactResponse.of(
        recycleBin.restore(groupId, current.id(), contactId, body.versionNumber()));
  }

  /**
   * Permanently deletes a Contact in the recycle bin with its Versions (R-013-01 … R-013-03).
   *
   * @param current the signed-in User
   * @param groupId the Group
   * @param contactId the Contact
   */
  @DeleteMapping("/recycle-bin/{contactId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void permanentlyDelete(
      @AuthenticationPrincipal CurrentUser current,
      @PathVariable UUID groupId,
      @PathVariable UUID contactId) {
    permanentDelete.permanentlyDelete(groupId, current.id(), contactId);
  }
}
