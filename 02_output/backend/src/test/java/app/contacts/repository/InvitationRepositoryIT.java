package app.contacts.repository;

import static org.assertj.core.api.Assertions.assertThat;

import app.contacts.domain.Group;
import app.contacts.domain.Invitation;
import app.contacts.domain.Membership;
import app.contacts.domain.Role;
import app.contacts.domain.User;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Invitations of a Group in PostgreSQL (R-004-01 … R-004-04, R-004-08, BR-11, AR-04). */
@DataJpaTest(properties = "spring.flyway.enabled=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class InvitationRepositoryIT {

  @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16.15");

  static {
    POSTGRES.start();
  }

  private static final Instant NOW = Instant.parse("2026-03-05T09:00:00Z");
  private static final String MARKO = "marko.kranjc@gmail.com";

  @Autowired private InvitationRepository invitations;
  @Autowired private MembershipRepository memberships;
  @Autowired private GroupRepository groups;
  @Autowired private UserRepository users;
  @Autowired private EntityManager entityManager;

  private UUID ana;
  private UUID groupId;
  private UUID otherGroupId;

  @BeforeEach
  void group() {
    ana = users.save(User.create("uid-ana", "ana.kovac@example.si", "Ana Kovač", NOW)).getId();
    groupId = groups.save(Group.create("Konferenca 2025", ana, NOW)).getId();
    otherGroupId = groups.save(Group.create("Podjetje Beta", ana, NOW)).getId();
    memberships.saveAndFlush(Membership.of(groupId, ana, Role.OWNER));
  }

  @Test
  void r00408FindsTheOpenInvitationsOfTheGroupOldestFirst() {
    final Invitation marko = invite(groupId, MARKO, NOW);
    final Invitation petra = invite(groupId, "petra.horvat@example.si", NOW.plusSeconds(60));
    Invitation revoked = invite(groupId, "nina.kos@example.si", NOW);
    revoked.revoke();
    invite(groupId, "janez.novak@example.si", NOW.minus(Duration.ofDays(7)));
    invite(otherGroupId, "maja.zupan@example.si", NOW);
    entityManager.flush();

    assertThat(invitations.findOpen(groupId, NOW))
        .extracting(Invitation::getId)
        .containsExactly(marko.getId(), petra.getId());
  }

  @Test
  void r00403FindsTheOpenInvitationsForAnEmailCaseInsensitively() {
    Invitation first = invite(groupId, "Marko.Kranjc@Gmail.com", NOW);
    invite(otherGroupId, MARKO, NOW);
    entityManager.flush();

    assertThat(invitations.findOpenForEmail(groupId, MARKO, NOW))
        .extracting(Invitation::getId)
        .containsExactly(first.getId());
  }

  @Test
  void r00404FindsAnInvitationByTheHashOfItsTokenOnlyInItsGroup() {
    byte[] hash = randomHash();
    Invitation marko =
        invitations.add(groupId, Invitation.create(groupId, MARKO, Role.EDITOR, hash, ana, NOW));
    entityManager.flush();

    assertThat(invitations.findByToken(groupId, hash.clone())).contains(marko);
    assertThat(invitations.findByToken(otherGroupId, hash.clone())).isEmpty();
    assertThat(invitations.findByToken(groupId, randomHash())).isEmpty();
  }

  @Test
  void r00409FindsAnInvitationByIdOnlyInItsGroup() {
    Invitation marko = invite(groupId, MARKO, NOW);
    entityManager.flush();

    assertThat(invitations.findInGroup(groupId, marko.getId())).contains(marko);
    assertThat(invitations.findInGroup(otherGroupId, marko.getId())).isEmpty();
  }

  @Test
  void r00402KnowsWhetherAnEmailBelongsToAMemberCaseInsensitively() {
    assertThat(memberships.existsMemberWithEmail(groupId, "Ana.Kovac@Example.si")).isTrue();
    assertThat(memberships.existsMemberWithEmail(otherGroupId, "ana.kovac@example.si")).isFalse();
    assertThat(memberships.existsMemberWithEmail(groupId, MARKO)).isFalse();
  }

  private Invitation invite(UUID group, String email, Instant at) {
    return invitations.add(
        group, Invitation.create(group, email, Role.EDITOR, randomHash(), ana, at));
  }

  private static byte[] randomHash() {
    byte[] hash = new byte[32];
    new java.security.SecureRandom().nextBytes(hash);
    return hash;
  }
}
