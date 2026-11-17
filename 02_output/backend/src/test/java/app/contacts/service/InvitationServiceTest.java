package app.contacts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

class InvitationServiceTest {

  private static final Instant NOW = Instant.parse("2026-03-05T09:00:00Z");
  private static final UUID ANA = UUID.randomUUID();
  private static final UUID GROUP = UUID.randomUUID();
  private static final String MARKO = "marko.kranjc@gmail.com";
  private static final UUID MARKO_ID = UUID.randomUUID();

  private final InvitationRepository invitations = mock(InvitationRepository.class);
  private final MembershipRepository memberships = mock(MembershipRepository.class);
  private final GroupRepository groups = mock(GroupRepository.class);
  private final AccessGuard guard = mock(AccessGuard.class);
  private final AuditService audit = mock(AuditService.class);
  private final InvitationService service =
      new InvitationService(
          invitations, memberships, groups, guard, audit, Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  void r00401OwnerCreatesAnOpenInvitationValidFor7DaysWithItsTokenAndAnAuditEntry() {
    when(invitations.add(eq(GROUP), any())).thenAnswer(call -> call.getArgument(1));

    CreatedInvitation created = service.create(GROUP, ANA, "  " + MARKO + " ", "EDITOR");

    ArgumentCaptor<Invitation> stored = ArgumentCaptor.forClass(Invitation.class);
    verify(invitations).add(eq(GROUP), stored.capture());
    Invitation invitation = stored.getValue();
    assertThat(created.invitation())
        .isEqualTo(
            new InvitationView(
                invitation.getId(), MARKO, "EDITOR", Instant.parse("2026-03-12T09:00:00Z")));
    assertThat(invitation.getState()).isEqualTo(Invitation.OPEN);
    InvitationToken token = InvitationToken.parse(created.token()).orElseThrow();
    assertThat(token.groupId()).isEqualTo(GROUP);
    verify(guard).require(GROUP, ANA, Permission.MANAGE_GROUP);
    verify(audit).record(GROUP, ANA, AuditEntry.INVITATION_CREATED, invitation.getId());
  }

  @Test
  void br04OnlyTheOwnerCreatesAnInvitation() {
    when(guard.require(GROUP, ANA, Permission.MANAGE_GROUP)).thenThrow(new ForbiddenException());

    assertThatThrownBy(() -> service.create(GROUP, ANA, MARKO, "VIEWER"))
        .isInstanceOf(ForbiddenException.class);
    verifyNoInteractions(invitations, audit);
  }

  @Test
  void ex00404InvitingMemberIsRejectedAndNothingIsCreated() {
    when(memberships.existsMemberWithEmail(GROUP, "ana.kovac@example.si")).thenReturn(true);

    assertThatThrownBy(() -> service.create(GROUP, ANA, "ana.kovac@example.si", "EDITOR"))
        .isInstanceOf(ConflictException.class)
        .hasMessage("invitation.alreadyMember");
    verifyNoInteractions(invitations, audit);
  }

  @Test
  void r00403NewInvitationRevokesTheOpenOneForTheSameEmailWithAnAuditEntry() {
    Invitation first = Invitation.create(GROUP, MARKO, Role.VIEWER, new byte[32], ANA, NOW);
    when(invitations.findOpenForEmail(GROUP, "Marko.Kranjc@Gmail.com", NOW))
        .thenReturn(List.of(first));
    when(invitations.add(eq(GROUP), any())).thenAnswer(call -> call.getArgument(1));

    service.create(GROUP, ANA, "Marko.Kranjc@Gmail.com", "EDITOR");

    assertThat(first.getState()).isEqualTo(Invitation.REVOKED);
    verify(audit).record(GROUP, ANA, AuditEntry.INVITATION_REVOKED, first.getId());
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "   ", "marko@", "marko kranjc@gmail.com", "@gmail.com"})
  void ex00406InvalidEmailIsRejectedAndNothingIsCreated(String email) {
    assertRejected(email, "VIEWER", new FieldProblem("email", "field.emailInvalid", Map.of()));
  }

  @Test
  void r00401EmailLongerThan254CharactersIsRejected() {
    String email = "m".repeat(243) + "@gmail.com.si";

    assertRejected(
        email,
        "VIEWER",
        new FieldProblem("email", "field.tooLong", Map.of("field", "email", "max", 254)));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"OWNER", "editor", ""})
  void ex00407RoleOtherThanEditorOrViewerIsRejectedAndNothingIsCreated(String role) {
    assertRejected(MARKO, role, new FieldProblem("role", "invitation.roleInvalid", Map.of()));
  }

  @Test
  void r00408ListsTheOpenInvitationsForTheOwner() {
    Invitation marko = Invitation.create(GROUP, MARKO, Role.EDITOR, new byte[32], ANA, NOW);
    when(invitations.findOpen(GROUP, NOW)).thenReturn(List.of(marko));

    assertThat(service.listOpen(GROUP, ANA))
        .containsExactly(
            new InvitationView(
                marko.getId(), MARKO, "EDITOR", Instant.parse("2026-03-12T09:00:00Z")));
    verify(guard).require(GROUP, ANA, Permission.MANAGE_GROUP);
  }

  @Test
  void br04OnlyTheOwnerSeesTheInvitations() {
    when(guard.require(GROUP, ANA, Permission.MANAGE_GROUP)).thenThrow(new ForbiddenException());

    assertThatThrownBy(() -> service.listOpen(GROUP, ANA)).isInstanceOf(ForbiddenException.class);
    verifyNoInteractions(invitations);
  }

  @Test
  void r00409OwnerRevokesAnOpenInvitationWithAnAuditEntry() {
    Invitation marko = Invitation.create(GROUP, MARKO, Role.EDITOR, new byte[32], ANA, NOW);
    when(invitations.findInGroup(GROUP, marko.getId())).thenReturn(Optional.of(marko));

    service.revoke(GROUP, ANA, marko.getId());

    assertThat(marko.getState()).isEqualTo(Invitation.REVOKED);
    verify(guard).require(GROUP, ANA, Permission.MANAGE_GROUP);
    verify(audit).record(GROUP, ANA, AuditEntry.INVITATION_REVOKED, marko.getId());
  }

  @Test
  void r00409AnInvitationThatIsNotOpenIsNotFound() {
    Invitation revoked = Invitation.create(GROUP, MARKO, Role.EDITOR, new byte[32], ANA, NOW);
    revoked.revoke();
    when(invitations.findInGroup(GROUP, revoked.getId())).thenReturn(Optional.of(revoked));
    UUID unknown = UUID.randomUUID();

    assertThatThrownBy(() -> service.revoke(GROUP, ANA, revoked.getId()))
        .isInstanceOf(NotFoundException.class);
    assertThatThrownBy(() -> service.revoke(GROUP, ANA, unknown))
        .isInstanceOf(NotFoundException.class);
    verifyNoInteractions(audit);
  }

  @Test
  void br04OnlyTheOwnerRevokes() {
    UUID id = UUID.randomUUID();
    when(guard.require(GROUP, ANA, Permission.MANAGE_GROUP)).thenThrow(new ForbiddenException());

    assertThatThrownBy(() -> service.revoke(GROUP, ANA, id)).isInstanceOf(ForbiddenException.class);
    verify(invitations, never()).findInGroup(any(), any());
  }

  @Test
  void r00404InviteeSeesTheGroupNameRoleAndExpiry() {
    InvitationToken token = invitation(MARKO, NOW);

    assertThat(service.view(token.value(), marko()))
        .isEqualTo(
            new InviteeView("Konferenca 2025", "EDITOR", Instant.parse("2026-03-12T09:00:00Z")));
  }

  @Test
  void r00404InviteeAcceptsAndBecomesMemberWithTheRoleAndAnAuditEntry() {
    InvitationToken token = invitation(MARKO, NOW);
    Invitation invitation = stored(token);

    GroupView group = service.accept(token.value(), marko());

    assertThat(group).isEqualTo(new GroupView(GROUP, "Konferenca 2025", "EDITOR"));
    ArgumentCaptor<Membership> membership = ArgumentCaptor.forClass(Membership.class);
    verify(memberships).save(membership.capture());
    assertThat(membership.getValue().getId()).isEqualTo(new MembershipId(GROUP, MARKO_ID));
    assertThat(membership.getValue().getRole()).isEqualTo(Role.EDITOR);
    assertThat(invitation.getState()).isEqualTo(Invitation.ACCEPTED);
    assertThat(invitation.getAcceptedBy()).isEqualTo(MARKO_ID);
    assertThat(invitation.getAcceptedAt()).isEqualTo(NOW);
    verify(audit).record(GROUP, MARKO_ID, AuditEntry.INVITATION_ACCEPTED, invitation.getId());
  }

  @Test
  void ex00401AcceptedOneMinuteBeforeItExpires() {
    InvitationToken token = invitation(MARKO, NOW.minus(Duration.ofDays(7)).plusSeconds(60));

    assertThat(service.accept(token.value(), marko()).role()).isEqualTo("EDITOR");
  }

  @Test
  void ex00402AtExpiresAtTheInvitationIsNoLongerValid() {
    InvitationToken token = invitation(MARKO, NOW.minus(Duration.ofDays(7)));

    assertNoLongerValid(token.value());
  }

  @Test
  void ex00403EmailMatchesCaseInsensitively() {
    InvitationToken token = invitation("Marko.Kranjc@Gmail.com", NOW);

    assertThat(service.accept(token.value(), marko()).name()).isEqualTo("Konferenca 2025");
  }

  @Test
  void r00405DotlessLetterMakesAnotherEmail() {
    InvitationToken token = invitation(MARKO, NOW);
    UserView other = new UserView(UUID.randomUUID(), "marko.kranjc@gmaıl.com", "M", "sl");

    assertThatThrownBy(() -> service.accept(token.value(), other))
        .isInstanceOf(ForbiddenException.class)
        .hasMessage("invitation.otherEmail");
  }

  @Test
  void r00405AnotherEmailCannotSeeOrAcceptTheInvitation() {
    InvitationToken token = invitation(MARKO, NOW);
    UserView petra =
        new UserView(UUID.randomUUID(), "petra.horvat@example.si", "Petra Horvat", "sl");

    assertThatThrownBy(() -> service.view(token.value(), petra))
        .isInstanceOf(ForbiddenException.class)
        .hasMessage("invitation.otherEmail");
    assertThatThrownBy(() -> service.accept(token.value(), petra))
        .isInstanceOf(ForbiddenException.class)
        .hasMessage("invitation.otherEmail");
    assertThat(stored(token).getState()).isEqualTo(Invitation.OPEN);
    verify(memberships, never()).save(any());
  }

  @Test
  void r00406AcceptedOrRevokedInvitationIsNoLongerValid() {
    InvitationToken accepted = invitation(MARKO, NOW);
    service.accept(accepted.value(), marko());
    InvitationToken revoked = invitation(MARKO, NOW);
    stored(revoked).revoke();

    assertNoLongerValid(accepted.value());
    assertNoLongerValid(revoked.value());
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "unknown"})
  void ex00408UnknownTokenIsNoLongerValid(String token) {
    assertNoLongerValid(token);
  }

  @Test
  void ex00408IssuedFormButNoSuchInvitationIsNoLongerValid() {
    assertNoLongerValid(InvitationToken.issue(GROUP).value());
  }

  @Test
  void ex00305InvitationOfGroupInTheRecycleBinIsNoLongerValid() {
    InvitationToken token = invitation(MARKO, NOW);
    groups.findById(GROUP).orElseThrow().moveToRecycleBin(NOW);

    assertNoLongerValid(token.value());
  }

  @Test
  void r00404InviteeWhoIsAlreadyMemberIsRejected() {
    InvitationToken token = invitation(MARKO, NOW);
    when(memberships.existsById(new MembershipId(GROUP, MARKO_ID))).thenReturn(true);

    assertThatThrownBy(() -> service.accept(token.value(), marko()))
        .isInstanceOf(ConflictException.class)
        .hasMessage("invitation.alreadyMember");
    verify(memberships, never()).save(any());
  }

  private static UserView marko() {
    return new UserView(MARKO_ID, MARKO, "Marko Kranjc", "sl");
  }

  /** An open EDITOR Invitation of "Konferenca 2025" created at the time, with its token. */
  private InvitationToken invitation(String email, Instant createdAt) {
    Group group = Group.create("Konferenca 2025", ANA, createdAt);
    when(groups.findById(GROUP)).thenReturn(Optional.of(group));
    InvitationToken token = InvitationToken.issue(GROUP);
    Invitation invitation =
        Invitation.create(GROUP, email, Role.EDITOR, token.hash(), ANA, createdAt);
    when(invitations.findByToken(eq(GROUP), eq(token.hash()))).thenReturn(Optional.of(invitation));
    return token;
  }

  private Invitation stored(InvitationToken token) {
    return invitations.findByToken(GROUP, token.hash()).orElseThrow();
  }

  private void assertNoLongerValid(String token) {
    assertThatThrownBy(() -> service.view(token, marko()))
        .isInstanceOf(ConflictException.class)
        .hasMessage("invitation.noLongerValid");
    assertThatThrownBy(() -> service.accept(token, marko()))
        .isInstanceOf(ConflictException.class)
        .hasMessage("invitation.noLongerValid");
  }

  private void assertRejected(String email, String role, FieldProblem problem) {
    assertThatThrownBy(() -> service.create(GROUP, ANA, email, role))
        .isInstanceOfSatisfying(
            ValidationException.class, e -> assertThat(e.problems()).contains(problem));
    verifyNoInteractions(invitations, audit);
  }
}
