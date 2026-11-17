package app.contacts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.contacts.domain.AuditEntry;
import app.contacts.domain.MemberRow;
import app.contacts.domain.Membership;
import app.contacts.domain.MembershipId;
import app.contacts.domain.Role;
import app.contacts.repository.MembershipRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class MembershipServiceTest {

  private static final UUID GROUP = UUID.randomUUID();
  private static final UUID ANA = UUID.randomUUID();
  private static final UUID MARKO = UUID.randomUUID();

  private final MembershipRepository memberships = mock(MembershipRepository.class);
  private final AccessGuard guard = mock(AccessGuard.class);
  private final AuditService audit = mock(AuditService.class);
  private final MembershipService service = new MembershipService(memberships, guard, audit);

  @Test
  void r00501OwnerSeesEveryMemberWithDisplayNameEmailAndRole() {
    when(memberships.findMembers(GROUP))
        .thenReturn(
            List.of(
                new MemberRow(ANA, "Ana Kovač", "ana.kovac@example.si", "OWNER"),
                new MemberRow(MARKO, "Marko Kranjc", "marko.kranjc@gmail.com", "EDITOR")));

    assertThat(service.list(GROUP, ANA))
        .containsExactly(
            new MemberView(ANA, "Ana Kovač", "ana.kovac@example.si", "OWNER"),
            new MemberView(MARKO, "Marko Kranjc", "marko.kranjc@gmail.com", "EDITOR"));
    verify(guard).require(GROUP, ANA, Permission.MANAGE_GROUP);
  }

  @Test
  void r00501OnlyTheOwnerSeesTheMembers() {
    when(guard.require(GROUP, MARKO, Permission.MANAGE_GROUP)).thenThrow(new ForbiddenException());

    assertThatThrownBy(() -> service.list(GROUP, MARKO)).isInstanceOf(ForbiddenException.class);
    verifyNoInteractions(memberships);
  }

  @Test
  void r00502OwnerChangesEditorToViewerWithAuditEntry() {
    Membership marko = Membership.of(GROUP, MARKO, Role.EDITOR);
    when(memberships.findById(new MembershipId(GROUP, MARKO))).thenReturn(Optional.of(marko));
    when(memberships.findMembers(GROUP))
        .thenReturn(
            List.of(
                new MemberRow(ANA, "Ana Kovač", "ana.kovac@example.si", "OWNER"),
                new MemberRow(MARKO, "Marko Kranjc", "marko.kranjc@gmail.com", "VIEWER")));

    MemberView changed = service.changeRole(GROUP, ANA, MARKO, "VIEWER");

    assertThat(marko.getRole()).isEqualTo(Role.VIEWER);
    assertThat(changed)
        .isEqualTo(new MemberView(MARKO, "Marko Kranjc", "marko.kranjc@gmail.com", "VIEWER"));
    verify(guard).require(GROUP, ANA, Permission.MANAGE_GROUP);
    verify(audit).record(GROUP, ANA, AuditEntry.MEMBER_ROLE_CHANGED, MARKO);
  }

  @Test
  void br04OnlyTheOwnerChangesRoles() {
    when(guard.require(GROUP, MARKO, Permission.MANAGE_GROUP)).thenThrow(new ForbiddenException());

    assertThatThrownBy(() -> service.changeRole(GROUP, MARKO, ANA, "EDITOR"))
        .isInstanceOf(ForbiddenException.class);
    verifyNoInteractions(memberships, audit);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"OWNER", "", "editor", "ADMIN"})
  void ex00503RoleOtherThanEditorOrViewerIsRejectedAndNothingChanges(String role) {
    assertThatThrownBy(() -> service.changeRole(GROUP, ANA, MARKO, role))
        .isInstanceOfSatisfying(
            ValidationException.class,
            e ->
                assertThat(e.problems())
                    .containsExactly(new FieldProblem("role", "member.roleInvalid", Map.of())));
    verifyNoInteractions(memberships, audit);
  }

  @Test
  void ex00502OwnerCannotChangeTheirOwnRole() {
    Membership ana = Membership.of(GROUP, ANA, Role.OWNER);
    when(memberships.findById(new MembershipId(GROUP, ANA))).thenReturn(Optional.of(ana));

    assertThatThrownBy(() -> service.changeRole(GROUP, ANA, ANA, "EDITOR"))
        .isInstanceOf(ConflictException.class)
        .hasMessage("member.ownerRequired");
    assertThat(ana.getRole()).isEqualTo(Role.OWNER);
    verifyNoInteractions(audit);
  }

  @Test
  void r00502RoleChangeOfNonMemberIsNotFound() {
    assertThatThrownBy(() -> service.changeRole(GROUP, ANA, MARKO, "VIEWER"))
        .isInstanceOf(NotFoundException.class);
    verifyNoInteractions(audit);
  }
}
