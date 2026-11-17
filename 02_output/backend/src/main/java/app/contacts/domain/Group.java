package app.contacts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** The tenant: a named set of Contacts and Members (glossary "Group", table contact_group). */
@Entity
@Table(name = "contact_group")
public class Group {

  /** State of a Group that is not in the recycle bin (Data model). */
  public static final String ACTIVE = "active";

  /** State of a Group in the recycle bin (R-003-02). */
  public static final String IN_RECYCLE_BIN = "inRecycleBin";

  @Id private UUID id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String state;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "created_by", nullable = false, updatable = false)
  private UUID createdBy;

  /** For JPA. */
  protected Group() {}

  /**
   * A new active Group (R-002-01).
   *
   * @param name the name, already trimmed and checked
   * @param createdBy the User who creates it
   * @param now the creation time
   * @return the new, unsaved Group
   */
  public static Group create(String name, UUID createdBy, Instant now) {
    Group group = new Group();
    group.id = UUID.randomUUID();
    group.name = name;
    group.state = ACTIVE;
    group.createdAt = now;
    group.createdBy = createdBy;
    return group;
  }

  /**
   * Gives the Group a new name (R-003-01).
   *
   * @param newName the name, already trimmed and checked
   */
  public void rename(String newName) {
    this.name = newName;
  }

  /**
   * Moves the Group to the recycle bin (R-003-02).
   *
   * @param now the time it is moved, its {@code deletedAt}
   */
  public void moveToRecycleBin(Instant now) {
    this.state = IN_RECYCLE_BIN;
    this.deletedAt = now;
  }

  /** Takes the Group out of the recycle bin with everything it had (R-003-05). */
  public void restore() {
    this.state = ACTIVE;
    this.deletedAt = null;
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getState() {
    return state;
  }

  public Instant getDeletedAt() {
    return deletedAt;
  }
}
