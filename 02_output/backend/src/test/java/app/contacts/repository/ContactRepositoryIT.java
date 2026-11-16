package app.contacts.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.contacts.domain.Contact;
import app.contacts.domain.ContactFields;
import app.contacts.domain.ContactVersion;
import app.contacts.domain.Group;
import app.contacts.domain.User;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Contacts and their Versions in PostgreSQL, always within one Group (AR-04, ADR-02, ADR-07). */
@DataJpaTest(properties = "spring.flyway.enabled=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ContactRepositoryIT {

  @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16.15");

  static {
    POSTGRES.start();
  }

  private static final Instant NOW = Instant.parse("2026-03-05T10:00:00Z");
  private static final ContactFields ZIGA =
      new ContactFields(
          "Žiga",
          "Čuk",
          "ziga.cuk@example.si",
          "+386 41 123 456",
          "Podjetje d.o.o.",
          "Direktor",
          "Slovenska cesta 1",
          "1000",
          "Ljubljana",
          "SI",
          "Govornik");

  @Autowired private ContactRepository contacts;
  @Autowired private ContactVersionRepository versions;
  @Autowired private GroupRepository groups;
  @Autowired private UserRepository users;
  @Autowired private EntityManager entityManager;
  @Autowired private JdbcTemplate jdbc;

  private UUID marko;
  private UUID groupId;

  @BeforeEach
  void group() {
    marko =
        users.save(User.create("uid-marko", "marko.kranjc@gmail.com", "Marko Kranjc", NOW)).getId();
    groupId = groups.save(Group.create("Konferenca 2025", marko, NOW)).getId();
  }

  @Test
  void r00601StoresAnActiveContactWithAllFieldsAndVersionNumberOne() {
    Contact contact = Contact.create(groupId, ZIGA, "ziga\ncuk", "Čuk", marko, NOW);

    contacts.add(groupId, contact);
    entityManager.flush();

    Map<String, Object> row =
        jdbc.queryForMap("SELECT * FROM contact WHERE id = ?", contact.getId());
    assertThat(row)
        .containsEntry("group_id", groupId)
        .containsEntry("first_name", "Žiga")
        .containsEntry("job_title", "Direktor")
        .containsEntry("postal_code", "1000")
        .containsEntry("country", "SI")
        .containsEntry("note", "Govornik")
        .containsEntry("state", "active")
        .containsEntry("version_number", 1)
        .containsEntry("search_text", "ziga\ncuk")
        .containsEntry("sort_name", "Čuk")
        .containsEntry("created_by", marko)
        .containsEntry("updated_by", marko);
  }

  @Test
  void ar04AContactOfAnotherGroupIsNotStored() {
    Contact contact = Contact.create(groupId, ZIGA, "ziga", "Čuk", marko, NOW);
    UUID otherGroup = groups.save(Group.create("Podjetje Beta", marko, NOW)).getId();

    assertThatThrownBy(() -> contacts.add(otherGroup, contact))
        .isInstanceOf(InvalidDataAccessApiUsageException.class);
    entityManager.flush();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM contact", Integer.class)).isZero();
  }

  @Test
  void r00601StoresTheVersionWithItsChangesAndSnapshot() {
    Contact contact = Contact.create(groupId, ZIGA, "ziga", "Čuk", marko, NOW);
    contacts.add(groupId, contact);

    versions.add(
        groupId,
        ContactVersion.of(
            contact,
            ContactVersion.CREATED,
            "[{\"item\":\"firstName\",\"oldValue\":null,\"newValue\":\"Žiga\"}]",
            "{\"fields\":{\"firstName\":\"Žiga\"},\"attributes\":[],\"tags\":[]}"));
    entityManager.flush();

    Map<String, Object> row =
        jdbc.queryForMap(
            """
            SELECT group_id, number, kind, changed_by, changed_at,
                   changes -> 0 ->> 'newValue' AS first_change,
                   snapshot -> 'fields' ->> 'firstName' AS first_name
            FROM contact_version WHERE contact_id = ?
            """,
            contact.getId());
    assertThat(row)
        .containsEntry("group_id", groupId)
        .containsEntry("number", 1)
        .containsEntry("kind", "created")
        .containsEntry("changed_by", marko)
        .containsEntry("first_change", "Žiga")
        .containsEntry("first_name", "Žiga");
  }
}
