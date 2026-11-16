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

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getState() {
    return state;
  }
}
