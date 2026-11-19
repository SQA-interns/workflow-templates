package app.contacts.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * NFR-05 (specification §8): the removal job deletes 10,000 Contacts whose recycle bin period has
 * passed in under a minute and leaves none of their rows (R-012-05, BR-08), together with a Contact
 * merged into one of them (D-21), each with an Audit entry (SR-06).
 */
@SpringBootTest(
    properties = {
      "DB_URL=jdbc:postgresql://replaced-by-service-connection/contacts",
      "DB_USER=unused",
      "DB_PASSWORD=unused",
      "APP_PUBLIC_URL=http://localhost:8080",
      "FIREBASE_PROJECT_ID=demo-contacts"
    })
class RemovalJobIT {

  @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16.15");

  static {
    POSTGRES.start();
  }

  private static final Logger LOG = LoggerFactory.getLogger(RemovalJobIT.class);
  private static final int DUE = 10_000;

  @Autowired private RemovalJob job;
  @Autowired private JdbcTemplate jdbc;

  @Test
  void nfr05RemovesTenThousandDueContactsInUnderOneMinute() {
    UUID user = UUID.randomUUID();
    UUID group = UUID.randomUUID();
    UUID tag = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO app_user (id, firebase_uid, email, display_name, created_at)"
            + " VALUES (?, ?, 'ana.kovac@example.si', 'Ana Kovač', now())",
        user,
        "uid-" + user);
    jdbc.update(
        "INSERT INTO contact_group (id, name, state, created_at, created_by)"
            + " VALUES (?, 'Konferenca 2025', 'active', now(), ?)",
        group,
        user);
    jdbc.update("INSERT INTO tag (id, group_id, name) VALUES (?, ?, 'udelezenec2025')", tag, group);
    // 10,000 Contacts moved to the recycle bin 31 days ago, one moved 29 days ago (not yet due)
    jdbc.update(contactsSql("inRecycleBin", "now() - interval '31 days'", DUE), group, user, user);
    jdbc.update(contactsSql("inRecycleBin", "now() - interval '29 days'", 1), group, user, user);
    UUID primary =
        jdbc.queryForObject(
            "SELECT id FROM contact WHERE deleted_at < now() - interval '30 days' LIMIT 1",
            UUID.class);
    UUID merged = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO contact (id, group_id, last_name, state, merged_into, version_number,"
            + " search_text, sort_name, created_at, created_by, updated_at, updated_by)"
            + " VALUES (?, ?, 'Novak', 'merged', ?, 1, 'novak', 'novak', now(), ?, now(), ?)",
        merged,
        group,
        primary,
        user,
        user);
    jdbc.update(
        "INSERT INTO contact_version (group_id, contact_id, number, kind, changed_by, changed_at,"
            + " changes, snapshot)"
            + " SELECT group_id, id, 1, 'created', created_by, created_at, '[]', '{}'"
            + " FROM contact");
    jdbc.update(
        "INSERT INTO attribute (id, group_id, contact_id, key, value)"
            + " SELECT gen_random_uuid(), group_id, id, 'kompetenca', 'spletnoProgramiranje'"
            + " FROM contact");
    jdbc.update(
        "INSERT INTO contact_tag (group_id, contact_id, tag_id)"
            + " SELECT group_id, id, ? FROM contact",
        tag);

    long start = System.nanoTime();
    job.removeDueContacts();
    Duration took = Duration.ofNanos(System.nanoTime() - start);

    LOG.info("NFR-05 removal job took {}", took);
    assertThat(took).as("NFR-05 removal job took %s", took).isLessThan(Duration.ofSeconds(60));
    assertThat(count("SELECT count(*) FROM contact")).isEqualTo(1);
    assertThat(count("SELECT count(*) FROM contact WHERE deleted_at > now() - interval '30 days'"))
        .isEqualTo(1);
    assertThat(count("SELECT count(*) FROM contact_version")).isEqualTo(1);
    assertThat(count("SELECT count(*) FROM attribute")).isEqualTo(1);
    assertThat(count("SELECT count(*) FROM contact_tag")).isEqualTo(1);
    assertThat(count("SELECT count(*) FROM audit_entry WHERE action = 'contact.removed'"))
        .isEqualTo(DUE + 1);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM audit_entry WHERE target_id = ? AND user_id = ?",
                Long.class,
                merged,
                user))
        .isEqualTo(1);
  }

  private static String contactsSql(String state, String deletedAt, int rows) {
    return "INSERT INTO contact (id, group_id, first_name, last_name, email, state, deleted_at,"
        + " version_number, search_text, sort_name, created_at, created_by, updated_at, updated_by)"
        + " SELECT gen_random_uuid(), ?, 'Oseba' || n, 'Priimek' || n,"
        + " 'oseba' || n || '@example.si', '"
        + state
        + "', "
        + deletedAt
        + ", 1, 'oseba' || n || ' priimek' || n, 'priimek' || n, now(), ?, now(), ?"
        + " FROM generate_series(1, "
        + rows
        + ") AS n";
  }

  private long count(String sql) {
    Long n = jdbc.queryForObject(sql, Long.class);
    return n == null ? 0 : n;
  }
}
