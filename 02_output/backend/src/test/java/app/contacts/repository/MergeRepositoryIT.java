package app.contacts.repository;

import static org.assertj.core.api.Assertions.assertThat;

import app.contacts.domain.Contact;
import app.contacts.domain.ContactFields;
import app.contacts.domain.Group;
import app.contacts.domain.Merge;
import app.contacts.domain.User;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Merges and merged Contacts in PostgreSQL, within one Group (US-018, AR-04). */
@DataJpaTest(properties = "spring.flyway.enabled=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MergeRepositoryIT {

  @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16.15");

  static {
    POSTGRES.start();
  }

  private static final Instant NOW = Instant.parse("2026-03-05T10:00:00Z");

  @Autowired private MergeRepository merges;
  @Autowired private ContactRepository contacts;
  @Autowired private GroupRepository groups;
  @Autowired private UserRepository users;
  @Autowired private EntityManager entityManager;
  @Autowired private JdbcTemplate jdbc;

  private UUID marko;
  private UUID groupId;

  @BeforeEach
  void group() {
    marko = users.save(User.create("uid-marko", "marko@example.si", "Marko", NOW)).getId();
    groupId = groups.save(Group.create("Konferenca 2025", marko, NOW)).getId();
  }

  private Contact added(String firstName) {
    ContactFields fields = ContactFields.ofNames(Map.of("firstName", firstName));
    return contacts.add(groupId, Contact.create(groupId, fields, "", "", marko, NOW));
  }

  @Test
  void r01804StoresTheOtherContactAsMergedIntoThePrimaryWithTheMerge() {
    Contact janez = added("Janez");
    Contact other = added("J.");
    entityManager.flush();

    other.mergeInto(janez.getId(), marko, NOW);
    final Merge merge = merges.add(groupId, Merge.create(janez, other, marko, NOW));
    entityManager.flush();

    Map<String, Object> row =
        jdbc.queryForMap(
            "SELECT state, merged_into, version_number FROM contact WHERE id = ?", other.getId());
    assertThat(row.get("state")).isEqualTo("merged");
    assertThat(row.get("merged_into")).isEqualTo(janez.getId());
    assertThat(row.get("version_number")).isEqualTo(2);
    Map<String, Object> stored =
        jdbc.queryForMap(
            "SELECT primary_id, merged_id, merged_by, undone_at FROM contact_merge WHERE id = ?",
            merge.getId());
    assertThat(stored.get("primary_id")).isEqualTo(janez.getId());
    assertThat(stored.get("merged_id")).isEqualTo(other.getId());
    assertThat(stored.get("merged_by")).isEqualTo(marko);
    assertThat(stored.get("undone_at")).isNull();
  }

  @Test
  void r01805FindsTheMergesIntoAPrimaryContactOldestFirst() {
    Contact janez = added("Janez");
    Contact first = added("J.");
    Contact second = added("Jani");
    entityManager.flush();
    Merge older = merges.add(groupId, Merge.create(janez, first, marko, NOW));
    Merge newer = merges.add(groupId, Merge.create(janez, second, marko, NOW.plusSeconds(60)));
    entityManager.flush();

    List<Merge> found = merges.findByGroupIdAndPrimaryIdOrderByMergedAt(groupId, janez.getId());

    assertThat(found).extracting(Merge::getId).containsExactly(older.getId(), newer.getId());
    assertThat(found.get(0).getMergedId()).isEqualTo(first.getId());
  }

  @Test
  void d21FindsTheContactsMergedIntoAContact() {
    Contact janez = added("Janez");
    Contact other = added("J.");
    added("Maja");
    other.mergeInto(janez.getId(), marko, NOW);
    entityManager.flush();

    assertThat(contacts.findByGroupIdAndMergedInto(groupId, janez.getId()))
        .extracting(Contact::getId)
        .containsExactly(other.getId());
  }
}
