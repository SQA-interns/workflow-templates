package app.contacts.repository;

import static org.assertj.core.api.Assertions.assertThat;

import app.contacts.domain.AuditEntry;
import app.contacts.domain.Group;
import app.contacts.domain.User;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Audit entries of one Group in PostgreSQL (BR-10, SR-04, AR-04). */
@DataJpaTest(properties = "spring.flyway.enabled=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuditEntryRepositoryIT {

  @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16.15");

  static {
    POSTGRES.start();
  }

  private static final Instant NOW = Instant.parse("2026-03-06T08:00:00Z");

  @Autowired private AuditEntryRepository entries;
  @Autowired private GroupRepository groups;
  @Autowired private UserRepository users;
  @Autowired private EntityManager entityManager;
  @Autowired private JdbcTemplate jdbc;

  @Test
  void br10StoresTheEntryWithTheUserTheActionTheTargetAndTheTime() {
    UUID ana = users.save(User.create("uid-ana", "ana@example.si", "Ana Horvat", NOW)).getId();
    UUID groupId = groups.save(Group.create("Konferenca 2025", ana, NOW)).getId();
    UUID contactId = UUID.randomUUID();

    entries.add(
        groupId,
        AuditEntry.create(groupId, ana, AuditEntry.CONTACT_PERMANENTLY_DELETED, contactId, NOW));
    entityManager.flush();

    Map<String, Object> row =
        jdbc.queryForMap(
            "SELECT user_id, action, target_id, at FROM audit_entry WHERE group_id = ?", groupId);
    assertThat(row.get("user_id")).isEqualTo(ana);
    assertThat(row.get("action")).isEqualTo("contact.permanentlyDeleted");
    assertThat(row.get("target_id")).isEqualTo(contactId);
    assertThat(((java.sql.Timestamp) row.get("at")).toInstant()).isEqualTo(NOW);
  }
}
