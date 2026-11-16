package app.contacts.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * One row of the Contact list (R-014-01).
 *
 * @param id the Contact id
 * @param firstName first name, or {@code null}
 * @param lastName last name, or {@code null}
 * @param company company, or {@code null}
 * @param email e-mail, or {@code null}
 * @param phone phone, or {@code null}
 * @param updatedAt time of the last change
 */
public record ContactListRow(
    UUID id,
    String firstName,
    String lastName,
    String company,
    String email,
    String phone,
    Instant updatedAt) {}
