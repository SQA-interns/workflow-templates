package app.contacts.service;

import app.contacts.domain.AuditEntry;
import app.contacts.domain.Membership;
import app.contacts.domain.MembershipId;
import app.contacts.domain.Role;
import app.contacts.repository.MembershipRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The Members of a Group, managed by its Owner (US-005). */
@Service
public class MembershipService {

  /** The Roles the Owner can give a Member; never Owner (R-005-02, EX-005-03). */
  private static final Set<String> ASSIGNABLE = Set.of(Role.EDITOR.name(), Role.VIEWER.name());

  private final MembershipRepository memberships;
  private final AccessGuard guard;
  private final AuditService audit;

  /**
   * The service.
   *
   * @param memberships the Membership repository
   * @param guard the access check
   * @param audit writes the Audit entries
   */
  public MembershipService(
      MembershipRepository memberships, AccessGuard guard, AuditService audit) {
    this.memberships = memberships;
    this.guard = guard;
    this.audit = audit;
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
    return members(groupId).toList();
  }

  /**
   * Changes a Member between Editor and Viewer with an Audit entry (R-005-02, BR-10); the new Role
   * applies to the Member's next request, because every request reads it (ADR-04).
   *
   * @param groupId the Group
   * @param userId the signed-in User
   * @param memberId the Member whose Role changes
   * @param role {@code EDITOR} or {@code VIEWER}
   * @return the Member with the new Role
   * @throws NotFoundException for a non-Member caller (BR-02) or a User who is not a Member
   * @throws ForbiddenException for an Editor or a Viewer (BR-04)
   * @throws ValidationException {@code member.roleInvalid} for any other Role (EX-005-03)
   * @throws ConflictException {@code member.ownerRequired} for the Owner (R-005-05)
   */
  @Transactional
  public MemberView changeRole(UUID groupId, UUID userId, UUID memberId, String role) {
    guard.require(groupId, userId, Permission.MANAGE_GROUP);
    if (role == null || !ASSIGNABLE.contains(role)) {
      throw new ValidationException(
          List.of(new FieldProblem("role", "member.roleInvalid", Map.of())));
    }
    notOwner(groupId, memberId).changeRole(Role.valueOf(role));
    audit.record(groupId, userId, AuditEntry.MEMBER_ROLE_CHANGED, memberId);
    return members(groupId)
        .filter(member -> member.userId().equals(memberId))
        .findFirst()
        .orElseThrow(NotFoundException::new);
  }

  /**
   * Removes a Member from the Group with an Audit entry (R-005-03, BR-10); the Versions they made
   * keep their User id and so their display name (R-005-04).
   *
   * @param groupId the Group
   * @param userId the signed-in User
   * @param memberId the Member to remove
   * @throws NotFoundException for a non-Member caller (BR-02) or a User who is not a Member
   * @throws ForbiddenException for an Editor or a Viewer (BR-04)
   * @throws ConflictException {@code member.ownerRequired} for the Owner (R-005-05)
   */
  @Transactional
  public void endMembership(UUID groupId, UUID userId, UUID memberId) {
    guard.require(groupId, userId, Permission.MANAGE_GROUP);
    memberships.delete(notOwner(groupId, memberId));
    audit.record(groupId, userId, AuditEntry.MEMBER_REMOVED, memberId);
  }

  private Stream<MemberView> members(UUID groupId) {
    return memberships.findMembers(groupId).stream()
        .map(row -> new MemberView(row.userId(), row.displayName(), row.email(), row.role()));
  }

  /** The Membership of a Member who is not the Owner; a Group always has one Owner (R-005-05). */
  private Membership notOwner(UUID groupId, UUID memberId) {
    Membership membership =
        memberships
            .findById(new MembershipId(groupId, memberId))
            .orElseThrow(NotFoundException::new);
    if (membership.getRole() == Role.OWNER) {
      throw new ConflictException("member.ownerRequired", Map.of());
    }
    return membership;
  }
}
