package app.contacts.web;

import app.contacts.auth.CurrentUser;
import app.contacts.service.ContactPageView;
import app.contacts.service.ContactQuery;
import app.contacts.service.ContactQueryService;
import app.contacts.service.ContactService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Contacts of a Group ({@code /groups/{groupId}/contacts}, US-006, US-007, US-014). */
@RestController
@RequestMapping("/api/v1/groups/{groupId}/contacts")
public class ContactController {

  private final ContactService contacts;
  private final ContactQueryService contactQueries;

  /**
   * The controller.
   *
   * @param contacts the Contact service
   * @param contactQueries the Contact list
   */
  public ContactController(ContactService contacts, ContactQueryService contactQueries) {
    this.contacts = contacts;
    this.contactQueries = contactQueries;
  }

  /**
   * One Page of the Contact list (R-014-01 … R-014-04); the service views are the contract's {@code
   * ContactPage}.
   *
   * @param current the signed-in User
   * @param groupId the Group
   * @param q the search text
   * @param sort the sort key
   * @param dir the order
   * @param page the Page number
   * @return the Page
   */
  @GetMapping
  public ContactPageView list(
      @AuthenticationPrincipal CurrentUser current,
      @PathVariable UUID groupId,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) String dir,
      @RequestParam(required = false) Integer page) {
    return contactQueries.page(groupId, current.id(), new ContactQuery(q, sort, dir, page));
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

  /**
   * The active Contacts with this e-mail, for the warning of R-006-06.
   *
   * @param current the signed-in User
   * @param groupId the Group
   * @param email the e-mail entered
   * @return the matching Contacts
   */
  @GetMapping("/email-matches")
  public List<ContactRefResponse> emailMatches(
      @AuthenticationPrincipal CurrentUser current,
      @PathVariable UUID groupId,
      @RequestParam String email) {
    return contacts.findByEmail(groupId, current.id(), email).stream()
        .map(match -> new ContactRefResponse(match.id(), match.name()))
        .toList();
  }

  /**
   * One Contact of the Group, for its details (R-014-06).
   *
   * @param current the signed-in User
   * @param groupId the Group
   * @param contactId the Contact
   * @return the Contact
   */
  @GetMapping("/{contactId}")
  public ContactResponse get(
      @AuthenticationPrincipal CurrentUser current,
      @PathVariable UUID groupId,
      @PathVariable UUID contactId) {
    return ContactResponse.of(contacts.get(groupId, current.id(), contactId));
  }

  /**
   * Saves edited Fields of a Contact as one Version {@code edited} (R-007-01).
   *
   * @param current the signed-in User
   * @param groupId the Group
   * @param contactId the Contact
   * @param body the Fields and the Version number read
   * @return the saved Contact
   */
  @PutMapping("/{contactId}")
  public ContactResponse update(
      @AuthenticationPrincipal CurrentUser current,
      @PathVariable UUID groupId,
      @PathVariable UUID contactId,
      @RequestBody ContactUpdateRequest body) {
    return ContactResponse.of(
        contacts.update(groupId, current.id(), contactId, body.versionNumber(), body.fields()));
  }
}
