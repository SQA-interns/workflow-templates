package app.contacts.service;

import app.contacts.domain.AuditEntry;
import app.contacts.repository.AuditEntryRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Writes the Audit entries of the actions of BR-10 (SR-04); they have no API (D-19). */
@Service
public class AuditService {

  private final AuditEntryRepository entries;
  private final Clock clock;

  /**
   * The service.
   *
   * @param entries stores the entries
   * @param clock the clock
   */
  public AuditService(AuditEntryRepository entries, Clock clock) {
    this.entries = entries;
    this.clock = clock;
  }

  /**
   * Writes one Audit entry at the current UTC time, in the caller's transaction (BR-10).
   *
   * @param groupId the Group
   * @param userId the User who acted
   * @param action the action, a constant of {@link AuditEntry}
   * @param targetId the id of the item acted on
   */
  public void record(UUID groupId, UUID userId, String action, UUID targetId) {
    entries.add(groupId, AuditEntry.create(groupId, userId, action, targetId, clock.instant()));
  }
}
