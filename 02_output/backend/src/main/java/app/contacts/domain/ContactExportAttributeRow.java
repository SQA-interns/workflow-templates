package app.contacts.domain;

import java.util.UUID;

/**
 * An Attribute of a Contact of the Export scope (R-016-03).
 *
 * @param contactId the Contact
 * @param key the key
 * @param value the value
 */
public record ContactExportAttributeRow(UUID contactId, String key, String value) {}
