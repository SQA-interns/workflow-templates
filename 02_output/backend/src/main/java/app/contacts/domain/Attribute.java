package app.contacts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** A free key–value pair kept on a Contact (glossary "Attribute", table attribute, US-008). */
@Entity
@Table(name = "attribute")
public class Attribute implements GroupOwned {

  @Id private UUID id;

  @Column(name = "group_id", nullable = false, updatable = false)
  private UUID groupId;

  @Column(name = "contact_id", nullable = false, updatable = false)
  private UUID contactId;

  @Column(nullable = false)
  private String key;

  @Column(nullable = false)
  private String value;

  /** For JPA. */
  protected Attribute() {}

  /**
   * A new Attribute of the Contact, in the Contact's Group (AR-04).
   *
   * @param contact the Contact
   * @param key the checked key, spelled as entered (R-008-06)
   * @param value the checked value
   * @return the Attribute
   */
  public static Attribute of(Contact contact, String key, String value) {
    Attribute attribute = new Attribute();
    attribute.id = UUID.randomUUID();
    attribute.groupId = contact.getGroupId();
    attribute.contactId = contact.getId();
    attribute.key = key;
    attribute.value = value;
    return attribute;
  }

  @Override
  public UUID getGroupId() {
    return groupId;
  }
}
