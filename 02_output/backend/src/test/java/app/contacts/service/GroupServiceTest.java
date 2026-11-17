package app.contacts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.contacts.domain.AuditEntry;
import app.contacts.domain.Group;
import app.contacts.domain.MemberGroup;
import app.contacts.domain.Membership;
import app.contacts.domain.MembershipId;
import app.contacts.domain.Role;
import app.contacts.repository.GroupRepository;
import app.contacts.repository.MembershipRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

class GroupServiceTest {

  private static final Instant NOW = Instant.parse("2026-03-05T10:00:00Z");
  private static final UUID ANA = UUID.randomUUID();

  private final GroupRepository groups = mock(GroupRepository.class);
  private final MembershipRepository memberships = mock(MembershipRepository.class);
  private final AccessGuard guard = mock(AccessGuard.class);
  private final AuditService audit = mock(AuditService.class);
  private final GroupService service =
      new GroupService(groups, memberships, guard, audit, Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  void r00201CreatorBecomesOwnerOfTheNewGroup() {
    when(groups.save(any(Group.class))).thenAnswer(call -> call.getArgument(0));

    GroupView created = service.create(ANA, "Konferenca 2025");

    ArgumentCaptor<Membership> membership = ArgumentCaptor.forClass(Membership.class);
    verify(memberships).save(membership.capture());
    assertThat(membership.getValue().getId().groupId()).isEqualTo(created.id()).isNotNull();
    assertThat(membership.getValue().getId().userId()).isEqualTo(ANA);
    assertThat(membership.getValue().getRole()).isEqualTo(Role.OWNER);
    assertThat(created).isEqualTo(new GroupView(created.id(), "Konferenca 2025", "OWNER"));
  }

  @Test
  void ex00203NameIsTrimmed() {
    when(groups.save(any(Group.class))).thenAnswer(call -> call.getArgument(0));

    assertThat(service.create(ANA, "  Konferenca 2025  ").name()).isEqualTo("Konferenca 2025");
  }

  @Test
  void ex00201NameOf100CharactersIsAccepted() {
    when(groups.save(any(Group.class))).thenAnswer(call -> call.getArgument(0));

    assertThat(service.create(ANA, "K".repeat(100)).name()).hasSize(100);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "   ", "\t\n"})
  void r00202BlankNameIsRejectedAndNothingIsCreated(String name) {
    assertNameRejected(name);
  }

  @Test
  void ex00202NameOf101CharactersIsRejectedAndNothingIsCreated() {
    assertNameRejected("K".repeat(101));
  }

  @Test
  void r00202LengthCountsCharactersNotUtf16Units() {
    when(groups.save(any(Group.class))).thenAnswer(call -> call.getArgument(0));

    assertThat(service.create(ANA, "😀".repeat(100)).name()).isEqualTo("😀".repeat(100));
  }

  @Test
  void r00203ListsTheGroupsOfTheUserInTheOrderOfTheQuery() {
    UUID beta = UUID.randomUUID();
    when(groups.findActiveGroupsOf(ANA)).thenReturn(List.of(row(beta, "Podjetje Beta", "VIEWER")));

    assertThat(service.listGroups(ANA))
        .containsExactly(new GroupView(beta, "Podjetje Beta", "VIEWER"));
  }

  @Test
  void ex00205GetGroupChecksMembershipFirst() {
    UUID other = UUID.randomUUID();
    when(guard.require(other, ANA, Permission.READ)).thenThrow(new NotFoundException());

    assertThatThrownBy(() -> service.getGroup(other, ANA)).isInstanceOf(NotFoundException.class);
    verify(groups, never()).findById(any());
  }

  @Test
  void r00203GetGroupShowsNameAndTheCallersRole() {
    Group group = Group.create("Konferenca 2025", ANA, NOW);
    when(guard.require(group.getId(), ANA, Permission.READ)).thenReturn(Role.EDITOR);
    when(groups.findById(group.getId())).thenReturn(Optional.of(group));

    assertThat(service.getGroup(group.getId(), ANA))
        .isEqualTo(new GroupView(group.getId(), "Konferenca 2025", "EDITOR"));
  }

  @Test
  void r00301OwnerRenamesTheGroupWithTheTrimmedName() {
    Group group = Group.create("Konferenca 2025", ANA, NOW);
    when(guard.require(group.getId(), ANA, Permission.MANAGE_GROUP)).thenReturn(Role.OWNER);
    when(groups.findById(group.getId())).thenReturn(Optional.of(group));

    GroupView renamed = service.rename(group.getId(), ANA, "  Konferenca 2026 ");

    assertThat(renamed).isEqualTo(new GroupView(group.getId(), "Konferenca 2026", "OWNER"));
    assertThat(group.getName()).isEqualTo("Konferenca 2026");
  }

  @Test
  void ac00302EditorCannotRenameAndTheNameIsNotRead() {
    UUID groupId = UUID.randomUUID();
    when(guard.require(groupId, ANA, Permission.MANAGE_GROUP)).thenThrow(new ForbiddenException());

    assertThatThrownBy(() -> service.rename(groupId, ANA, "Test"))
        .isInstanceOf(ForbiddenException.class);
    verifyNoInteractions(groups);
  }

  @Test
  void ex00306RenameToA101CharacterNameIsRejectedAndTheNameStays() {
    Group group = Group.create("Konferenca 2025", ANA, NOW);
    when(guard.require(group.getId(), ANA, Permission.MANAGE_GROUP)).thenReturn(Role.OWNER);
    when(groups.findById(group.getId())).thenReturn(Optional.of(group));

    assertThatThrownBy(() -> service.rename(group.getId(), ANA, "K".repeat(101)))
        .isInstanceOfSatisfying(
            ValidationException.class,
            invalid ->
                assertThat(invalid.problems())
                    .extracting(FieldProblem::code)
                    .containsExactly("group.nameLength"));
    assertThat(group.getName()).isEqualTo("Konferenca 2025");
  }

  @Test
  void r00302OwnerMovesTheGroupToTheRecycleBinNowWithAnAuditEntry() {
    Group group = Group.create("Konferenca 2025", ANA, NOW.minusSeconds(3600));
    when(guard.require(group.getId(), ANA, Permission.MANAGE_GROUP)).thenReturn(Role.OWNER);
    when(groups.findForUpdate(group.getId())).thenReturn(Optional.of(group));

    service.moveToRecycleBin(group.getId(), ANA);

    assertThat(group.getState()).isEqualTo(Group.IN_RECYCLE_BIN);
    assertThat(group.getDeletedAt()).isEqualTo(NOW);
    verify(audit).record(group.getId(), ANA, AuditEntry.GROUP_MOVED_TO_RECYCLE_BIN, group.getId());
  }

  @Test
  void ex00303ViewerCannotMoveTheGroupToTheRecycleBin() {
    UUID groupId = UUID.randomUUID();
    when(guard.require(groupId, ANA, Permission.MANAGE_GROUP)).thenThrow(new ForbiddenException());

    assertThatThrownBy(() -> service.moveToRecycleBin(groupId, ANA))
        .isInstanceOf(ForbiddenException.class);
    verifyNoInteractions(groups, audit);
  }

  @Test
  void r00304DeletedGroupsListsTheOwnersGroupsInTheRecycleBinWithTheirRemovalDate() {
    Group group = inRecycleBinSince(NOW.minus(Duration.ofDays(2)));
    when(groups.findInRecycleBinOwnedBy(ANA)).thenReturn(List.of(group));

    assertThat(service.listDeletedGroups(ANA))
        .containsExactly(
            new DeletedGroupView(
                group.getId(),
                "Konferenca 2025",
                NOW.minus(Duration.ofDays(2)),
                NOW.plus(Duration.ofDays(28))));
  }

  @Test
  void ex00301OwnerRestoresOneMinuteBeforeTheRemovalDateWithAnAuditEntry() {
    Group group = inRecycleBinSince(NOW.minus(Duration.ofDays(30)).plusSeconds(60));
    ownedByAna(group);

    GroupView restored = service.restore(group.getId(), ANA);

    assertThat(restored).isEqualTo(new GroupView(group.getId(), "Konferenca 2025", "OWNER"));
    assertThat(group.getState()).isEqualTo(Group.ACTIVE);
    assertThat(group.getDeletedAt()).isNull();
    verify(audit).record(group.getId(), ANA, AuditEntry.GROUP_RESTORED, group.getId());
  }

  @Test
  void ex00302RestoreExactlyAtTheRemovalDateIsRejectedAndNothingChanges() {
    Group group = inRecycleBinSince(NOW.minus(Duration.ofDays(30)));
    ownedByAna(group);

    assertThatThrownBy(() -> service.restore(group.getId(), ANA))
        .isInstanceOfSatisfying(
            ConflictException.class,
            conflict -> assertThat(conflict.getCode()).isEqualTo("group.noLongerRestorable"));
    assertThat(group.getState()).isEqualTo(Group.IN_RECYCLE_BIN);
    verifyNoInteractions(audit);
  }

  @Test
  void r00305OnlyTheOwnerRestoresTheGroupOthersGetNotFound() {
    Group group = inRecycleBinSince(NOW.minus(Duration.ofDays(1)));
    when(groups.findForUpdate(group.getId())).thenReturn(Optional.of(group));
    when(memberships.findById(new MembershipId(group.getId(), ANA)))
        .thenReturn(Optional.of(Membership.of(group.getId(), ANA, Role.EDITOR)));

    assertThatThrownBy(() -> service.restore(group.getId(), ANA))
        .isInstanceOf(NotFoundException.class);
    assertThat(group.getState()).isEqualTo(Group.IN_RECYCLE_BIN);
    verifyNoInteractions(audit);
  }

  @Test
  void r00305AnActiveGroupIsNotRestorable() {
    Group group = Group.create("Konferenca 2025", ANA, NOW);
    ownedByAna(group);

    assertThatThrownBy(() -> service.restore(group.getId(), ANA))
        .isInstanceOf(NotFoundException.class);
    verifyNoInteractions(audit);
  }

  private static Group inRecycleBinSince(Instant deletedAt) {
    Group group = Group.create("Konferenca 2025", ANA, deletedAt.minus(Duration.ofDays(1)));
    group.moveToRecycleBin(deletedAt);
    return group;
  }

  private void ownedByAna(Group group) {
    when(groups.findForUpdate(group.getId())).thenReturn(Optional.of(group));
    when(memberships.findById(new MembershipId(group.getId(), ANA)))
        .thenReturn(Optional.of(Membership.of(group.getId(), ANA, Role.OWNER)));
  }

  private void assertNameRejected(String name) {
    assertThatThrownBy(() -> service.create(ANA, name))
        .isInstanceOfSatisfying(
            ValidationException.class,
            invalid ->
                assertThat(invalid.problems())
                    .extracting(FieldProblem::field, FieldProblem::code)
                    .containsExactly(tuple("name", "group.nameLength")));
    verifyNoInteractions(groups, memberships);
  }

  private static MemberGroup row(UUID id, String name, String role) {
    return new MemberGroup() {
      @Override
      public UUID getId() {
        return id;
      }

      @Override
      public String getName() {
        return name;
      }

      @Override
      public String getRole() {
        return role;
      }
    };
  }
}
