package app.contacts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;

/** A Tag put on a Contact (table contact_tag, US-009); at most once per Contact (R-009-06). */
@Entity
@Table(name = "contact_tag")
@IdClass(ContactTagId.class)
public class ContactTag implements GroupOwned {

  @Id
  @Column(name = "contact_id")
  private UUID contactId;

  @Id
  @Column(name = "tag_id")
  private UUID tagId;

  @Column(name = "group_id", nullable = false, updatable = false)
  private UUID groupId;

  /** For JPA. */
  protected ContactTag() {}

  /**
   * The Tag put on the Contact, in the Contact's Group (AR-04).
   *
   * @param contact the Contact
   * @param tag a Tag of the Contact's Group
   * @return the assignment
   */
  public static ContactTag of(Contact contact, Tag tag) {
    ContactTag assignment = new ContactTag();
    assignment.contactId = contact.getId();
    assignment.tagId = tag.getId();
    assignment.groupId = contact.getGroupId();
    return assignment;
  }

  @Override
  public UUID getGroupId() {
    return groupId;
  }
}
