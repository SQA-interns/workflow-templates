package app.contacts.web;

import app.contacts.service.AttributeView;
import app.contacts.service.ContactView;
import app.contacts.service.TagView;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;
import java.util.UUID;

/**
 * A Contact (contract {@code Contact}). The Merge links stay empty until US-018 fills them.
 *
 * @param id the Contact id
 * @param versionNumber the current Version number
 * @param state the state
 * @param fields Field name to value in form order, {@code null} for an empty Field
 * @param attributes the Attributes
 * @param tags the Tags
 * @param createdAt when it was added
 * @param updatedAt when it was last changed
 * @param mergedFrom the Contacts merged into this one
 * @param mergedInto the Contact this one was merged into, or {@code null}
 */
public record ContactResponse(
    UUID id,
    int versionNumber,
    String state,
    SequencedMap<String, String> fields,
    List<AttributeView> attributes,
    List<TagView> tags,
    Instant createdAt,
    Instant updatedAt,
    List<Object> mergedFrom,
    ContactRefResponse mergedInto) {

  /**
   * A response with its own copy of the Fields.
   *
   * @param id the Contact id
   * @param versionNumber the current Version number
   * @param state the state
   * @param fields the Fields
   * @param attributes the Attributes
   * @param tags the Tags
   * @param createdAt when it was added
   * @param updatedAt when it was last changed
   * @param mergedFrom the Contacts merged into this one
   * @param mergedInto the Contact this one was merged into
   */
  public ContactResponse {
    fields = new LinkedHashMap<>(fields);
    attributes = List.copyOf(attributes);
    tags = List.copyOf(tags);
    mergedFrom = List.copyOf(mergedFrom);
  }

  static ContactResponse of(ContactView contact) {
    return new ContactResponse(
        contact.id(),
        contact.versionNumber(),
        contact.state(),
        contact.fields(),
        contact.attributes(),
        contact.tags(),
        contact.createdAt(),
        contact.updatedAt(),
        List.of(),
        null);
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
}
