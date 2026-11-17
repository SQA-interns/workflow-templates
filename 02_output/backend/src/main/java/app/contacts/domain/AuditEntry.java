package app.contacts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A record of a sensitive action (glossary "Audit entry", table audit_entry, BR-10): ids, the
 * action and the UTC time only, never data of the target (R-013-02, SR-06); never changed (SR-04).
 */
@Entity
@Table(name = "audit_entry")
public class AuditEntry implements GroupOwned {

  /** The Owner permanently deleted a Contact from the recycle bin (R-013-02). */
  public static final String CONTACT_PERMANENTLY_DELETED = "contact.permanentlyDeleted";

  /**
   * A Contact was removed after the recycle bin period (BR-08, ADR-10); the User is the one who
   * moved it to the recycle bin (BR-07).
   */
  public static final String CONTACT_REMOVED = "contact.removed";

  @Id private UUID id;

  @Column(name = "group_id", nullable = false, updatable = false)
  private UUID groupId;

  @Column(name = "user_id", nullable = false, updatable = false)
  private UUID userId;

  @Column(nullable = false, updatable = false)
  private String action;

  @Column(name = "target_id", nullable = false, updatable = false)
  private UUID targetId;

  @Column(nullable = false, updatable = false)
  private Instant at;

  /** For JPA. */
  protected AuditEntry() {}

  /**
   * A new Audit entry.
   *
   * @param groupId the Group
   * @param userId the User who acted
   * @param action the action, one of the constants of this class
   * @param targetId the id of the item acted on
   * @param at the time
   * @return the unsaved entry
   */
  public static AuditEntry create(
      UUID groupId, UUID userId, String action, UUID targetId, Instant at) {
    AuditEntry entry = new AuditEntry();
    entry.id = UUID.randomUUID();
    entry.groupId = groupId;
    entry.userId = userId;
    entry.action = action;
    entry.targetId = targetId;
    entry.at = at;
    return entry;
  }

  public UUID getId() {
    return id;
  }

  @Override
  public UUID getGroupId() {
    return groupId;
  }

  public UUID getUserId() {
    return userId;
  }

  public String getAction() {
    return action;
  }

  public UUID getTargetId() {
    return targetId;
  }

  public Instant getAt() {
    return at;
  }
}
