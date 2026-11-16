package app.contacts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** A free label of a Group put on Contacts (glossary "Tag", table tag, US-009). */
@Entity
@Table(name = "tag")
public class Tag implements GroupOwned {

  @Id private UUID id;

  @Column(name = "group_id", nullable = false, updatable = false)
  private UUID groupId;

  @Column(nullable = false)
  private String name;

  /** For JPA. */
  protected Tag() {}

  /**
   * A new Tag of the Group (R-009-04).
   *
   * @param groupId the Group
   * @param name the checked name, spelled as entered
   * @return the Tag
   */
  public static Tag create(UUID groupId, String name) {
    Tag tag = new Tag();
    tag.id = UUID.randomUUID();
    tag.groupId = groupId;
    tag.name = name;
    return tag;
  }

  /**
   * The id.
   *
   * @return the Tag id
   */
  public UUID getId() {
    return id;
  }

  /**
   * The name in the spelling it was created with (R-009-03).
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  @Override
  public UUID getGroupId() {
    return groupId;
  }
}
