package app.contacts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Combining a Possible duplicate pair into the Primary contact (glossary "Merge", table
 * contact_merge; {@code merge} is an SQL keyword).
 */
@Entity
@Table(name = "contact_merge")
public class Merge implements GroupOwned {

  @Id private UUID id;

  @Column(name = "group_id", nullable = false, updatable = false)
  private UUID groupId;

  @Column(name = "primary_id", nullable = false, updatable = false)
  private UUID primaryId;

  @Column(name = "merged_id", nullable = false, updatable = false)
  private UUID mergedId;

  @Column(name = "merged_by", nullable = false, updatable = false)
  private UUID mergedBy;

  @Column(name = "merged_at", nullable = false, updatable = false)
  private Instant mergedAt;

  @Column(name = "undone_at")
  private Instant undoneAt;

  @Column(name = "undone_by")
  private UUID undoneBy;

  /** For JPA. */
  protected Merge() {}

  /**
   * The Merge of the Other contact into the Primary contact (R-018-04).
   *
   * @param primary the Primary contact
   * @param other the Other contact
   * @param userId the User who merges
   * @param now the time
   * @return the new, unsaved Merge
   */
  public static Merge create(Contact primary, Contact other, UUID userId, Instant now) {
    Merge merge = new Merge();
    merge.id = UUID.randomUUID();
    merge.groupId = primary.getGroupId();
    merge.primaryId = primary.getId();
    merge.mergedId = other.getId();
    merge.mergedBy = userId;
    merge.mergedAt = now;
    return merge;
  }

  /**
   * Marks the Merge undone (R-019-01).
   *
   * @param userId the User who undoes it
   * @param now the time
   */
  public void undo(UUID userId, Instant now) {
    this.undoneAt = now;
    this.undoneBy = userId;
  }

  public UUID getId() {
    return id;
  }

  @Override
  public UUID getGroupId() {
    return groupId;
  }

  public UUID getPrimaryId() {
    return primaryId;
  }

  /**
   * The Other contact, merged into the Primary contact.
   *
   * @return its id
   */
  public UUID getMergedId() {
    return mergedId;
  }

  public Instant getMergedAt() {
    return mergedAt;
  }

  /**
   * Whether the Merge was undone (US-019).
   *
   * @return true once undone
   */
  public boolean isUndone() {
    return undoneAt != null;
  }
}
