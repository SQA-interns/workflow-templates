package app.contacts.service;

import app.contacts.domain.AuditEntry;
import app.contacts.domain.Contact;
import app.contacts.repository.ContactRepository;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Permanently deletes Contacts from the recycle bin, by the Owner (US-013) or after the recycle bin
 * period (BR-08, ADR-10), with every Contact merged into them (D-21), each with an Audit entry
 * (SR-06, BR-10).
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
    for (Contact merged :
        mergedInto(contactId, id -> contacts.findByGroupIdAndMergedInto(groupId, id))) {
      permanentlyDeleteOne(groupId, userId, AuditEntry.CONTACT_PERMANENTLY_DELETED, merged.getId());
    }
    permanentlyDeleteOne(groupId, userId, AuditEntry.CONTACT_PERMANENTLY_DELETED, contactId);
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
    List<Contact> due = contacts.findDueForRemoval(groupId, cutoff);
    // One read of the merged Contacts instead of one query per Contact (NFR-05): each query would
    // flush and dirty-check every Contact read above.
    Map<UUID, List<Contact>> mergedByPrimary =
        contacts.findByGroupIdAndState(groupId, Contact.MERGED).stream()
            .collect(Collectors.groupingBy(Contact::getMergedInto));
    for (Contact contact : due) {
      UUID movedBy = contact.getUpdatedBy();
      for (Contact merged :
          mergedInto(contact.getId(), id -> mergedByPrimary.getOrDefault(id, List.of()))) {
        permanentlyDeleteOne(groupId, movedBy, AuditEntry.CONTACT_REMOVED, merged.getId());
      }
      audit.record(groupId, movedBy, AuditEntry.CONTACT_REMOVED, contact.getId());
    }
    return contacts.permanentlyDeleteDue(groupId, cutoff);
  }

  /** Deletes one Contact of the Group with an Audit entry. */
  private void permanentlyDeleteOne(UUID groupId, UUID userId, String action, UUID contactId) {
    contacts.permanentlyDelete(groupId, contactId);
    audit.record(groupId, userId, action, contactId);
  }

  /**
   * The Contacts merged into the Contact, directly or into one merged into it, deepest first so
   * that none is deleted before a Contact merged into it (D-21).
   *
   * @param contactId the Primary contact
   * @param mergedIntoOne the Contacts merged directly into a Contact
   */
  private static List<Contact> mergedInto(
      UUID contactId, Function<UUID, List<Contact>> mergedIntoOne) {
    List<Contact> found = new ArrayList<>();
    Deque<UUID> open = new ArrayDeque<>(List.of(contactId));
    while (!open.isEmpty()) {
      for (Contact merged : mergedIntoOne.apply(open.pop())) {
        found.add(merged);
        open.push(merged.getId());
      }
    }
    return found.reversed();
  }
}
