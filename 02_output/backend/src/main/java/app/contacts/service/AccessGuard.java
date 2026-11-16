package app.contacts.service;

import app.contacts.domain.Role;
import app.contacts.repository.MembershipRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** The one check of every request to Group data (ADR-04, BR-01 … BR-04, SR-02). */
@Service
public class AccessGuard {

  private final MembershipRepository memberships;

  /**
   * The guard.
   *
   * @param memberships the Membership repository
   */
  public AccessGuard(MembershipRepository memberships) {
    this.memberships = memberships;
  }

  /**
   * The User's Role in the Group, if it has the permission.
   *
   * @param groupId the Group
   * @param userId the signed-in User
   * @param permission what the request needs
   * @return the Role
   * @throws NotFoundException for a non-Member or a Group in the recycle bin (BR-02, R-003-03)
   * @throws ForbiddenException when the Role lacks the permission (BR-03, BR-04)
   */
  public Role require(UUID groupId, UUID userId, Permission permission) {
    Role role = memberships.findActiveRole(groupId, userId).orElseThrow(NotFoundException::new);
    if (!permission.allows(role)) {
      throw new ForbiddenException();
    }
    return role;
  }
}
