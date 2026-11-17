package app.contacts.service;

import app.contacts.domain.AuditEntry;
import app.contacts.domain.Group;
import app.contacts.domain.Membership;
import app.contacts.domain.Role;
import app.contacts.repository.GroupRepository;
import app.contacts.repository.MembershipRepository;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Groups: create, the Group switcher list, one Group (US-002); rename and recycle bin (US-003). */
@Service
public class GroupService {

  /** Longest Group name after trimming (R-002-02, specification §3). */
  static final int NAME_MAX = 100;

  private final GroupRepository groups;
  private final MembershipRepository memberships;
  private final AccessGuard guard;
  private final AuditService audit;
  private final Clock clock;

  /**
   * The service.
   *
   * @param groups the Group repository
   * @param memberships the Membership repository
   * @param guard the access check
   * @param audit writes the Audit entries
   * @param clock the clock
   */
  public GroupService(
      GroupRepository groups,
      MembershipRepository memberships,
      AccessGuard guard,
      AuditService audit,
      Clock clock) {
    this.groups = groups;
    this.memberships = memberships;
    this.guard = guard;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Creates a Group with the User as its Owner (R-002-01); names need not be unique (EX-002-04).
   *
   * @param userId the signed-in User
   * @param name the name; trimmed, then 1 to 100 characters
   * @return the new Group
   * @throws ValidationException {@code group.nameLength} otherwise (R-002-02)
   */
  @Transactional
  public GroupView create(UUID userId, String name) {
    String trimmed = checkedName(name);
    Group group = groups.save(Group.create(trimmed, userId, clock.instant()));
    memberships.save(Membership.of(group.getId(), userId, Role.OWNER));
    return new GroupView(group.getId(), group.getName(), Role.OWNER.name());
  }

  /**
   * The active Groups of which the User is a Member, by name A–Z (R-002-03).
   *
   * @param userId the signed-in User
   * @return the Groups with the User's Role
   */
  @Transactional(readOnly = true)
  public List<GroupView> listGroups(UUID userId) {
    return groups.findActiveGroupsOf(userId).stream()
        .map(row -> new GroupView(row.getId(), row.getName(), row.getRole()))
        .toList();
  }

  /**
   * One Group of the User.
   *
   * @param groupId the Group
   * @param userId the signed-in User
   * @return the Group with the User's Role
   * @throws NotFoundException for a non-Member (BR-02, EX-002-05)
   */
  @Transactional(readOnly = true)
  public GroupView getGroup(UUID groupId, UUID userId) {
    Role role = guard.require(groupId, userId, Permission.READ);
    Group group = groups.findById(groupId).orElseThrow(NotFoundException::new);
    return new GroupView(group.getId(), group.getName(), role.name());
  }

  /**
   * Renames the Group; only its Owner may (R-003-01, BR-04).
   *
   * @param groupId the Group
   * @param userId the signed-in User
   * @param name the new name; trimmed, then 1 to 100 characters (R-002-02)
   * @return the renamed Group
   * @throws NotFoundException for a non-Member or a Group in the recycle bin (BR-02, R-003-03)
   * @throws ForbiddenException for an Editor or a Viewer (AC-003-02)
   * @throws ValidationException {@code group.nameLength}; the name stays (EX-003-06)
   */
  @Transactional
  public GroupView rename(UUID groupId, UUID userId, String name) {
    Role role = guard.require(groupId, userId, Permission.MANAGE_GROUP);
    Group group = groups.findById(groupId).orElseThrow(NotFoundException::new);
    group.rename(checkedName(name));
    return new GroupView(group.getId(), group.getName(), role.name());
  }

  /**
   * Moves the Group to the recycle bin with {@code deletedAt} = now; from then on it is not found
   * for anyone (R-003-02, R-003-03); writes an Audit entry (BR-10). Only its Owner may (BR-04).
   *
   * @param groupId the Group
   * @param userId the signed-in User
   * @throws NotFoundException for a non-Member or a Group already in the recycle bin (BR-02)
   * @throws ForbiddenException for an Editor or a Viewer (EX-003-03)
   */
  @Transactional
  public void moveToRecycleBin(UUID groupId, UUID userId) {
    guard.require(groupId, userId, Permission.MANAGE_GROUP);
    Group group = groups.findForUpdate(groupId).orElseThrow(NotFoundException::new);
    group.moveToRecycleBin(clock.instant());
    audit.record(groupId, userId, AuditEntry.GROUP_MOVED_TO_RECYCLE_BIN, groupId);
  }

  private static String checkedName(String name) {
    String trimmed = name == null ? "" : name.strip();
    int length = trimmed.codePointCount(0, trimmed.length());
    if (length == 0 || length > NAME_MAX) {
      throw new ValidationException(
          List.of(new FieldProblem("name", "group.nameLength", Map.of())));
    }
    return trimmed;
  }
}
