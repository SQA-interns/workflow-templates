package app.contacts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.contacts.domain.User;
import app.contacts.repository.GroupRepository;
import app.contacts.repository.MembershipRepository;
import app.contacts.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

class UserServiceTest {

  private static final Instant NOW = Instant.parse("2026-03-05T10:00:00Z");

  private final UserRepository users = mock(UserRepository.class);
  private final GroupRepository groups = mock(GroupRepository.class);
  private final MembershipRepository memberships = mock(MembershipRepository.class);
  private final AccountDeleter accounts = mock(AccountDeleter.class);
  private final UserService service =
      new UserService(users, groups, memberships, accounts, Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  void r00104FirstCallCreatesTheUserWithTokenEmailNameAndLanguageSl() {
    when(users.findByFirebaseUid("uid-ana")).thenReturn(Optional.empty());
    when(users.saveAndFlush(any(User.class))).thenAnswer(call -> call.getArgument(0));

    UserView ana = service.signIn("uid-ana", "ana.kovac@example.si", "Ana Kovač");

    ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
    verify(users).saveAndFlush(saved.capture());
    assertThat(saved.getValue().getFirebaseUid()).isEqualTo("uid-ana");
    assertThat(saved.getValue().getCreatedAt()).isEqualTo(NOW);
    assertThat(ana.id()).isEqualTo(saved.getValue().getId()).isNotNull();
    assertThat(ana.email()).isEqualTo("ana.kovac@example.si");
    assertThat(ana.displayName()).isEqualTo("Ana Kovač");
    assertThat(ana.language()).isEqualTo("sl");
  }

  @Test
  void r00104TokenWithoutNameUsesTheEmailLocalPart() {
    when(users.findByFirebaseUid("uid-ana")).thenReturn(Optional.empty());
    when(users.saveAndFlush(any(User.class))).thenAnswer(call -> call.getArgument(0));

    assertThat(service.signIn("uid-ana", "ana.kovac@example.si", null).displayName())
        .isEqualTo("ana.kovac");
    assertThat(service.signIn("uid-ana", "ana.kovac@example.si", "  ").displayName())
        .isEqualTo("ana.kovac");
  }

  @Test
  void r00104DisplayNameIsCutToTheStoredMaximumOf100() {
    when(users.findByFirebaseUid("uid-ana")).thenReturn(Optional.empty());
    when(users.saveAndFlush(any(User.class))).thenAnswer(call -> call.getArgument(0));

    assertThat(service.signIn("uid-ana", "ana@example.si", "A".repeat(150)).displayName())
        .hasSize(100);
  }

  @Test
  void ex00103SecondCallReturnsTheExistingUser() {
    User existing = User.create("uid-ana", "ana.kovac@example.si", "Ana Kovač", NOW);
    when(users.findByFirebaseUid("uid-ana")).thenReturn(Optional.of(existing));

    UserView ana = service.signIn("uid-ana", "ana.kovac@example.si", "Ana Kovač");

    assertThat(ana.id()).isEqualTo(existing.getId());
    verify(users, never()).saveAndFlush(any(User.class));
  }

  @Test
  void ex00103ConcurrentFirstCallsCreateOneUser() {
    User createdByTheOtherRequest =
        User.create("uid-ana", "ana.kovac@example.si", "Ana Kovač", NOW);
    when(users.findByFirebaseUid("uid-ana"))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(createdByTheOtherRequest));
    when(users.saveAndFlush(any(User.class)))
        .thenThrow(new DataIntegrityViolationException("app_user_firebase_uid_key"));

    UserView ana = service.signIn("uid-ana", "ana.kovac@example.si", "Ana Kovač");

    assertThat(ana.id()).isEqualTo(createdByTheOtherRequest.getId());
  }

  @Test
  void d20DeletedUserIsRefusedAndNeverCreatedAgain() {
    User deleted = User.create("uid-ana", "ana.kovac@example.si", "Ana Kovač", NOW);
    ReflectionTestUtils.setField(deleted, "deletedAt", NOW);
    when(users.findByFirebaseUid("uid-ana")).thenReturn(Optional.of(deleted));

    assertThatThrownBy(() -> service.signIn("uid-ana", "ana.kovac@example.si", "Ana Kovač"))
        .isInstanceOf(UserDeletedException.class);
    verify(users, never()).saveAndFlush(any(User.class));
  }

  @Test
  void r02002ChosenLanguageIsStoredForTheUser() {
    User ana = User.create("uid-ana", "ana.kovac@example.si", "Ana Kovač", NOW);
    when(users.findById(ana.getId())).thenReturn(Optional.of(ana));

    UserView changed = service.changeLanguage(ana.getId(), "en");

    assertThat(changed.language()).isEqualTo("en");
    assertThat(ana.getLanguage()).isEqualTo("en");
    verify(users).save(ana);
  }

  @Test
  void ex02001LanguageDeIsRejectedAndTheLanguageIsUnchanged() {
    User ana = User.create("uid-ana", "ana.kovac@example.si", "Ana Kovač", NOW);
    when(users.findById(ana.getId())).thenReturn(Optional.of(ana));

    assertThatThrownBy(() -> service.changeLanguage(ana.getId(), "de"))
        .isInstanceOfSatisfying(
            ValidationException.class,
            invalid ->
                assertThat(invalid.problems())
                    .containsExactly(new FieldProblem("language", "me.languageInvalid", Map.of())));
    assertThat(ana.getLanguage()).isEqualTo("sl");
    verify(users, never()).save(any(User.class));
  }

  @Test
  void ex02001MissingLanguageIsRejected() {
    assertThatThrownBy(() -> service.changeLanguage(UUID.randomUUID(), null))
        .isInstanceOf(ValidationException.class);
  }

  @Test
  void r02101DeletesTheMembershipsAnonymisesTheUserAndDeletesTheFirebaseAccount() {
    User marko = User.create("uid-marko", "marko.kranjc@gmail.com", "Marko Kranjc", NOW);
    when(users.findById(marko.getId())).thenReturn(Optional.of(marko));
    when(groups.findNamesOwnedBy(marko.getId())).thenReturn(List.of());

    service.closeAccount(marko.getId(), "marko.kranjc@gmail.com");

    assertThat(marko.getDeletedAt()).isEqualTo(NOW);
    assertThat(marko.getEmail()).isNull();
    assertThat(marko.getDisplayName()).isNull();
    assertThat(marko.getFirebaseUid()).isEqualTo("uid-marko");
    InOrder order = inOrder(memberships, users, accounts);
    order.verify(memberships).dropAllOfUser(marko.getId());
    order.verify(users).saveAndFlush(marko);
    order.verify(accounts).purge("uid-marko");
  }

  @Test
  void r02101TypedEmailIsComparedIgnoringCaseAndOuterSpaces() {
    User marko = User.create("uid-marko", "marko.kranjc@gmail.com", "Marko Kranjc", NOW);
    when(users.findById(marko.getId())).thenReturn(Optional.of(marko));
    when(groups.findNamesOwnedBy(marko.getId())).thenReturn(List.of());

    service.closeAccount(marko.getId(), " Marko.Kranjc@Gmail.com ");

    verify(accounts).purge("uid-marko");
  }

  @Test
  void ex02101TypedEmailThatDoesNotMatchIsInvalidAndChangesNothing() {
    User marko = User.create("uid-marko", "marko.kranjc@gmail.com", "Marko Kranjc", NOW);
    when(users.findById(marko.getId())).thenReturn(Optional.of(marko));

    for (String typed : new String[] {"marko@example.si", "", null}) {
      assertThatThrownBy(() -> service.closeAccount(marko.getId(), typed))
          .isInstanceOfSatisfying(
              InvalidRequestException.class,
              invalid -> assertThat(invalid.getCode()).isEqualTo("validation"));
    }
    assertNothingDeleted(marko);
  }

  @Test
  void r02102OwnerOfAnyGroupIsRejectedWithTheGroupNamesAndNothingChanges() {
    User ana = User.create("uid-ana", "ana.kovac@example.si", "Ana Kovač", NOW);
    when(users.findById(ana.getId())).thenReturn(Optional.of(ana));
    when(groups.findNamesOwnedBy(ana.getId())).thenReturn(List.of("Arhiv", "Konferenca 2025"));

    assertThatThrownBy(() -> service.closeAccount(ana.getId(), "ana.kovac@example.si"))
        .isInstanceOfSatisfying(
            ConflictException.class,
            conflict -> {
              assertThat(conflict.getCode()).isEqualTo("account.ownsGroups");
              assertThat(conflict.getParams())
                  .containsExactly(Map.entry("names", "Arhiv, Konferenca 2025"));
            });
    assertNothingDeleted(ana);
  }

  private void assertNothingDeleted(User user) {
    assertThat(user.getDeletedAt()).isNull();
    assertThat(user.getEmail()).isNotNull();
    verify(memberships, never()).dropAllOfUser(any(UUID.class));
    verify(users, never()).saveAndFlush(any(User.class));
    verify(accounts, never()).purge(anyString());
  }
}
