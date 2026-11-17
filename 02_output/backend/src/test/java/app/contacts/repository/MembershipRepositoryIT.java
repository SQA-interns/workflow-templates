package app.contacts.repository;

import static org.assertj.core.api.Assertions.assertThat;

import app.contacts.domain.Group;
import app.contacts.domain.MemberRow;
import app.contacts.domain.Membership;
import app.contacts.domain.Role;
import app.contacts.domain.User;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** The Members of a Group in PostgreSQL (R-005-01). */
@DataJpaTest(properties = "spring.flyway.enabled=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MembershipRepositoryIT {

  @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16.15");

  static {
    POSTGRES.start();
  }

  private static final Instant NOW = Instant.parse("2026-03-05T09:00:00Z");

  @Autowired private MembershipRepository memberships;
  @Autowired private GroupRepository groups;
  @Autowired private UserRepository users;

  @Test
  void r00501FindsTheMembersOfTheGroupByDisplayNameInSlovenianOrder() {
    UUID ana = user("uid-ana", "ana.kovac@example.si", "Ana Kovač");
    UUID zan = user("uid-zan", "zan.zupan@example.si", "Žan Županič");
    UUID marko = user("uid-marko", "marko.kranjc@gmail.com", "Marko Kranjc");
    UUID groupId = groups.save(Group.create("Konferenca 2025", ana, NOW)).getId();
    UUID otherGroupId = groups.save(Group.create("Podjetje Beta", marko, NOW)).getId();
    memberships.save(Membership.of(groupId, ana, Role.OWNER));
    memberships.save(Membership.of(groupId, zan, Role.VIEWER));
    memberships.save(Membership.of(groupId, marko, Role.EDITOR));
    memberships.saveAndFlush(Membership.of(otherGroupId, marko, Role.OWNER));

    assertThat(memberships.findMembers(groupId))
        .containsExactly(
            new MemberRow(ana, "Ana Kovač", "ana.kovac@example.si", "OWNER"),
            new MemberRow(marko, "Marko Kranjc", "marko.kranjc@gmail.com", "EDITOR"),
            new MemberRow(zan, "Žan Županič", "zan.zupan@example.si", "VIEWER"));
  }

  private UUID user(String uid, String email, String displayName) {
    return users.save(User.create(uid, email, displayName, NOW)).getId();
  }
}
