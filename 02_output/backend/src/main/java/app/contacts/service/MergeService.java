package app.contacts.service;

import app.contacts.domain.AuditEntry;
import app.contacts.domain.Contact;
import app.contacts.domain.Merge;
import app.contacts.repository.MergeRepository;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Merges a pair of Possible duplicates into the Primary contact (US-018). */
@Service
public class MergeService {

  private final AccessGuard guard;
  private final ContactService contacts;
  private final MergeRepository merges;
  private final AuditService audit;
  private final ContactValidator validator;
  private final Clock clock;

  /**
   * The service.
   *
   * @param guard the access check
   * @param contacts locks, reads and changes both Contacts
   * @param merges stores the Merge
   * @param audit writes the Audit entry
   * @param validator checks the merged Fields
   * @param clock the clock
   */
  public MergeService(
      AccessGuard guard,
      ContactService contacts,
      MergeRepository merges,
      AuditService audit,
      ContactValidator validator,
      Clock clock) {
    this.guard = guard;
    this.contacts = contacts;
    this.merges = merges;
    this.audit = audit;
    this.validator = validator;
    this.clock = clock;
  }

  /**
   * Merges the Other contact into the Primary contact: the Primary contact gets the chosen Field
   * values and the Attributes and Tags of both (R-018-02), the Other contact becomes {@code merged}
   * (R-018-04); both get a Version {@code merged}, and the Merge and an Audit entry are written
   * (BR-10), in one transaction with both Contacts locked. The Owner and Editors may do it (BR-03).
   *
   * @param groupId the Group
   * @param userId the signed-in User
   * @param command the Merge as confirmed
   * @return the Merge
   * @throws NotFoundException for a non-Member or a Contact that is not the Group's (BR-02)
   * @throws ForbiddenException for a Viewer (BR-03, AC-018-04)
   * @throws ValidationException for a missing id or Version number, an unknown choice, or merged
   *     Fields that are invalid together
   * @throws InvalidRequestException {@code merge.notPossible} for a Contact merged with itself
   *     (EX-018-05), {@code merge.choiceRequired} (R-018-03)
   * @throws ConflictException {@code version.conflict} (BR-06, EX-018-04), {@code
   *     merge.notPossible} for a Contact that is not active (EX-018-03), {@code
   *     merge.tooManyAttributes} (R-018-06)
   */
  @Transactional
  public MergeView merge(UUID groupId, UUID userId, MergeCommand command) {
    guard.require(groupId, userId, Permission.CHANGE_CONTACT);
    requireComplete(command);
    if (command.primaryId().equals(command.otherId())) {
      throw new InvalidRequestException("merge.notPossible", Map.of());
    }
    Map<UUID, Contact> pair = lockedPair(groupId, command);
    Contact primary = pair.get(command.primaryId());
    Contact other = pair.get(command.otherId());
    ContactContent combined =
        MergedContent.of(
            contacts.contentOf(primary),
            contacts.contentOf(other),
            command.fieldChoices(),
            command.attributeChoices());
    ContactContent merged =
        new ContactContent(
            validator.check(combined.fields()), combined.attributes(), combined.tags());
    contacts.saveMerged(primary, userId, merged);
    contacts.markMerged(other, primary.getId(), userId);
    Merge merge = merges.add(groupId, Merge.create(primary, other, userId, clock.instant()));
    audit.record(groupId, userId, AuditEntry.CONTACTS_MERGED, merge.getId());
    return MergeView.of(merge);
  }

  private static void requireComplete(MergeCommand command) {
    if (command.primaryId() == null
        || command.otherId() == null
        || command.primaryVersionNumber() == null
        || command.otherVersionNumber() == null) {
      throw new ValidationException(List.of());
    }
  }

  /**
   * The Contacts of the command by id, locked in id order so that two Merges of one pair run one
   * after the other, each current (BR-06) and active (R-018-07).
   */
  private Map<UUID, Contact> lockedPair(UUID groupId, MergeCommand command) {
    SortedMap<UUID, Integer> read = new TreeMap<>();
    read.put(command.primaryId(), command.primaryVersionNumber());
    read.put(command.otherId(), command.otherVersionNumber());
    Map<UUID, Contact> locked = new HashMap<>();
    read.forEach(
        (id, versionNumber) ->
            locked.put(id, active(contacts.lockedCurrent(groupId, id, versionNumber))));
    return locked;
  }

  private static Contact active(Contact contact) {
    if (!Contact.ACTIVE.equals(contact.getState())) {
      throw new ConflictException("merge.notPossible", Map.of());
    }
    return contact;
  }
}
