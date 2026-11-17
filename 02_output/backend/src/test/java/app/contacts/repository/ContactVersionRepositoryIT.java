package app.contacts.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import app.contacts.domain.Contact;
import app.contacts.domain.ContactFields;
import app.contacts.domain.ContactVersion;
import app.contacts.domain.Group;
import app.contacts.domain.User;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** The Versions of a Contact in PostgreSQL (R-010-01, R-010-02, AR-04). */
@DataJpaTest(properties = "spring.flyway.enabled=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ContactVersionRepositoryIT {

  @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16.15");

  static {
    POSTGRES.start();
  }

  private static final Instant CREATED_AT = Instant.parse("2026-03-05T09:00:00Z");
  private static final Instant EDITED_AT = Instant.parse("2026-07-01T08:00:00Z");

  @Autowired private ContactVersionRepository versions;
  @Autowired private ContactRepository contacts;
  @Autowired private GroupRepository groups;
  @Autowired private UserRepository users;

  private UUID ana;
  private UUID groupId;
  private Contact janez;

  @BeforeEach
  void janezWithTwoVersions() {
    ana =
        users.save(User.create("uid-ana", "ana.kovac@example.si", "Ana Kovač", CREATED_AT)).getId();
    groupId = groups.save(Group.create("Konferenca 2025", ana, CREATED_AT)).getId();
    ContactFields fields = ContactFields.ofNames(Map.of("firstName", "Janez"));
    janez =
        contacts.add(groupId, Contact.create(groupId, fields, "janez", "janez", ana, CREATED_AT));
    versions.add(groupId, ContactVersion.of(janez, ContactVersion.CREATED, "[]", "{}"));
    janez.edit(fields, "janez", "janez", ana, EDITED_AT);
    versions.add(groupId, ContactVersion.of(janez, ContactVersion.EDITED, "[]", "{}"));
  }

  @Test
  void r01001FindsAllVersionsOfTheContactNewestFirst() {
    assertThat(versions.findHistory(groupId, janez.getId()))
        .extracting(ContactVersion::getNumber, ContactVersion::getKind)
        .containsExactly(tuple(2, "edited"), tuple(1, "created"));
  }

  @Test
  void r01002FindsOneVersionByNumber() {
    assertThat(versions.findVersion(groupId, janez.getId(), 1))
        .map(ContactVersion::getKind)
        .contains("created");
    assertThat(versions.findVersion(groupId, janez.getId(), 99)).isEmpty();
  }

  @Test
  void ac01003VersionsAreNotFoundThroughAnotherGroup() {
    UUID beta = groups.save(Group.create("Podjetje Beta", ana, CREATED_AT)).getId();

    assertThat(versions.findHistory(beta, janez.getId())).isEmpty();
    assertThat(versions.findVersion(beta, janez.getId(), 1)).isEmpty();
  }
}
