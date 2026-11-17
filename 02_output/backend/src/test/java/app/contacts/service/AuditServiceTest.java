package app.contacts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import app.contacts.domain.AuditEntry;
import app.contacts.repository.AuditEntryRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AuditServiceTest {

  private static final Instant NOW = Instant.parse("2026-03-06T08:00:00Z");
  private static final UUID GROUP_ID = UUID.randomUUID();
  private static final UUID ANA = UUID.randomUUID();
  private static final UUID CONTACT_ID = UUID.randomUUID();

  private final AuditEntryRepository entries = mock(AuditEntryRepository.class);
  private final AuditService audit = new AuditService(entries, Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  void br10WritesOneEntryWithTheUserTheActionTheTargetAndTheUtcTime() {
    audit.record(GROUP_ID, ANA, AuditEntry.CONTACT_PERMANENTLY_DELETED, CONTACT_ID);

    ArgumentCaptor<AuditEntry> written = ArgumentCaptor.forClass(AuditEntry.class);
    verify(entries).add(eq(GROUP_ID), written.capture());
    AuditEntry entry = written.getValue();
    assertThat(entry.getId()).isNotNull();
    assertThat(entry.getGroupId()).isEqualTo(GROUP_ID);
    assertThat(entry.getUserId()).isEqualTo(ANA);
    assertThat(entry.getAction()).isEqualTo("contact.permanentlyDeleted");
    assertThat(entry.getTargetId()).isEqualTo(CONTACT_ID);
    assertThat(entry.getAt()).isEqualTo(NOW);
  }
}
