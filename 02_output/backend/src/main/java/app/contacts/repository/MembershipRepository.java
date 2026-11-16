package app.contacts.repository;

import app.contacts.domain.Membership;
import app.contacts.domain.MembershipId;
import app.contacts.domain.Role;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
