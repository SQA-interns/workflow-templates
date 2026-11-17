package app.contacts.service;

import app.contacts.domain.AuditEntry;
import app.contacts.domain.Group;
import app.contacts.domain.Invitation;
import app.contacts.domain.Membership;
import app.contacts.domain.MembershipId;
import app.contacts.domain.Role;
import app.contacts.repository.GroupRepository;
import app.contacts.repository.InvitationRepository;
import app.contacts.repository.MembershipRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Invitations: create, list and revoke by the Owner; view and accept by the invitee (US-004). */
@Service
public class InvitationService {

  /** Longest e-mail in characters (business-rules.md, Data model). */
  static final int EMAIL_MAX = 254;

  /** The Roles an Invitation can grant; never Owner (R-004-01, EX-004-07). */
  private static final Set<String> INVITABLE = Set.of(Role.EDITOR.name(), Role.VIEWER.name());

  private final InvitationRepository invitations;
  private final MembershipRepository memberships;
  private final GroupRepository groups;
  private final AccessGuard guard;
  private final AuditService audit;
  private final Clock clock;

  /**
   * The service.
   *
   * @param invitations the Invitation repository
   * @param memberships the Membership repository
   * @param groups the Group repository
   * @param guard the access check
   * @param audit writes the Audit entries
   * @param clock the clock
   */
  public InvitationService(
      InvitationRepository invitations,
      MembershipRepository memberships,
      GroupRepository groups,
      AccessGuard guard,
      AuditService audit,
      Clock clock) {
    this.invitations = invitations;
    this.memberships = memberships;
    this.groups = groups;
    this.guard = guard;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Creates an open Invitation valid for 7 days and returns its token once (R-004-01); an open
   * Invitation for the same e-mail is revoked (R-004-03); both write Audit entries (BR-10, D-17).
   *
   * @param groupId the Group
   * @param userId the signed-in User
   * @param email the invitee's e-mail; trimmed
   * @param role {@code EDITOR} or {@code VIEWER}
   * @return the Invitation and its token
   * @throws NotFoundException for a non-Member (BR-02)
   * @throws ForbiddenException for an Editor or a Viewer (BR-04, AC-004-05)
   * @throws ValidationException {@code field.emailInvalid}, {@code field.tooLong} or {@code
   *     invitation.roleInvalid} (EX-004-06, EX-004-07)
   * @throws ConflictException {@code invitation.alreadyMember} (R-004-02)
   */
  @Transactional
  public CreatedInvitation create(UUID groupId, UUID userId, String email, String role) {
    guard.require(groupId, userId, Permission.MANAGE_GROUP);
    String trimmed = email == null ? "" : email.strip();
    checkRequest(trimmed, role);
    if (memberships.existsMemberWithEmail(groupId, trimmed)) {
      throw new ConflictException("invitation.alreadyMember", Map.of());
    }
    Instant now = clock.instant();
    for (Invitation earlier : invitations.findOpenForEmail(groupId, trimmed, now)) {
      markRevoked(earlier, userId);
    }
    InvitationToken token = InvitationToken.issue(groupId);
    Invitation invitation =
        invitations.add(
            groupId,
            Invitation.create(groupId, trimmed, Role.valueOf(role), token.hash(), userId, now));
    audit.record(groupId, userId, AuditEntry.INVITATION_CREATED, invitation.getId());
    return new CreatedInvitation(toView(invitation), token.value());
  }

  /**
   * The open Invitations of the Group with e-mail, Role and expiry (R-004-08); Owner only.
   *
   * @param groupId the Group
   * @param userId the signed-in User
   * @return the Invitations, oldest first
   */
  @Transactional(readOnly = true)
  public List<InvitationView> listOpen(UUID groupId, UUID userId) {
    guard.require(groupId, userId, Permission.MANAGE_GROUP);
    return invitations.findOpen(groupId, clock.instant()).stream()
        .map(InvitationService::toView)
        .toList();
  }

  /**
   * Revokes an open Invitation with an Audit entry (R-004-09, BR-10); Owner only.
   *
   * @param groupId the Group
   * @param userId the signed-in User
   * @param invitationId the Invitation
   * @throws NotFoundException when the Group has no open Invitation with this id
   */
  @Transactional
  public void revoke(UUID groupId, UUID userId, UUID invitationId) {
    guard.require(groupId, userId, Permission.MANAGE_GROUP);
    Invitation invitation =
        invitations
            .findInGroup(groupId, invitationId)
            .filter(found -> found.isOpenAt(clock.instant()))
            .orElseThrow(NotFoundException::new);
    markRevoked(invitation, userId);
  }

  /**
   * The Invitation of the link for its invitee (R-004-04).
   *
   * @param token the token of the link
   * @param user the signed-in User
   * @return the Group name, Role and expiry
   * @throws ConflictException {@code invitation.noLongerValid} (R-004-06, EX-004-08, EX-003-05)
   * @throws ForbiddenException {@code invitation.otherEmail} (R-004-05)
   */
  @Transactional
  public InviteeView view(String token, UserView user) {
    Invitation invitation = usable(token, user);
    return new InviteeView(
        activeGroup(invitation).getName(), invitation.getRole().name(), invitation.getExpiresAt());
  }

  /**
   * Makes the invitee a Member with the Invitation's Role and marks the Invitation accepted, with
   * an Audit entry (R-004-04, D-17).
   *
   * @param token the token of the link
   * @param user the signed-in User
   * @return the Group with the new Role
   * @throws ConflictException {@code invitation.noLongerValid} (R-004-06), {@code
   *     invitation.alreadyMember} when the User is a Member already
   * @throws ForbiddenException {@code invitation.otherEmail} (R-004-05)
   */
  @Transactional
  public GroupView accept(String token, UserView user) {
    Invitation invitation = usable(token, user);
    UUID groupId = invitation.getGroupId();
    if (memberships.existsById(new MembershipId(groupId, user.id()))) {
      throw new ConflictException("invitation.alreadyMember", Map.of());
    }
    memberships.save(Membership.of(groupId, user.id(), invitation.getRole()));
    invitation.accept(user.id(), clock.instant());
    audit.record(groupId, user.id(), AuditEntry.INVITATION_ACCEPTED, invitation.getId());
    return new GroupView(groupId, activeGroup(invitation).getName(), invitation.getRole().name());
  }

  /** The open Invitation of the token in an active Group, if it is for the User. */
  private Invitation usable(String token, UserView user) {
    Invitation invitation =
        InvitationToken.parse(token)
            .flatMap(parsed -> invitations.findByToken(parsed.groupId(), parsed.hash()))
            .filter(found -> found.isOpenAt(clock.instant()))
            .filter(
                found ->
                    groups
                        .findById(found.getGroupId())
                        .filter(g -> Group.ACTIVE.equals(g.getState()))
                        .isPresent())
            .orElseThrow(() -> new ConflictException("invitation.noLongerValid", Map.of()));
    if (!invitation.isFor(user.email())) {
      throw new ForbiddenException("invitation.otherEmail");
    }
    return invitation;
  }

  private Group activeGroup(Invitation invitation) {
    return groups.findById(invitation.getGroupId()).orElseThrow(NotFoundException::new);
  }

  private void markRevoked(Invitation invitation, UUID userId) {
    invitation.revoke();
    audit.record(
        invitation.getGroupId(), userId, AuditEntry.INVITATION_REVOKED, invitation.getId());
  }

  private static void checkRequest(String email, String role) {
    List<FieldProblem> problems = new ArrayList<>();
    if (email.codePointCount(0, email.length()) > EMAIL_MAX) {
      problems.add(
          new FieldProblem("email", "field.tooLong", Map.of("field", "email", "max", EMAIL_MAX)));
    } else if (!ContactValidator.EMAIL.test(email)) {
      problems.add(new FieldProblem("email", "field.emailInvalid", Map.of()));
    }
    if (role == null || !INVITABLE.contains(role)) {
      problems.add(new FieldProblem("role", "invitation.roleInvalid", Map.of()));
    }
    if (!problems.isEmpty()) {
      throw new ValidationException(problems);
    }
  }

  private static InvitationView toView(Invitation invitation) {
    return new InvitationView(
        invitation.getId(),
        invitation.getEmail(),
        invitation.getRole().name(),
        invitation.getExpiresAt());
  }
}
