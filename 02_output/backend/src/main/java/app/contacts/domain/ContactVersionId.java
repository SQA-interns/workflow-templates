package app.contacts.domain;

import java.io.Serializable;
import java.util.UUID;

/**
 * The key of a Version: its Contact and number (Data model).
 *
 * @param contactId the Contact
 * @param number the Version number
 */
public record ContactVersionId(UUID contactId, int number) implements Serializable {}
