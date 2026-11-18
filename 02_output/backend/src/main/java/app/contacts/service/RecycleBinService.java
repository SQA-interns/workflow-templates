package app.contacts.service;

import app.contacts.domain.Contact;
import app.contacts.domain.RecycleBin;
import app.contacts.repository.ContactRepository;
import app.contacts.repository.UserRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The recycle bin of a Group's Contacts: move, list and restore (US-012, ADR-10). */
@Service
public class RecycleBinService {

  private final AccessGuard guard;
  private final ContactService contacts;
  private final ContactRepository contactRepository;
  private final UserRepository users;

  /**
   * The service.
   *
   * @param guard the access check
   * @param contacts changes the state of the Contact
   * @param contactRepository reads the Contacts in the recycle bin
   * @param users names the User who moved each Contact
   */
  public RecycleBinService(
      AccessGuard guard,
      ContactService contacts,
      ContactRepository contactRepository,
      UserRepository users) {
    this.guard = guard;
    this.contacts = contacts;
    this.contactRepository = contactRepository;
    this.users = users;
  }

  /**
   * The Group's Contacts in the recycle bin, newest first, with who moved them, when, and their
   * removal date (R-012-02); the Owner and Editors may see it.
   *
   * @param groupId the Group
   * @param userId the signed-in User
   * @return the entries
   * @throws NotFoundException for a non-Member (BR-02)
   * @throws ForbiddenException for a Viewer (AC-012-05)
   */
  @Transactional(readOnly = true)
  public List<RecycleBinEntryView> list(UUID groupId, UUID userId) {
    guard.require(groupId, userId, Permission.VIEW_RECYCLE_BIN);
    List<Contact> found = contactRepository.findInRecycleBin(groupId);
    Map<UUID, String> names = namesOf(found);
    return found.stream()
        .map(
            contact ->
                new RecycleBinEntryView(
                    new ContactRefView(contact.getId(), contact.getFields().name()),
                    contact.getVersionNumber(),
                    names.get(contact.getUpdatedBy()),
                    contact.getDeletedAt(),
                    RecycleBin.removalAt(contact.getDeletedAt())))
        .toList();
  }

  /**
   * Moves an active Contact to the recycle bin (R-012-01); the Owner and Editors may do it (BR-03,
   * D-15).
   *
   * @param groupId the Group
   * @param userId the signed-in User
   * @param contactId the Contact
   * @param versionNumber the current Version number the client read (BR-06)
   * @return the Contact in the recycle bin
   * @throws NotFoundException for a non-Member or a Contact that is not the Group's (BR-02)
   * @throws ForbiddenException for a Viewer (BR-03, AC-012-04)
   * @throws ValidationException for a missing Version number
   * @throws ConflictException as {@link ContactService#moveToRecycleBin}
   */
  @Transactional
  public ContactView move(UUID groupId, UUID userId, UUID contactId, Integer versionNumber) {
    requireChange(groupId, userId, versionNumber);
    return contacts.moveToRecycleBin(groupId, userId, contactId, versionNumber);
  }

  /**
   * Restores a Contact from the recycle bin before its removal date (R-012-03, R-012-04); the Owner
   * and Editors may do it (BR-03, D-15).
   *
   * @param groupId the Group
   * @param userId the signed-in User
   * @param contactId the Contact
   * @param versionNumber the current Version number the client read (BR-06)
   * @return the active Contact
   * @throws NotFoundException for a non-Member, a Contact that is not the Group's (BR-02) or not in
   *     the recycle bin
   * @throws ForbiddenException for a Viewer (BR-03)
   * @throws ValidationException for a missing Version number
   * @throws ConflictException as {@link ContactService#restoreFromRecycleBin}
   */
  @Transactional
  public ContactView restore(UUID groupId, UUID userId, UUID contactId, Integer versionNumber) {
    requireChange(groupId, userId, versionNumber);
    return contacts.restoreFromRecycleBin(groupId, userId, contactId, versionNumber);
  }

  /** Checks the permission to change (BR-03), then that the Version number read was sent. */
  private void requireChange(UUID groupId, UUID userId, Integer versionNumber) {
    guard.require(groupId, userId, Permission.CHANGE_CONTACT);
    if (versionNumber == null) {
      throw new ValidationException(List.of());
    }
  }

  /** The display name of the User who moved each Contact: the User of its current Version. */
  private Map<UUID, String> namesOf(List<Contact> found) {
    Map<UUID, String> names = new HashMap<>();
    users
        .findAllById(found.stream().map(Contact::getUpdatedBy).distinct().toList())
        .forEach(user -> names.put(user.getId(), user.shownName()));
    return names;
  }
}
