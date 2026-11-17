package app.contacts.web;

import app.contacts.auth.CurrentUser;
import app.contacts.service.RecycleBinService;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The recycle bin of a Group's Contacts (US-012): move a Contact there, list and restore. */
@RestController
@RequestMapping("/api/v1/groups/{groupId}")
public class RecycleBinController {

  private final RecycleBinService recycleBin;

  /**
   * The controller.
   *
   * @param recycleBin the recycle bin service
   */
  public RecycleBinController(RecycleBinService recycleBin) {
    this.recycleBin = recycleBin;
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
}
