package app.contacts.service;

import app.contacts.domain.Contact;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.SequencedMap;
import java.util.UUID;

/**
 * A Contact as a Member sees it (contract {@code Contact}).
 *
 * @param id the Contact id
 * @param versionNumber the current Version number
 * @param state {@code active}, {@code inRecycleBin} or {@code merged}
 * @param fields Field name to value in form order; {@code null} for an empty Field
 * @param createdAt when it was added
 * @param updatedAt when it was last changed
 */
public record ContactView(
    UUID id,
    int versionNumber,
    String state,
    SequencedMap<String, String> fields,
    Instant createdAt,
    Instant updatedAt) {

  /**
   * A view with its own copy of the Fields.
   *
   * @param id the Contact id
   * @param versionNumber the current Version number
   * @param state the state
   * @param fields the Fields
   * @param createdAt when it was added
   * @param updatedAt when it was last changed
   */
  public ContactView {
    fields = new LinkedHashMap<>(fields);
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
   * The view of a Contact.
   *
   * @param contact the Contact
   * @return the view
   */
  static ContactView of(Contact contact) {
    return new ContactView(
        contact.getId(),
        contact.getVersionNumber(),
        contact.getState(),
        contact.getFields().byName(),
        contact.getCreatedAt(),
        contact.getUpdatedAt());
  }
}
