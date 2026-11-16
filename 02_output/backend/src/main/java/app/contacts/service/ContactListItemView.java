package app.contacts.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * One row of the Contact list (contract {@code ContactListItem}, R-014-01).
 *
 * @param id the Contact id
 * @param firstName first name, or {@code null}
 * @param lastName last name, or {@code null}
 * @param company company, or {@code null}
 * @param email e-mail, or {@code null}
 * @param phone phone, or {@code null}
 * @param tags the Tags by name
 * @param updatedAt time of the last change
 */
public record ContactListItemView(
    UUID id,
    String firstName,
    String lastName,
    String company,
    String email,
    String phone,
    List<TagView> tags,
    Instant updatedAt) {

  /**
   * A row with its own copy of the Tags.
   *
   * @param id the Contact id
   * @param firstName first name
   * @param lastName last name
   * @param company company
   * @param email e-mail
   * @param phone phone
   * @param tags the Tags
   * @param updatedAt time of the last change
   */
  public ContactListItemView {
    tags = List.copyOf(tags);
  }
}
