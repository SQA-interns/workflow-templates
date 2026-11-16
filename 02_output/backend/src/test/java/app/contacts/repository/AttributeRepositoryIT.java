package app.contacts.repository;

import static org.assertj.core.api.Assertions.assertThat;

import app.contacts.domain.Attribute;
import app.contacts.domain.Contact;
import app.contacts.domain.ContactAttributeRow;
import app.contacts.domain.ContactFields;
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
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** The Attributes of a Contact in PostgreSQL, always within its Group (R-008-01, AR-04). */
@DataJpaTest(properties = "spring.flyway.enabled=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ContactQueryRepository.class)
class AttributeRepositoryIT {

  @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16.15");

  static {
    POSTGRES.start();
  }

  private static final Instant NOW = Instant.parse("2026-03-05T10:00:00Z");

  @Autowired private AttributeRepository attributes;
  @Autowired private ContactRepository contacts;
  @Autowired private ContactQueryRepository contactQueries;
  @Autowired private GroupRepository groups;
  @Autowired private UserRepository users;
  @Autowired private EntityManager entityManager;
  @Autowired private JdbcTemplate jdbc;

  private UUID groupId;
  private Contact janez;
  private Contact maja;

  @BeforeEach
  void contacts() {
    UUID marko =
        users.save(User.create("uid-marko", "marko.kranjc@gmail.com", "Marko Kranjc", NOW)).getId();
    groupId = groups.save(Group.create("Konferenca 2025", marko, NOW)).getId();
    janez = contactOf(marko, "Janez");
    maja = contactOf(marko, "Maja");
  }

  private Contact contactOf(UUID userId, String firstName) {
    ContactFields fields = ContactFields.ofNames(Map.of("firstName", firstName));
    return contacts.add(
        groupId, Contact.create(groupId, fields, firstName, firstName, userId, NOW));
  }

  @Test
  void r00801StoresAnAttributeOfTheContactInItsGroup() {
    attributes.add(groupId, Attribute.of(janez, "kompetenca", "spletnoProgramiranje"));
    entityManager.flush();

    Map<String, Object> row =
        jdbc.queryForMap("SELECT * FROM attribute WHERE contact_id = ?", janez.getId());
    assertThat(row)
        .containsEntry("group_id", groupId)
        .containsEntry("key", "kompetenca")
        .containsEntry("value", "spletnoProgramiranje");
  }

  @Test
  void r00801RemovedAttributesMakeRoomForTheSameKeyInOtherCase() {
    attributes.add(groupId, Attribute.of(janez, "kompetenca", "spletnoProgramiranje"));
    attributes.add(groupId, Attribute.of(maja, "kompetenca", "java"));
    entityManager.flush();

    attributes.deleteOfContact(groupId, janez.getId());
    attributes.add(groupId, Attribute.of(janez, "Kompetenca", "mobilnoProgramiranje"));

    assertThat(contactQueries.findAttributes(groupId, janez.getId()))
        .containsExactly(new ContactAttributeRow("Kompetenca", "mobilnoProgramiranje"));
    assertThat(contactQueries.findAttributes(groupId, maja.getId()))
        .containsExactly(new ContactAttributeRow("kompetenca", "java"));
  }

  @Test
  void ar04AttributesOfTheContactInAnotherGroupAreNotRemoved() {
    attributes.add(groupId, Attribute.of(janez, "kompetenca", "java"));
    entityManager.flush();

    attributes.deleteOfContact(UUID.randomUUID(), janez.getId());

    assertThat(contactQueries.findAttributes(groupId, janez.getId())).hasSize(1);
  }
}
