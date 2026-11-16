package app.contacts.repository;

import static org.assertj.core.api.Assertions.assertThat;

import app.contacts.domain.Contact;
import app.contacts.domain.ContactFields;
import app.contacts.domain.ContactListRow;
import app.contacts.domain.ContactSort;
import app.contacts.domain.ContactTagRow;
import app.contacts.domain.Group;
import app.contacts.domain.User;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
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

/** The Contact list query in PostgreSQL: one Group, active only, sorted, searched, paged. */
@DataJpaTest(properties = "spring.flyway.enabled=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ContactQueryRepository.class)
class ContactQueryRepositoryIT {

  @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16.15");

  static {
    POSTGRES.start();
  }

  private static final Instant NOW = Instant.parse("2026-03-05T10:00:00Z");
  private static final String ALL = "%";

  @Autowired private ContactQueryRepository query;
  @Autowired private ContactRepository contacts;
  @Autowired private GroupRepository groups;
  @Autowired private UserRepository users;
  @Autowired private EntityManager entityManager;
  @Autowired private JdbcTemplate jdbc;

  private UUID ana;
  private UUID groupId;

  @BeforeEach
  void group() {
    ana = users.save(User.create("uid-ana", "ana.kovac@example.si", "Ana Kovač", NOW)).getId();
    groupId = groups.save(Group.create("Konferenca 2025", ana, NOW)).getId();
  }

  private Contact add(UUID group, Map<String, String> values, String searchText, Instant at) {
    ContactFields fields = ContactFields.ofNames(values);
    String sortName =
        values.getOrDefault("lastName", values.getOrDefault("firstName", values.get("company")));
    Contact contact = Contact.create(group, fields, searchText, sortName, ana, at);
    contacts.add(group, contact);
    return contact;
  }

  /** A person; the search text Normalised as the Normaliser does (specification §3). */
  private Contact add(String first, String last) {
    String normalised =
        (first + " " + last)
            .toLowerCase(Locale.ROOT)
            .replace('č', 'c')
            .replace('š', 's')
            .replace('ž', 'z');
    return add(groupId, Map.of("firstName", first, "lastName", last), normalised, NOW);
  }

  private List<String> names(List<ContactListRow> rows) {
    return rows.stream()
        .map(
            row ->
                row.lastName() == null
                    ? (row.firstName() == null ? row.company() : row.firstName())
                    : row.firstName() + " " + row.lastName())
        .toList();
  }

  private void moveToRecycleBin(Contact contact) {
    entityManager.flush();
    jdbc.update(
        "UPDATE contact SET state = 'inRecycleBin', deleted_at = now() WHERE id = ?",
        contact.getId());
  }

  @Test
  void ac01401ActiveContactsByLastNameInSlovenianOrder() {
    add("Janez", "Novak");
    add("Maja", "Zupan");
    add("Žiga", "Čuk");
    moveToRecycleBin(add("Ana", "Bizjak"));

    List<ContactListRow> page = query.findPage(groupId, ALL, ContactSort.NAME, false, 0, 50);

    assertThat(names(page)).containsExactly("Žiga Čuk", "Janez Novak", "Maja Zupan");
    assertThat(query.count(groupId, ALL)).isEqualTo(3);
  }

  @Test
  void ex01404OnlyCompanySortsByCompanyInTheNamePosition() {
    add("Janez", "Novak");
    add(groupId, Map.of("company", "Alfa d.o.o."), "alfa d.o.o.", NOW);

    assertThat(names(query.findPage(groupId, ALL, ContactSort.NAME, false, 0, 50)))
        .containsExactly("Alfa d.o.o.", "Janez Novak");
  }

  @Test
  void r01403NameDescendingReversesTheOrder() {
    add("Janez", "Novak");
    add("Žiga", "Čuk");

    assertThat(names(query.findPage(groupId, ALL, ContactSort.NAME, true, 0, 50)))
        .containsExactly("Janez Novak", "Žiga Čuk");
  }

  @Test
  void r01403SortsByCompanyEmailAndLastChanged() {
    Contact beta =
        add(
            groupId,
            Map.of("firstName", "Maja", "lastName", "Zupan", "company", "Beta", "email", "a@x.si"),
            "maja",
            NOW.plusSeconds(60));
    Contact alfa =
        add(
            groupId,
            Map.of("firstName", "Janez", "lastName", "Novak", "company", "Alfa", "email", "b@x.si"),
            "janez",
            NOW);

    assertThat(query.findPage(groupId, ALL, ContactSort.COMPANY, false, 0, 50))
        .extracting(ContactListRow::id)
        .containsExactly(alfa.getId(), beta.getId());
    assertThat(query.findPage(groupId, ALL, ContactSort.EMAIL, false, 0, 50))
        .extracting(ContactListRow::id)
        .containsExactly(beta.getId(), alfa.getId());
    assertThat(query.findPage(groupId, ALL, ContactSort.UPDATED_AT, true, 0, 50))
        .extracting(ContactListRow::id)
        .containsExactly(beta.getId(), alfa.getId());
  }

  @Test
  void r01404SearchMatchesThePatternInTheSearchText() {
    add("Janez", "Novak");
    add("Žiga", "Čuk");

    assertThat(names(query.findPage(groupId, "%cuk%", ContactSort.NAME, false, 0, 50)))
        .containsExactly("Žiga Čuk");
    assertThat(query.count(groupId, "%cuk%")).isEqualTo(1);
  }

  @Test
  void ex01405EscapedWildcardsMatchLiterally() {
    add(groupId, Map.of("company", "Bio 100% d.o.o."), "bio 100% d.o.o.", NOW);
    add(groupId, Map.of("company", "Bio 100 d.o.o."), "bio 100 d.o.o.", NOW);

    assertThat(names(query.findPage(groupId, "%100\\%%", ContactSort.NAME, false, 0, 50)))
        .containsExactly("Bio 100% d.o.o.");
  }

  @Test
  void ac01405OffsetAndLimitGiveOnePage() {
    for (int i = 1; i <= 120; i++) {
      add("Oseba", String.format("Priimek%03d", i));
    }

    List<ContactListRow> third = query.findPage(groupId, ALL, ContactSort.NAME, false, 100, 50);

    assertThat(third).hasSize(20);
    assertThat(names(third).get(0)).isEqualTo("Oseba Priimek101");
    assertThat(query.findPage(groupId, ALL, ContactSort.NAME, false, 400, 50)).isEmpty();
  }

  @Test
  void ex01407ContactsOfAnotherGroupAreNeverListed() {
    add("Janez", "Novak");
    UUID beta = groups.save(Group.create("Podjetje Beta", ana, NOW)).getId();
    add(beta, Map.of("firstName", "Peter", "lastName", "Novak"), "peter novak", NOW);

    assertThat(names(query.findPage(groupId, "%novak%", ContactSort.NAME, false, 0, 50)))
        .containsExactly("Janez Novak");
    assertThat(query.count(groupId, "%novak%")).isEqualTo(1);
  }

  @Test
  void r01401ListsTheTagsOfTheContactsOnThePage() {
    Contact janez = add("Janez", "Novak");
    entityManager.flush();
    UUID tag = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO tag (id, group_id, name) VALUES (?, ?, 'udelezenec2025')", tag, groupId);
    jdbc.update(
        "INSERT INTO contact_tag (group_id, contact_id, tag_id) VALUES (?, ?, ?)",
        groupId,
        janez.getId(),
        tag);

    assertThat(query.findTags(groupId, List.of(janez.getId())))
        .containsExactly(new ContactTagRow(janez.getId(), tag, "udelezenec2025"));
    assertThat(query.findTags(groupId, List.of())).isEmpty();
  }

  @Test
  void r01401RowsCarryTheListColumns() {
    add(
        groupId,
        Map.of(
            "firstName", "Janez",
            "lastName", "Novak",
            "company", "Podjetje d.o.o.",
            "email", "janez.novak@example.si",
            "phone", "+386 41 123 456"),
        "janez",
        NOW);

    ContactListRow row = query.findPage(groupId, ALL, ContactSort.NAME, false, 0, 50).get(0);

    assertThat(row.company()).isEqualTo("Podjetje d.o.o.");
    assertThat(row.email()).isEqualTo("janez.novak@example.si");
    assertThat(row.phone()).isEqualTo("+386 41 123 456");
    assertThat(row.updatedAt()).isEqualTo(NOW);
  }
}
