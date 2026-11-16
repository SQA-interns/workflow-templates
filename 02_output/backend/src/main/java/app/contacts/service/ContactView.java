package app.contacts.service;

import app.contacts.domain.Contact;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;
import java.util.UUID;

/**
 * A Contact as a Member sees it (contract {@code Contact}).
 *
 * @param id the Contact id
 * @param versionNumber the current Version number
 * @param state {@code active}, {@code inRecycleBin} or {@code merged}
 * @param fields Field name to value in form order; {@code null} for an empty Field
 * @param attributes the Attributes by key A–Z
 * @param tags the Tags by name
 * @param createdAt when it was added
 * @param updatedAt when it was last changed
 */
public record ContactView(
    UUID id,
    int versionNumber,
    String state,
    SequencedMap<String, String> fields,
    List<AttributeView> attributes,
    List<TagView> tags,
    Instant createdAt,
    Instant updatedAt) {

  /**
   * A view with its own copies of the Fields, Attributes and Tags.
   *
   * @param id the Contact id
   * @param versionNumber the current Version number
   * @param state the state
   * @param fields the Fields
   * @param attributes the Attributes
   * @param tags the Tags
   * @param createdAt when it was added
   * @param updatedAt when it was last changed
   */
  public ContactView {
    fields = new LinkedHashMap<>(fields);
    attributes = List.copyOf(attributes);
    tags = List.copyOf(tags);
  }

  /**
   * The Fields in form order.
   *
   * @return a copy of the Fields
   */
  @Override
  public SequencedMap<String, String> fields() {
    return new LinkedHashMap<>(fields);
  }

  /**
   * The view of a Contact with its Attributes and Tags.
   *
   * @param contact the Contact
   * @param attributes its Attributes
   * @param tags its Tags
   * @return the view
   */
  static ContactView of(Contact contact, List<AttributeView> attributes, List<TagView> tags) {
    return new ContactView(
        contact.getId(),
        contact.getVersionNumber(),
        contact.getState(),
        contact.getFields().byName(),
        attributes,
        tags,
        contact.getCreatedAt(),
        contact.getUpdatedAt());
  }
}
