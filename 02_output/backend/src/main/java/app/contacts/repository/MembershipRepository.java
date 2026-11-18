package app.contacts.repository;

import app.contacts.domain.MemberRow;
import app.contacts.domain.Membership;
import app.contacts.domain.MembershipId;
import app.contacts.domain.Role;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** Memberships; read across Groups for the Group switcher, so exempt from AR-04 (ADR-02). */
public interface MembershipRepository extends JpaRepository<Membership, MembershipId> {

  /**
   * The User's Role in the Group, only while the Group is active (ADR-04, R-003-03).
   *
   * @param groupId the Group
   * @param userId the User
   * @return the Role, empty for a non-Member or a Group in the recycle bin
   */
  @Query(
      """
      SELECT m.role FROM Membership m, Group g
      WHERE g.id = m.id.groupId AND m.id.groupId = :groupId AND m.id.userId = :userId
        AND g.state = 'active'
      """)
  Optional<Role> findActiveRole(UUID groupId, UUID userId);

  /**
   * Whether a Member of the Group has the e-mail, compared case-insensitively (R-004-02, BR-11).
   *
   * @param groupId the Group
   * @param email the e-mail
   * @return true for a Member
   */
  @Query(
      """
      SELECT count(m) > 0 FROM Membership m, User u
      WHERE u.id = m.id.userId AND m.id.groupId = :groupId AND lower(u.email) = lower(:email)
      """)
  boolean existsMemberWithEmail(UUID groupId, String email);

  /**
   * The Members of the Group with display name, e-mail and Role, by display name in Slovenian order
   * like the Group switcher (R-005-01, R-002-03).
   *
   * @param groupId the Group
   * @return the Members
   */
  @Query(
      value =
          """
          SELECT u.id AS user_id, u.display_name, u.email, m.role
          FROM membership m JOIN app_user u ON u.id = m.user_id
          WHERE m.group_id = :groupId
          ORDER BY u.display_name COLLATE "sl-x-icu", u.id
          """,
      nativeQuery = true)
  List<MemberRow> findMembers(UUID groupId);

  /**
   * Deletes every Membership of the User, in any Group (R-021-01).
   *
   * @param userId the User
   * @return how many Memberships were deleted
   */
  @Modifying
  @Query("DELETE FROM Membership m WHERE m.id.userId = :userId")
  int dropAllOfUser(UUID userId);
}
