package app.contacts.domain;

import java.io.Serializable;
import java.util.UUID;

/**
 * The key of a Tag assignment: its Contact and Tag.
 *
 * @param contactId the Contact
 * @param tagId the Tag
 */
public record ContactTagId(UUID contactId, UUID tagId) implements Serializable {}
