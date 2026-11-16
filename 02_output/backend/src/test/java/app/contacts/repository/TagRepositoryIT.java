package app.contacts.repository;

import static org.assertj.core.api.Assertions.assertThat;

import app.contacts.domain.Contact;
import app.contacts.domain.ContactFields;
import app.contacts.domain.ContactTag;
import app.contacts.domain.ContactTagRow;
import app.contacts.domain.Group;
import app.contacts.domain.Tag;
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
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** The Tags of a Group and their assignment to Contacts in PostgreSQL (R-009-01 … R-009-04). */
@DataJpaTest(properties = "spring.flyway.enabled=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ContactQueryRepository.class)
class TagRepositoryIT {

  @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16.15");

  static {
    POSTGRES.start();
  }

  private static final Instant NOW = Instant.parse("2026-03-05T10:00:00Z");

  @Autowired private TagRepository tags;
  @Autowired private ContactTagRepository contactTags;
  @Autowired private ContactRepository contacts;
  @Autowired private ContactQueryRepository contactQueries;
  @Autowired private GroupRepository groups;
  @Autowired private UserRepository users;
  @Autowired private EntityManager entityManager;
  @Autowired private JdbcTemplate jdbc;

  private UUID marko;
  private UUID groupId;
  private Contact janez;
  private Contact maja;

  @BeforeEach
  void contacts() {
    marko =
        users.save(User.create("uid-marko", "marko.kranjc@gmail.com", "Marko Kranjc", NOW)).getId();
    groupId = groups.save(Group.create("Konferenca 2025", marko, NOW)).getId();
    janez = contactOf(groupId, "Janez");
    maja = contactOf(groupId, "Maja");
  }

  private Contact contactOf(UUID group, String firstName) {
    ContactFields fields = ContactFields.ofNames(Map.of("firstName", firstName));
    return contacts.add(group, Contact.create(group, fields, firstName, firstName, marko, NOW));
  }

  @Test
  void r00904StoresATagOfTheGroupWithItsSpelling() {
    Tag tag = tags.add(groupId, Tag.create(groupId, "udelezenec2025"));
    entityManager.flush();

    Map<String, Object> row = jdbc.queryForMap("SELECT * FROM tag WHERE id = ?", tag.getId());
    assertThat(row).containsEntry("group_id", groupId).containsEntry("name", "udelezenec2025");
  }

  @Test
  void r00903FindsTheTagOfTheGroupCaseInsensitively() {
    Tag tag = tags.add(groupId, Tag.create(groupId, "udelezenec2025"));

    assertThat(tags.findInGroup(groupId, "Udelezenec2025")).contains(tag);
    assertThat(tags.findInGroup(groupId, "udeležba")).isEmpty();
  }

  @Test
  void ex00904TagOfAnotherGroupIsNotFound() {
    UUID beta = groups.save(Group.create("Podjetje Beta", marko, NOW)).getId();
    tags.add(beta, Tag.create(beta, "udelezenec2025"));

    assertThat(tags.findInGroup(groupId, "udelezenec2025")).isEmpty();
  }

  @Test
  void r00901AssignsTagsToAContact() {
    Tag vip = tags.add(groupId, Tag.create(groupId, "vip"));
    Tag neObvescaj = tags.add(groupId, Tag.create(groupId, "neObvescaj"));

    contactTags.add(groupId, ContactTag.of(janez, vip));
    contactTags.add(groupId, ContactTag.of(janez, neObvescaj));
    entityManager.flush();

    assertThat(contactQueries.findTags(groupId, List.of(janez.getId())))
        .containsExactly(
            new ContactTagRow(janez.getId(), neObvescaj.getId(), "neObvescaj"),
            new ContactTagRow(janez.getId(), vip.getId(), "vip"));
  }

  @Test
  void r00901TakesOnlyTheGivenTagsOffTheContact() {
    Tag vip = tags.add(groupId, Tag.create(groupId, "vip"));
    Tag neObvescaj = tags.add(groupId, Tag.create(groupId, "neObvescaj"));
    contactTags.add(groupId, ContactTag.of(janez, vip));
    contactTags.add(groupId, ContactTag.of(janez, neObvescaj));
    contactTags.add(groupId, ContactTag.of(maja, neObvescaj));
    entityManager.flush();

    contactTags.discardOfContact(groupId, janez.getId(), List.of(neObvescaj.getId()));

    assertThat(contactQueries.findTags(groupId, List.of(janez.getId(), maja.getId())))
        .containsExactlyInAnyOrder(
            new ContactTagRow(janez.getId(), vip.getId(), "vip"),
            new ContactTagRow(maja.getId(), neObvescaj.getId(), "neObvescaj"));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM tag", Long.class)).isEqualTo(2);
  }

  @Test
  void ar04TagsOfTheContactInAnotherGroupAreNotTakenOff() {
    Tag vip = tags.add(groupId, Tag.create(groupId, "vip"));
    contactTags.add(groupId, ContactTag.of(janez, vip));
    entityManager.flush();

    contactTags.discardOfContact(UUID.randomUUID(), janez.getId(), List.of(vip.getId()));

    assertThat(contactQueries.findTags(groupId, List.of(janez.getId()))).hasSize(1);
  }

  private Tag on(Contact contact, String name) {
    Tag tag = tags.findInGroup(contact.getGroupId(), name).orElse(null);
    if (tag == null) {
      tag = tags.add(contact.getGroupId(), Tag.create(contact.getGroupId(), name));
    }
    contactTags.add(contact.getGroupId(), ContactTag.of(contact, tag));
    entityManager.flush();
    return tag;
  }

  @Test
  void r00902SuggestsTheTagsOnActiveContactsContainingTheTextCaseInsensitively() {
    on(janez, "udelezenec2025");
    on(maja, "neObvescaj");
    on(maja, "Udeležba");

    assertThat(tags.suggest(groupId, "UDEL"))
        .extracting(Tag::getName)
        .containsExactly("udelezenec2025", "Udeležba");
    assertThat(tags.suggest(groupId, "")).hasSize(3);
  }

  @Test
  void r00902SuggestsATagOnSeveralContactsOnce() {
    on(janez, "vip");
    on(maja, "vip");

    assertThat(tags.suggest(groupId, "vip")).hasSize(1);
  }

  @Test
  void ex00905TagOnNoActiveContactIsNotSuggested() {
    tags.add(groupId, Tag.create(groupId, "udelezenec2025"));
    on(janez, "udeležba");
    jdbc.update(
        "UPDATE contact SET state = 'inRecycleBin', deleted_at = now() WHERE id = ?",
        janez.getId());

    assertThat(tags.suggest(groupId, "udel")).isEmpty();
  }

  @Test
  void ex00904TagOfAnotherGroupIsNotSuggested() {
    UUID beta = groups.save(Group.create("Podjetje Beta", marko, NOW)).getId();
    on(contactOf(beta, "Maja"), "udelezenec2025");

    assertThat(tags.suggest(groupId, "udel")).isEmpty();
  }

  @Test
  void r00902TextIsTakenLiterally() {
    on(janez, "50%_popust");
    on(janez, "500popust");

    assertThat(tags.suggest(groupId, "%_")).extracting(Tag::getName).containsExactly("50%_popust");
  }
}
