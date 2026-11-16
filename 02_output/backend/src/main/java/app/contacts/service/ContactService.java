package app.contacts.service;

import app.contacts.domain.Attribute;
import app.contacts.domain.Contact;
import app.contacts.domain.ContactFields;
import app.contacts.domain.ContactVersion;
import app.contacts.domain.Tag;
import app.contacts.domain.User;
import app.contacts.repository.AttributeRepository;
import app.contacts.repository.ContactQueryRepository;
import app.contacts.repository.ContactRepository;
import app.contacts.repository.UserRepository;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adds Contacts with their Attributes and Tags to a Group, edits, reads and finds them by e-mail
 * (US-006, US-007, US-008, US-009, US-014).
 */
@Service
public class ContactService {

  private final ContactRepository contacts;
  private final AttributeRepository attributes;
  private final TagService tags;
  private final ContactQueryRepository contactQueries;
  private final UserRepository users;
  private final VersionWriter versionWriter;
  private final AccessGuard guard;
  private final ContactValidator validator;
  private final Normaliser normaliser;
  private final Clock clock;

  /**
   * The service.
   *
   * @param contacts the Contact repository
   * @param attributes stores the Attributes of a save (R-008-01)
   * @param tags finds or creates the Tags of a save and puts them on (R-009-01)
   * @param contactQueries reads Attributes and Tags for the details
   * @param users names the User of the current Version (BR-06)
   * @param versionWriter writes the Version of every change
   * @param guard the access check
   * @param validator checks the Fields
   * @param normaliser derives the search text and sort name
   * @param clock the clock
   */
  public ContactService(
      ContactRepository contacts,
      AttributeRepository attributes,
      TagService tags,
      ContactQueryRepository contactQueries,
      UserRepository users,
      VersionWriter versionWriter,
      AccessGuard guard,
      ContactValidator validator,
      Normaliser normaliser,
      Clock clock) {
    this.contacts = contacts;
    this.attributes = attributes;
    this.tags = tags;
    this.contactQueries = contactQueries;
    this.users = users;
    this.versionWriter = versionWriter;
    this.guard = guard;
    this.validator = validator;
    this.normaliser = normaliser;
    this.clock = clock;
  }

  /**
   * Adds a Contact with its Attributes and Tags to the Group with Version 1 {@code created}
   * (R-006-01, R-008-01, R-009-01).
   *
   * @param groupId the Group
   * @param userId the signed-in User
   * @param fields Field name to value as sent; a missing or {@code null} value is an empty Field
   * @param sentAttributes the Attributes as sent; {@code null} for none
   * @param sentTags the Tag names as sent; {@code null} for none
   * @return the new Contact
   * @throws NotFoundException for a non-Member (BR-02, EX-006-09)
   * @throws ForbiddenException for a Viewer (BR-03, AC-006-06)
   * @throws ValidationException for an unknown Field name, invalid Fields, Attributes or Tag names
   *     (R-006-02 … R-006-05, R-008-02 … R-008-05, R-009-05)
   */
  @Transactional
  public ContactView create(
      UUID groupId,
      UUID userId,
      Map<String, String> fields,
      List<AttributeView> sentAttributes,
      List<String> sentTags) {
    guard.require(groupId, userId, Permission.CHANGE_CONTACT);
    ContactFields checked = checked(fields);
    List<AttributeView> checkedAttributes =
        sentAttributes == null ? List.of() : validator.checkAttributes(sentAttributes);
    List<String> checkedTags = sentTags == null ? List.of() : validator.checkTags(sentTags);
    Contact contact =
        Contact.create(
            groupId,
            checked,
            normaliser.searchText(checked),
            normaliser.sortName(checked),
            userId,
            clock.instant());
    contacts.add(groupId, contact);
    List<Tag> wanted = tags.resolve(groupId, checkedTags);
    ContactContent content = new ContactContent(checked, checkedAttributes, names(wanted));
    store(contact, content.attributes());
    tags.retag(contact, List.of(), wanted);
    versionWriter.record(contact, ContactVersion.CREATED, ContactContent.EMPTY, content);
    return view(contact);
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
   * Saves edited Fields, Attributes and Tags of a Contact as one Version {@code edited} (R-007-01,
   * R-008-01, R-009-01), in one transaction with the Contact locked (specification §4).
   *
   * @param groupId the Group
   * @param userId the signed-in User
   * @param contactId the Contact
   * @param versionNumber the Version number the client read (BR-06)
   * @param fields Field name to value as sent; a missing or {@code null} value is an empty Field
   * @param sentAttributes the Attributes as sent; {@code null} keeps the stored ones
   * @param sentTags the Tag names as sent; {@code null} keeps the Tags the Contact has
   * @return the saved Contact
   * @throws NotFoundException for a non-Member or a Contact that is not the Group's (BR-02)
   * @throws ForbiddenException for a Viewer (BR-03, AC-007-05)
   * @throws ValidationException for a missing Version number, an unknown Field name, invalid
   *     Fields, Attributes or Tag names (R-007-02, R-008-02 … R-008-05, R-009-05)
   * @throws ConflictException {@code version.conflict} for a Version number that is not current
   *     (BR-06), {@code contact.inRecycleBin} or {@code contact.merged} for a Contact that is not
   *     active (EX-007-03, D-23), {@code contact.noChanges} when no Field, Attribute or Tag changed
   *     (R-007-03)
   */
  @Transactional
  public ContactView update(
      UUID groupId,
      UUID userId,
      UUID contactId,
      Integer versionNumber,
      Map<String, String> fields,
      List<AttributeView> sentAttributes,
      List<String> sentTags) {
    guard.require(groupId, userId, Permission.CHANGE_CONTACT);
    final ContactFields edited = checked(fields);
    final List<AttributeView> editedAttributes =
        sentAttributes == null ? null : validator.checkAttributes(sentAttributes);
    final List<String> editedTags = sentTags == null ? null : validator.checkTags(sentTags);
    if (versionNumber == null) {
      throw new ValidationException(List.of());
    }
    Contact contact =
        contacts.findForUpdate(groupId, contactId).orElseThrow(NotFoundException::new);
    requireCurrent(contact, versionNumber);
    requireActive(contact);
    List<TagView> stored = tagsOf(contact);
    ContactContent before =
        new ContactContent(
            contact.getFields(),
            attributesOf(contact),
            stored.stream().map(TagView::name).toList());
    List<Tag> wanted = editedTags == null ? null : tags.resolve(groupId, editedTags);
    ContactContent after =
        new ContactContent(
            edited,
            editedAttributes == null ? before.attributes() : editedAttributes,
            wanted == null ? before.tags() : names(wanted));
    if (after.equals(before)) {
      throw new ConflictException("contact.noChanges", Map.of());
    }
    contact.edit(
        edited,
        normaliser.searchText(edited),
        normaliser.sortName(edited),
        userId,
        clock.instant());
    if (!after.attributes().equals(before.attributes())) {
      attributes.discardOfContact(groupId, contactId);
      store(contact, after.attributes());
    }
    if (wanted != null) {
      tags.retag(contact, stored.stream().map(TagView::id).toList(), wanted);
    }
    versionWriter.record(contact, ContactVersion.EDITED, before, after);
    return view(contact);
  }

  /** Rejects a change sent with a Version number that is not the current one (BR-06). */
  private void requireCurrent(Contact contact, int versionNumber) {
    if (contact.getVersionNumber() != versionNumber) {
      String changedBy =
          users.findById(contact.getUpdatedBy()).map(User::getDisplayName).orElseThrow();
      throw new ConflictException("version.conflict", Map.of("changedBy", changedBy));
    }
  }

  /** Rejects a change of a Contact in the recycle bin (EX-007-03) or merged into another (D-23). */
  private static void requireActive(Contact contact) {
    switch (contact.getState()) {
      case Contact.ACTIVE -> {}
      case Contact.IN_RECYCLE_BIN -> throw new ConflictException("contact.inRecycleBin", Map.of());
      default -> throw new ConflictException("contact.merged", Map.of());
    }
  }

  /** The Fields as sent, trimmed and checked (R-006-02 … R-006-07). */
  private ContactFields checked(Map<String, String> fields) {
    if (fields == null || !ContactFields.NAMES.containsAll(fields.keySet())) {
      throw new ValidationException(List.of());
    }
    return validator.check(ContactFields.ofNames(fields));
  }

  /** Stores the checked Attributes of the Contact (R-008-01). */
  private void store(Contact contact, List<AttributeView> checked) {
    checked.forEach(
        attribute ->
            attributes.add(
                contact.getGroupId(), Attribute.of(contact, attribute.key(), attribute.value())));
  }

  /** The stored Attributes of the Contact, by key A–Z (R-008-06). */
  private List<AttributeView> attributesOf(Contact contact) {
    return contactQueries.findAttributes(contact.getGroupId(), contact.getId()).stream()
        .map(row -> new AttributeView(row.key(), row.value()))
        .toList();
  }

  /** The Tags the Contact has, by name A–Z. */
  private List<TagView> tagsOf(Contact contact) {
    return contactQueries.findTags(contact.getGroupId(), List.of(contact.getId())).stream()
        .map(row -> new TagView(row.tagId(), row.name()))
        .toList();
  }

  private static List<String> names(List<Tag> tags) {
    return tags.stream().map(Tag::getName).toList();
  }

  /** The Contact with its Attributes and Tags. */
  private ContactView view(Contact contact) {
    return ContactView.of(contact, attributesOf(contact), tagsOf(contact));
  }
}
