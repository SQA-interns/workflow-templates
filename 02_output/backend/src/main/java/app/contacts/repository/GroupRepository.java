package app.contacts.repository;

import app.contacts.domain.Group;
import app.contacts.domain.MemberGroup;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

/** Groups; a Group is the tenant itself, so exempt from AR-04 (ADR-02). */
public interface GroupRepository extends JpaRepository<Group, UUID> {

  /**
   * The active Groups of which the User is a Member, by name A–Z in Slovenian order (R-002-03).
   *
   * @param userId the User
   * @return the Groups with the User's Role
   */
  @Query(
      value =
          """
          SELECT g.id AS id, g.name AS name, m.role AS role
          FROM contact_group g JOIN membership m ON m.group_id = g.id
          WHERE m.user_id = :userId AND g.state = 'active'
          ORDER BY g.name COLLATE "sl-x-icu", g.created_at, g.id
          """,
      nativeQuery = true)
  List<MemberGroup> findActiveGroupsOf(UUID userId);

  /**
   * The Groups in the recycle bin of which the User is the Owner, newest first (R-003-04).
   *
   * @param userId the User
   * @return the Groups
   */
  @Query(
      """
      SELECT g FROM Group g, Membership m
      WHERE m.id.groupId = g.id AND m.id.userId = :userId
        AND m.role = app.contacts.domain.Role.OWNER AND g.state = 'inRecycleBin'
      ORDER BY g.deletedAt DESC, g.id
      """)
  List<Group> findInRecycleBinOwnedBy(UUID userId);

  /**
   * The id of every Group in any state, for the removal job (BR-08, D-18).
   *
   * @return the ids
   */
  @Query("SELECT g.id FROM Group g")
  List<UUID> findAllIds();

  /**
   * The Group, locked until the transaction ends, so a move, a restore and the removal job do not
   * overlap (R-003-02, R-003-05, BR-08).
   *
   * @param id the Group
   * @return the Group in any state
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT g FROM Group g WHERE g.id = :id")
  Optional<Group> findForUpdate(UUID id);

  /**
   * Permanently deletes the Groups moved to the recycle bin at or before the cutoff, with all their
   * Contacts, Versions, Members, Invitations, Tags, Merges and Audit entries (BR-08, D-19; the
   * database cascades), in one transaction (ADR-10).
   *
   * @param cutoff now minus the recycle bin period
   * @return how many Groups were deleted
   */
  @Transactional
  @Modifying
  @Query("DELETE FROM Group g WHERE g.state = 'inRecycleBin' AND g.deletedAt <= :cutoff")
  int permanentlyDeleteDue(Instant cutoff);
}
