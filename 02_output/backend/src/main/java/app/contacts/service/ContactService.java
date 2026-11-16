package app.contacts.service;

import app.contacts.domain.Contact;
import app.contacts.domain.ContactFields;
import app.contacts.domain.ContactVersion;
import app.contacts.repository.ContactQueryRepository;
import app.contacts.repository.ContactRepository;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Adds Contacts to a Group, edits, reads and finds them by e-mail (US-006, US-007, US-014). */
@Service
public class ContactService {

  private final ContactRepository contacts;
  private final ContactQueryRepository contactQueries;
  private final VersionWriter versionWriter;
  private final AccessGuard guard;
  private final ContactValidator validator;
  private final Normaliser normaliser;
  private final Clock clock;

  /**
   * The service.
   *
   * @param contacts the Contact repository
   * @param contactQueries reads Attributes and Tags for the details
   * @param versionWriter writes the Version of every change
   * @param guard the access check
   * @param validator checks the Fields
   * @param normaliser derives the search text and sort name
   * @param clock the clock
   */
  public ContactService(
      ContactRepository contacts,
      ContactQueryRepository contactQueries,
      VersionWriter versionWriter,
      AccessGuard guard,
      ContactValidator validator,
      Normaliser normaliser,
      Clock clock) {
    this.contacts = contacts;
    this.contactQueries = contactQueries;
    this.versionWriter = versionWriter;
    this.guard = guard;
    this.validator = validator;
    this.normaliser = normaliser;
    this.clock = clock;
  }

  /**
   * Adds a Contact to the Group with Version 1 {@code created} (R-006-01).
   *
   * @param groupId the Group
   * @param userId the signed-in User
   * @param fields Field name to value as sent; a missing or {@code null} value is an empty Field
   * @return the new Contact
   * @throws NotFoundException for a non-Member (BR-02, EX-006-09)
   * @throws ForbiddenException for a Viewer (BR-03, AC-006-06)
   * @throws ValidationException for an unknown Field name or invalid Fields (R-006-02 … R-006-05)
   */
  @Transactional
  public ContactView create(UUID groupId, UUID userId, Map<String, String> fields) {
    guard.require(groupId, userId, Permission.CHANGE_CONTACT);
    ContactFields checked = checked(fields);
    Contact contact =
        Contact.create(
            groupId,
            checked,
            normaliser.searchText(checked),
            normaliser.sortName(checked),
            userId,
            clock.instant());
    contacts.add(groupId, contact);
    versionWriter.record(contact, ContactVersion.CREATED, ContactFields.EMPTY);
    return ContactView.of(contact, List.of(), List.of());
  }

  /**
   * The active Contacts of the Group with this e-mail, for the warning of R-006-06; any Member may
   * ask.
   *
   * @param groupId the Group
   * @param userId the signed-in User
   * @param email the e-mail, compared trimmed and case-insensitively (BR-11)
   * @return the matching Contacts, oldest first; none for a blank e-mail
   * @throws NotFoundException for a non-Member (BR-02)
   */
  @Transactional(readOnly = true)
  public List<ContactRefView> findByEmail(UUID groupId, UUID userId, String email) {
    guard.require(groupId, userId, Permission.READ);
    String trimmed = email == null ? "" : email.strip();
    if (trimmed.isEmpty()) {
      return List.of();
    }
    return contacts.findActiveByEmail(groupId, trimmed).stream()
        .map(contact -> new ContactRefView(contact.getId(), contact.getFields().name()))
        .toList();
  }

  /**
   * One Contact of the Group with its Attributes and Tags, for its details (R-014-06); any Member
   * may read it.
   *
   * @param groupId the Group
   * @param userId the signed-in User
   * @param contactId the Contact
   * @return the Contact
   * @throws NotFoundException for a non-Member or a Contact that is not the Group's (BR-02)
   */
  @Transactional(readOnly = true)
  public ContactView get(UUID groupId, UUID userId, UUID contactId) {
    guard.require(groupId, userId, Permission.READ);
    Contact contact =
        contacts.findByGroupIdAndId(groupId, contactId).orElseThrow(NotFoundException::new);
    return view(contact);
  }

  /**
   * Saves edited Fields of a Contact as one Version {@code edited} (R-007-01), in one transaction
   * with the Contact locked (specification §4).
   *
   * @param groupId the Group
   * @param userId the signed-in User
   * @param contactId the Contact
   * @param versionNumber the Version number the client read (BR-06)
   * @param fields Field name to value as sent; a missing or {@code null} value is an empty Field
   * @return the saved Contact
   * @throws NotFoundException for a non-Member or a Contact that is not the Group's (BR-02)
   * @throws ForbiddenException for a Viewer (BR-03, AC-007-05)
   * @throws ValidationException for a missing Version number, an unknown Field name or invalid
   *     Fields (R-007-02)
   * @throws ConflictException {@code contact.noChanges} when no Field changed (R-007-03)
   */
  @Transactional
  public ContactView update(
      UUID groupId,
      UUID userId,
      UUID contactId,
      Integer versionNumber,
      Map<String, String> fields) {
    guard.require(groupId, userId, Permission.CHANGE_CONTACT);
    ContactFields edited = checked(fields);
    if (versionNumber == null) {
      throw new ValidationException(List.of());
    }
    Contact contact =
        contacts.findForUpdate(groupId, contactId).orElseThrow(NotFoundException::new);
    ContactFields before = contact.getFields();
    if (edited.equals(before)) {
      throw new ConflictException("contact.noChanges", Map.of());
    }
    contact.edit(
        edited,
        normaliser.searchText(edited),
        normaliser.sortName(edited),
        userId,
        clock.instant());
    versionWriter.record(contact, ContactVersion.EDITED, before);
    return view(contact);
  }

  /** The Fields as sent, trimmed and checked (R-006-02 … R-006-07). */
  private ContactFields checked(Map<String, String> fields) {
    if (fields == null || !ContactFields.NAMES.containsAll(fields.keySet())) {
      throw new ValidationException(List.of());
    }
    return validator.check(ContactFields.ofNames(fields));
  }

  /** The Contact with its Attributes and Tags. */
  private ContactView view(Contact contact) {
    UUID groupId = contact.getGroupId();
    UUID contactId = contact.getId();
    List<AttributeView> attributes =
        contactQueries.findAttributes(groupId, contactId).stream()
            .map(row -> new AttributeView(row.key(), row.value()))
            .toList();
    List<TagView> tags =
        contactQueries.findTags(groupId, List.of(contactId)).stream()
            .map(row -> new TagView(row.tagId(), row.name()))
            .toList();
    return ContactView.of(contact, attributes, tags);
  }
}
