package app.contacts.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The recycle bin of a Group's Contacts: move, list and restore (US-012, ADR-10). */
@Service
public class RecycleBinService {

  private final AccessGuard guard;
  private final ContactService contacts;

  /**
   * The service.
   *
   * @param guard the access check
   * @param contacts changes the state of the Contact
   */
  public RecycleBinService(AccessGuard guard, ContactService contacts) {
    this.guard = guard;
    this.contacts = contacts;
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
    guard.require(groupId, userId, Permission.CHANGE_CONTACT);
    if (versionNumber == null) {
      throw new ValidationException(List.of());
    }
    return contacts.moveToRecycleBin(groupId, userId, contactId, versionNumber);
  }
}
