package app.contacts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.contacts.domain.User;
import app.contacts.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

class UserServiceTest {

  private static final Instant NOW = Instant.parse("2026-03-05T10:00:00Z");

  private final UserRepository users = mock(UserRepository.class);
  private final UserService service = new UserService(users, Clock.fixed(NOW, ZoneOffset.UTC));

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
}
