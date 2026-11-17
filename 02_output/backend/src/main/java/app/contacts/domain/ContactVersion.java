package app.contacts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * The state of a Contact after one saved change (glossary "Version", table contact_version,
 * ADR-07).
 */
@Entity
@Table(name = "contact_version")
@IdClass(ContactVersionId.class)
public class ContactVersion implements GroupOwned {

  /** Kind of the Version that adds a Contact (R-006-01). */
  public static final String CREATED = "created";

  /** Kind of the Version that saves edited Fields (R-007-01). */
  public static final String EDITED = "edited";

  /** Kind of the Version that makes an earlier Version current again (R-011-01). */
  public static final String RESTORED = "restored";

  /** Kind of the Version that moves a Contact to the recycle bin (R-012-01). */
  public static final String MOVED_TO_RECYCLE_BIN = "movedToRecycleBin";

  @Id
  @Column(name = "contact_id")
  private UUID contactId;

  @Id private int number;

  @Column(name = "group_id", nullable = false)
  private UUID groupId;

  @Column(nullable = false)
  private String kind;

  @Column(name = "changed_by", nullable = false)
  private UUID changedBy;

  @Column(name = "changed_at", nullable = false)
  private Instant changedAt;

  @Column(name = "restored_from")
  private Integer restoredFrom;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false)
  private String changes;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false)
  private String snapshot;

  /** For JPA. */
  protected ContactVersion() {}

  /**
   * The Version of the Contact's current Version number, by the User and at the time of its last
   * change (BR-05).
   *
   * @param contact the Contact after the change
   * @param kind the kind, e.g. {@link #CREATED}
   * @param changes JSON list of the changed items (item, oldValue, newValue)
   * @param snapshot JSON of the full content after the change (fields, attributes, tags)
   * @return the new, unsaved Version
   */
  public static ContactVersion of(Contact contact, String kind, String changes, String snapshot) {
    ContactVersion version = new ContactVersion();
    version.contactId = contact.getId();
    version.number = contact.getVersionNumber();
    version.groupId = contact.getGroupId();
    version.kind = kind;
    version.changedBy = contact.getUpdatedBy();
    version.changedAt = contact.getUpdatedAt();
    version.changes = changes;
    version.snapshot = snapshot;
    return version;
  }

  /**
   * The Version {@link #RESTORED} of the Contact's current Version number that names the restored
   * Version (R-011-01).
   *
   * @param contact the Contact after the restore
   * @param restoredFrom the number of the restored Version
   * @param changes JSON list of the changed items (item, oldValue, newValue)
   * @param snapshot JSON of the full content after the restore
   * @return the new, unsaved Version
   */
  public static ContactVersion restored(
      Contact contact, int restoredFrom, String changes, String snapshot) {
    ContactVersion version = of(contact, RESTORED, changes, snapshot);
    version.restoredFrom = restoredFrom;
    return version;
  }

  @Override
  public UUID getGroupId() {
    return groupId;
  }

  public UUID getChangedBy() {
    return changedBy;
  }

  public Instant getChangedAt() {
    return changedAt;
  }

  public String getKind() {
    return kind;
  }

  public int getNumber() {
    return number;
  }

  /**
   * The number of the Version a {@link #RESTORED} Version made current again.
   *
   * @return the number; {@code null} for every other kind
   */
  public Integer getRestoredFrom() {
    return restoredFrom;
  }

  public String getChanges() {
    return changes;
  }

  public String getSnapshot() {
    return snapshot;
  }
}
