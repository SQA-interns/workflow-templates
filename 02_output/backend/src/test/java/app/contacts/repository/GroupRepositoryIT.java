package app.contacts.repository;

import static org.assertj.core.api.Assertions.assertThat;

import app.contacts.domain.Group;
import app.contacts.domain.MemberGroup;
import app.contacts.domain.Membership;
import app.contacts.domain.Role;
import app.contacts.domain.User;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** The Group queries against PostgreSQL (collation sl-x-icu, ADR-02). */
@DataJpaTest(properties = "spring.flyway.enabled=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class GroupRepositoryIT {

  @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16.15");

  static {
    POSTGRES.start();
  }

  private static final Instant NOW = Instant.parse("2026-03-05T10:00:00Z");

  @Autowired private GroupRepository groups;
  @Autowired private MembershipRepository memberships;
  @Autowired private UserRepository users;
  @Autowired private JdbcTemplate jdbc;

  private User ana;
  private User marko;

  @BeforeEach
  void users() {
    ana = users.save(User.create("uid-ana", "ana.kovac@example.si", "Ana Kovač", NOW));
    marko = users.save(User.create("uid-marko", "marko.kranjc@gmail.com", "Marko Kranjc", NOW));
  }

  @Test
  void r00203ListsOnlyTheActiveGroupsOfTheMemberSortedByNameInSlovenian() {
    UUID drevo = group(ana, "Drevo");
    UUID cebela = group(ana, "Čebela");
    UUID cesta = group(ana, "Cesta");
    group(marko, "Podjetje Beta");
    UUID gone = group(ana, "Arhiv");
    jdbc.update(
        "UPDATE contact_group SET state = 'inRecycleBin', deleted_at = now() WHERE id = ?", gone);

    List<MemberGroup> listed = groups.findActiveGroupsOf(ana.getId());

    assertThat(listed).extracting(MemberGroup::getId).containsExactly(cesta, cebela, drevo);
    assertThat(listed).extracting(MemberGroup::getRole).containsOnly("OWNER");
  }

  @Test
  void adr04RoleIsFoundOnlyForAMemberOfAnActiveGroup() {
    UUID beta = group(marko, "Podjetje Beta");

    assertThat(memberships.findActiveRole(beta, marko.getId())).contains(Role.OWNER);
    assertThat(memberships.findActiveRole(beta, ana.getId())).isEmpty();

    jdbc.update(
        "UPDATE contact_group SET state = 'inRecycleBin', deleted_at = now() WHERE id = ?", beta);
    assertThat(memberships.findActiveRole(beta, marko.getId())).isEmpty();
  }

  private UUID group(User owner, String name) {
    Group group = groups.save(Group.create(name, owner.getId(), NOW));
    memberships.saveAndFlush(Membership.of(group.getId(), owner.getId(), Role.OWNER));
    return group.getId();
  }
}
