package app.contacts.repository;

import app.contacts.domain.Contact;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Contacts of one Group (AR-04). */
public interface ContactRepository extends Repository<Contact, UUID>, GroupOwnedStore<Contact> {

  /**
   * The active Contacts of the Group with this e-mail, compared case-insensitively (R-006-06,
   * BR-11), oldest first.
   *
   * @param groupId the Group
   * @param email the e-mail
   * @return the Contacts
   */
  @Query(
      """
      SELECT c FROM Contact c
      WHERE c.groupId = :groupId AND c.state = 'active' AND lower(c.fields.email) = lower(:email)
      ORDER BY c.createdAt, c.id
      """)
  List<Contact> findActiveByEmail(UUID groupId, String email);

  /**
   * The Contacts of the Group in the recycle bin, newest first (R-012-02).
   *
   * @param groupId the Group
   * @return the Contacts
   */
  @Query(
      """
      SELECT c FROM Contact c
      WHERE c.groupId = :groupId AND c.state = 'inRecycleBin'
      ORDER BY c.deletedAt DESC, c.id
      """)
  List<Contact> findInRecycleBin(UUID groupId);

  /**
   * A Contact of the Group in any state (R-014-06).
   *
   * @param groupId the Group
   * @param id the Contact
   * @return the Contact, empty when it is not one of the Group's (BR-02)
   */
  Optional<Contact> findByGroupIdAndId(UUID groupId, UUID id);

  /**
   * The Contacts of the Group merged into a Contact (D-21).
   *
   * @param groupId the Group
   * @param mergedInto the Primary contact
   * @return the merged Contacts
   */
  List<Contact> findByGroupIdAndMergedInto(UUID groupId, UUID mergedInto);

  /**
   * A Contact of the Group in any state, locked until the transaction ends so that two saves of it
   * run one after the other (specification §4, BR-06).
   *
   * @param groupId the Group
   * @param id the Contact
   * @return the Contact, empty when it is not one of the Group's (BR-02)
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT c FROM Contact c WHERE c.groupId = :groupId AND c.id = :id")
  Optional<Contact> findForUpdate(UUID groupId, UUID id);

  /**
   * The Group's Contacts moved to the recycle bin at or before the cutoff (BR-08).
   *
   * @param groupId the Group
   * @param cutoff now minus the recycle bin period
   * @return the Contacts
   */
  @Query(
      """
      SELECT c FROM Contact c
      WHERE c.groupId = :groupId AND c.state = 'inRecycleBin' AND c.deletedAt <= :cutoff
      """)
  List<Contact> findDueForRemoval(UUID groupId, Instant cutoff);

  /**
   * Permanently deletes the Group's Contacts moved to the recycle bin at or before the cutoff, with
   * their Versions, Attributes and Tag assignments (BR-08; the database cascades), in one
   * transaction (ADR-10).
   *
   * @param groupId the Group
   * @param cutoff now minus the recycle bin period
   * @return how many Contacts were deleted
   */
  @Transactional
  @Modifying
  @Query(
      """
      DELETE FROM Contact c
      WHERE c.groupId = :groupId AND c.state = 'inRecycleBin' AND c.deletedAt <= :cutoff
      """)
  int permanentlyDeleteDue(UUID groupId, Instant cutoff);

  /**
   * Permanently deletes one Contact of the Group with its Versions, Attributes and Tag assignments
   * (R-013-01, SR-06; the database cascades).
   *
   * @param groupId the Group
   * @param id the Contact
   * @return how many Contacts were deleted: 1, or 0 when it is not one of the Group's
   */
  @Transactional
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("DELETE FROM Contact c WHERE c.groupId = :groupId AND c.id = :id")
  int permanentlyDelete(UUID groupId, UUID id);
}
