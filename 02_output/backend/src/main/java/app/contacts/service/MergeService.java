package app.contacts.service;

import app.contacts.domain.AuditEntry;
import app.contacts.domain.Contact;
import app.contacts.domain.ContactVersion;
import app.contacts.domain.Merge;
import app.contacts.repository.ContactVersionRepository;
import app.contacts.repository.MergeRepository;
import java.time.Clock;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Merges a pair of Possible duplicates into the Primary contact (US-018) and undoes it (US-019).
 */
@Service
public class MergeService {

  private final AccessGuard guard;
  private final ContactService contacts;
  private final MergeRepository merges;
  private final ContactVersionRepository versions;
  private final AuditService audit;
  private final ContactValidator validator;
  private final Clock clock;

  /**
   * The service.
   *
   * @param guard the access check
   * @param contacts locks, reads and changes both Contacts
   * @param merges stores the Merge
   * @param versions reads the Primary contact's Versions for an undo
   * @param audit writes the Audit entry
   * @param validator checks the merged Fields
   * @param clock the clock
   */
  public MergeService(
      AccessGuard guard,
      ContactService contacts,
      MergeRepository merges,
      ContactVersionRepository versions,
      AuditService audit,
      ContactValidator validator,
      Clock clock) {
    this.guard = guard;
    this.contacts = contacts;
    this.merges = merges;
    this.versions = versions;
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

  /**
   * Undoes a Merge: the Primary contact gets the content it had before the Merge, the Other contact
   * becomes active, both get a Version {@code mergeUndone}, and the Merge is marked undone with an
   * Audit entry (R-019-01, BR-10), in one transaction with the Merge and both Contacts locked. The
   * checks run in the order of D-25 (c). The Owner and Editors may do it (BR-03).
   *
   * @param groupId the Group
   * @param userId the signed-in User
   * @param mergeId the Merge
   * @param versionNumber the Primary contact's Version number the client read (BR-06)
   * @return the undone Merge
   * @throws NotFoundException for a non-Member or a Merge that is not the Group's (BR-02)
   * @throws ForbiddenException for a Viewer (BR-03, AC-019-03)
   * @throws ValidationException for a missing Version number
   * @throws ConflictException {@code merge.alreadyUndone} (EX-019-01), {@code merge.restoreFirst}
   *     for a Contact in the recycle bin (R-019-03), {@code contact.merged} for a Primary contact
   *     merged into another, {@code version.conflict} (BR-06), {@code merge.changedAfter} when the
   *     Primary contact has a Version after the Merge (R-019-02)
   */
  @Transactional
  public MergeView undoMerge(UUID groupId, UUID userId, UUID mergeId, Integer versionNumber) {
    guard.require(groupId, userId, Permission.CHANGE_CONTACT);
    if (versionNumber == null) {
      throw new ValidationException(List.of());
    }
    Merge merge = merges.findForUpdate(groupId, mergeId).orElseThrow(NotFoundException::new);
    if (merge.isUndone()) {
      throw new ConflictException("merge.alreadyUndone", Map.of());
    }
    Map<UUID, Contact> pair =
        lockedInIdOrder(
            List.of(merge.getPrimaryId(), merge.getMergedId()), id -> contacts.locked(groupId, id));
    Contact primary = pair.get(merge.getPrimaryId());
    Contact other = pair.get(merge.getMergedId());
    requireUndoable(primary, other);
    contacts.requireCurrent(primary, versionNumber);
    ContactContent beforeMerge = contentBefore(merge, primary);
    contacts.saveMergeUndone(primary, userId, beforeMerge);
    contacts.unmarkMerged(other, userId);
    merge.undo(userId, clock.instant());
    audit.record(groupId, userId, AuditEntry.CONTACTS_MERGE_UNDONE, merge.getId());
    return MergeView.of(merge);
  }

  /**
   * Rejects an undo while a Contact is in the recycle bin (R-019-03) or the Primary contact was
   * merged into another (D-25 (c)).
   */
  private static void requireUndoable(Contact primary, Contact other) {
    if (Contact.IN_RECYCLE_BIN.equals(primary.getState())
        || Contact.IN_RECYCLE_BIN.equals(other.getState())) {
      throw new ConflictException("merge.restoreFirst", Map.of());
    }
    if (!Contact.ACTIVE.equals(primary.getState())) {
      throw new ConflictException("contact.merged", Map.of());
    }
  }

  /**
   * The Primary contact's content before the Merge, if its latest Version is the Merge's {@code
   * merged} Version: no later change (R-019-02) and no later Merge into it. Two Merges at the same
   * time both count as later, so neither undo loses a change.
   */
  private ContactContent contentBefore(Merge merge, Contact primary) {
    UUID groupId = primary.getGroupId();
    int latest = primary.getVersionNumber();
    ContactVersion version = versions.findVersion(groupId, primary.getId(), latest).orElseThrow();
    boolean laterMerge =
        merges.findByGroupIdAndPrimaryIdOrderByMergedAt(groupId, primary.getId()).stream()
            .anyMatch(
                into ->
                    !into.getId().equals(merge.getId())
                        && !into.getMergedAt().isBefore(merge.getMergedAt()));
    if (!ContactVersion.MERGED.equals(version.getKind()) || laterMerge) {
      throw new ConflictException("merge.changedAfter", Map.of());
    }
    return SnapshotView.of(versions.findVersion(groupId, primary.getId(), latest - 1).orElseThrow())
        .content();
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
    Map<UUID, Integer> read =
        Map.of(
            command.primaryId(), command.primaryVersionNumber(),
            command.otherId(), command.otherVersionNumber());
    return lockedInIdOrder(
        read.keySet(), id -> active(contacts.lockedCurrent(groupId, id, read.get(id))));
  }

  /**
   * Each Contact locked in id order, so that two transactions on the same pair run one after the
   * other instead of waiting for each other.
   */
  private static Map<UUID, Contact> lockedInIdOrder(
      Collection<UUID> ids, Function<UUID, Contact> lock) {
    Map<UUID, Contact> locked = new HashMap<>();
    ids.stream().sorted().forEach(id -> locked.put(id, lock.apply(id)));
    return locked;
  }

  private static Contact active(Contact contact) {
    if (!Contact.ACTIVE.equals(contact.getState())) {
      throw new ConflictException("merge.notPossible", Map.of());
    }
    return contact;
  }
}
