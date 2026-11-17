package app.contacts.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.contacts.domain.Attribute;
import app.contacts.domain.Contact;
import app.contacts.domain.ContactFields;
import app.contacts.domain.ContactTag;
import app.contacts.domain.ContactVersion;
import app.contacts.domain.Group;
import app.contacts.domain.Tag;
import app.contacts.domain.User;
import jakarta.persistence.EntityManager;
import java.time.Duration;
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
  @Autowired private AttributeRepository attributes;
  @Autowired private TagRepository tags;
  @Autowired private ContactTagRepository contactTags;
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

  @Test
  void r00606FindsActiveContactsOfTheGroupByEmailIgnoringCase() {
    final Contact janez = added(groupId, "janez.novak@example.si", NOW);
    final Contact second = added(groupId, "JANEZ.NOVAK@example.si", NOW.plusSeconds(1));
    added(groupId, "maja.zupan@example.si", NOW);
    Contact inRecycleBin = added(groupId, "janez.novak@example.si", NOW);
    UUID otherGroup = groups.save(Group.create("Podjetje Beta", marko, NOW)).getId();
    added(otherGroup, "janez.novak@example.si", NOW);
    entityManager.flush();
    jdbc.update(
        "UPDATE contact SET state = 'inRecycleBin', deleted_at = now() WHERE id = ?",
        inRecycleBin.getId());
    entityManager.clear();

    assertThat(contacts.findActiveByEmail(groupId, "Janez.Novak@example.si"))
        .extracting(Contact::getId)
        .containsExactly(janez.getId(), second.getId());
  }

  @Test
  void r01406FindsAContactOnlyInItsGroup() {
    Contact janez = added(groupId, "janez.novak@example.si", NOW);
    final UUID otherGroup = groups.save(Group.create("Podjetje Beta", marko, NOW)).getId();
    entityManager.flush();
    entityManager.clear();

    assertThat(contacts.findByGroupIdAndId(groupId, janez.getId()))
        .get()
        .extracting(Contact::getFields)
        .isEqualTo(janez.getFields());
    assertThat(contacts.findByGroupIdAndId(otherGroup, janez.getId())).isEmpty();
  }

  private Contact added(UUID group, String email, Instant createdAt) {
    ContactFields fields =
        new ContactFields("Janez", "Novak", email, null, null, null, null, null, null, null, null);
    return contacts.add(group, Contact.create(group, fields, "janez", "Novak", marko, createdAt));
  }

  @Test
  void r01202FindsTheGroupsContactsInTheRecycleBinNewestFirst() {
    final Contact active = Contact.create(groupId, ZIGA, "", "a", marko, NOW);
    Contact older = Contact.create(groupId, ZIGA, "", "b", marko, NOW);
    Contact newer = Contact.create(groupId, ZIGA, "", "c", marko, NOW);
    UUID otherGroupId = groups.save(Group.create("Drugo", marko, NOW)).getId();
    Contact other = Contact.create(otherGroupId, ZIGA, "", "d", marko, NOW);
    older.moveToRecycleBin(marko, NOW.plus(Duration.ofHours(1)));
    newer.moveToRecycleBin(marko, NOW.plus(Duration.ofHours(2)));
    other.moveToRecycleBin(marko, NOW.plus(Duration.ofHours(3)));
    contacts.add(groupId, active);
    contacts.add(groupId, older);
    contacts.add(groupId, newer);
    contacts.add(otherGroupId, other);
    entityManager.flush();

    assertThat(contacts.findInRecycleBin(groupId))
        .extracting(Contact::getId)
        .containsExactly(newer.getId(), older.getId());
  }

  /** A Contact of the Group with a Version, an Attribute and a Tag, moved at {@code deletedAt}. */
  private Contact withContent(UUID group, Instant deletedAt) {
    Contact contact = contacts.add(group, Contact.create(group, ZIGA, "", "Čuk", marko, NOW));
    Tag vip = tags.add(group, Tag.create(group, "vip-" + contact.getId()));
    attributes.add(group, Attribute.of(contact, "kompetenca", "Java"));
    contactTags.add(group, ContactTag.of(contact, vip));
    if (deletedAt != null) {
      contact.moveToRecycleBin(marko, deletedAt);
    }
    versions.add(group, ContactVersion.of(contact, ContactVersion.CREATED, "[]", "{}"));
    return contact;
  }

  private int rows(String table, Contact contact) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM " + table + " WHERE contact_id = ?", Integer.class, contact.getId());
  }

  @Test
  void br08DeletesTheGroupsDueContactsWithVersionsAttributesAndTagAssignments() {
    Instant cutoff = NOW.plus(Duration.ofDays(1));
    final Contact due = withContent(groupId, cutoff);
    final Contact notYetDue = withContent(groupId, cutoff.plusSeconds(1));
    final Contact active = withContent(groupId, null);
    UUID otherGroupId = groups.save(Group.create("Drugo", marko, NOW)).getId();
    final Contact otherGroups = withContent(otherGroupId, NOW);
    entityManager.flush();

    int deleted = contacts.permanentlyDeleteDue(groupId, cutoff);
    entityManager.clear();

    assertThat(deleted).isEqualTo(1);
    assertThat(contacts.findByGroupIdAndId(groupId, due.getId())).isEmpty();
    for (String table : new String[] {"contact_version", "attribute", "contact_tag"}) {
      assertThat(rows(table, due)).as(table).isZero();
    }
    assertThat(contacts.findByGroupIdAndId(groupId, notYetDue.getId())).isPresent();
    assertThat(contacts.findByGroupIdAndId(groupId, active.getId())).isPresent();
    assertThat(contacts.findByGroupIdAndId(otherGroupId, otherGroups.getId())).isPresent();
    assertThat(groups.findAllIds()).contains(groupId, otherGroupId);
  }

  @Test
  void r01301DeletesOneContactOfTheGroupWithVersionsAttributesAndTagAssignments() {
    final Contact janez = withContent(groupId, NOW);
    final Contact other = withContent(groupId, NOW);
    UUID otherGroupId = groups.save(Group.create("Drugo", marko, NOW)).getId();
    entityManager.flush();

    assertThat(contacts.permanentlyDelete(otherGroupId, janez.getId())).isZero();
    int deleted = contacts.permanentlyDelete(groupId, janez.getId());
    entityManager.clear();

    assertThat(deleted).isEqualTo(1);
    assertThat(contacts.findByGroupIdAndId(groupId, janez.getId())).isEmpty();
    for (String table : new String[] {"contact_version", "attribute", "contact_tag"}) {
      assertThat(rows(table, janez)).as(table).isZero();
      assertThat(rows(table, other)).as(table).isOne();
    }
  }

  @Test
  void br08FindsTheGroupsContactsDueForRemoval() {
    Instant cutoff = NOW.plus(Duration.ofDays(1));
    final Contact due = withContent(groupId, cutoff);
    withContent(groupId, cutoff.plusSeconds(1));
    withContent(groupId, null);
    UUID otherGroupId = groups.save(Group.create("Drugo", marko, NOW)).getId();
    withContent(otherGroupId, NOW);
    entityManager.flush();

    assertThat(contacts.findDueForRemoval(groupId, cutoff))
        .extracting(Contact::getId)
        .containsExactly(due.getId());
  }
}
