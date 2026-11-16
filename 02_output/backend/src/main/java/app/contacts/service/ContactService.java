package app.contacts.service;

import app.contacts.domain.Contact;
import app.contacts.domain.ContactFields;
import app.contacts.domain.ContactVersion;
import app.contacts.repository.ContactRepository;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Adds Contacts to a Group (US-006). */
@Service
public class ContactService {

  private final ContactRepository contacts;
  private final VersionWriter versionWriter;
  private final AccessGuard guard;
  private final ContactValidator validator;
  private final Normaliser normaliser;
  private final Clock clock;

  /**
   * The service.
   *
   * @param contacts the Contact repository
   * @param versionWriter writes the Version of every change
   * @param guard the access check
   * @param validator checks the Fields
   * @param normaliser derives the search text and sort name
   * @param clock the clock
   */
  public ContactService(
      ContactRepository contacts,
      VersionWriter versionWriter,
      AccessGuard guard,
      ContactValidator validator,
      Normaliser normaliser,
      Clock clock) {
    this.contacts = contacts;
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
    if (fields == null || !ContactFields.NAMES.containsAll(fields.keySet())) {
      throw new ValidationException(List.of());
    }
    ContactFields checked = validator.check(ContactFields.ofNames(fields));
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
    return ContactView.of(contact);
  }
}
