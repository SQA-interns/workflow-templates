package app.contacts.domain;

import java.util.UUID;

/**
 * A Tag of a Contact, as the Contact list shows it (R-014-01).
 *
 * @param contactId the Contact
 * @param tagId the Tag
 * @param name the Tag name
 */
public record ContactTagRow(UUID contactId, UUID tagId, String name) {}
