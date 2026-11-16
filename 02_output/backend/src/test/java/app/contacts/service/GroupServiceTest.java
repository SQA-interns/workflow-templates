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

import app.contacts.domain.Group;
import app.contacts.domain.MemberGroup;
import app.contacts.domain.Membership;
import app.contacts.domain.Role;
import app.contacts.repository.GroupRepository;
import app.contacts.repository.MembershipRepository;
import java.time.Clock;
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
  private final GroupService service =
      new GroupService(groups, memberships, guard, Clock.fixed(NOW, ZoneOffset.UTC));

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
