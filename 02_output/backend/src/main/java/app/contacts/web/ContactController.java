package app.contacts.web;

import app.contacts.auth.CurrentUser;
import app.contacts.service.ContactService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Contacts of a Group ({@code /groups/{groupId}/contacts}, US-006). */
@RestController
@RequestMapping("/api/v1/groups/{groupId}/contacts")
public class ContactController {

  private final ContactService contacts;

  /**
   * The controller.
   *
   * @param contacts the Contact service
   */
  public ContactController(ContactService contacts) {
    this.contacts = contacts;
  }

  /**
   * Adds a Contact with Version 1 {@code created} (R-006-01).
   *
   * @param current the signed-in User
   * @param groupId the Group
   * @param body the Fields
   * @return 201 with the new Contact
   */
  @PostMapping
  public ResponseEntity<ContactResponse> create(
      @AuthenticationPrincipal CurrentUser current,
      @PathVariable UUID groupId,
      @RequestBody ContactWriteRequest body) {
    ContactResponse created =
        ContactResponse.of(contacts.create(groupId, current.id(), body.fields()));
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }
}
