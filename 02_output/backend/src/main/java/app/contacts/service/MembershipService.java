package app.contacts.service;

import app.contacts.repository.MembershipRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The Members of a Group, managed by its Owner (US-005). */
@Service
public class MembershipService {

  private final MembershipRepository memberships;
  private final AccessGuard guard;

  /**
   * The service.
   *
   * @param memberships the Membership repository
   * @param guard the access check
   */
  public MembershipService(MembershipRepository memberships, AccessGuard guard) {
    this.memberships = memberships;
    this.guard = guard;
  }

  /**
   * The Members with display name, e-mail and Role; Owner only (R-005-01).
   *
   * @param groupId the Group
   * @param userId the signed-in User
   * @return the Members by display name
   * @throws NotFoundException for a non-Member (BR-02)
   * @throws ForbiddenException for an Editor or a Viewer (R-005-01)
   */
  @Transactional(readOnly = true)
  public List<MemberView> list(UUID groupId, UUID userId) {
    guard.require(groupId, userId, Permission.MANAGE_GROUP);
    return memberships.findMembers(groupId).stream()
        .map(row -> new MemberView(row.userId(), row.displayName(), row.email(), row.role()))
        .toList();
  }
}
