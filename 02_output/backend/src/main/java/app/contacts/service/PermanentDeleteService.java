package app.contacts.service;

import app.contacts.domain.AuditEntry;
import app.contacts.domain.Contact;
import app.contacts.repository.ContactRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Permanently deletes Contacts from the recycle bin, by the Owner (US-013) or after the recycle bin
 * period (BR-08, ADR-10), each with an Audit entry (SR-06, BR-10).
 */
@Service
public class PermanentDeleteService {

  private final AccessGuard guard;
  private final ContactRepository contacts;
  private final AuditService audit;

  /**
   * The service.
   *
   * @param guard the access check
   * @param contacts reads and deletes the Contacts
   * @param audit writes the Audit entries
   */
  public PermanentDeleteService(AccessGuard guard, ContactRepository contacts, AuditService audit) {
    this.guard = guard;
    this.contacts = contacts;
    this.audit = audit;
  }

  /**
   * Deletes a Contact in the recycle bin with all its Versions, Attributes and Tag assignments, and
   * writes an Audit entry with ids only (R-013-01, R-013-02); only the Owner may do it (BR-04).
   *
   * @param groupId the Group
   * @param userId the signed-in User
   * @param contactId the Contact
   * @throws NotFoundException for a non-Member or a Contact that is not the Group's (BR-02,
   *     EX-013-04)
   * @throws ForbiddenException for an Editor or a Viewer (BR-04, AC-013-02)
   * @throws ConflictException {@code contact.notInRecycleBin} for a Contact that is not in the
   *     recycle bin (R-013-03)
   */
  @Transactional
  public void permanentlyDelete(UUID groupId, UUID userId, UUID contactId) {
    guard.require(groupId, userId, Permission.PERMANENTLY_DELETE);
    Contact contact =
        contacts.findForUpdate(groupId, contactId).orElseThrow(NotFoundException::new);
    if (!Contact.IN_RECYCLE_BIN.equals(contact.getState())) {
      throw new ConflictException("contact.notInRecycleBin", Map.of());
    }
    contacts.permanentlyDelete(groupId, contactId);
    audit.record(groupId, userId, AuditEntry.CONTACT_PERMANENTLY_DELETED, contactId);
  }

  /**
   * Deletes the Group's Contacts moved to the recycle bin at or before the cutoff, in one
   * transaction, with an Audit entry each by the User who moved it (BR-08, ADR-10).
   *
   * @param groupId the Group
   * @param cutoff now minus the recycle bin period
   * @return how many Contacts were deleted
   */
  @Transactional
  public int permanentlyDeleteDue(UUID groupId, Instant cutoff) {
    contacts
        .findDueForRemoval(groupId, cutoff)
        .forEach(
            contact ->
                audit.record(
                    groupId, contact.getUpdatedBy(), AuditEntry.CONTACT_REMOVED, contact.getId()));
    return contacts.permanentlyDeleteDue(groupId, cutoff);
  }
}
