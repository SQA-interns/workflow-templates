package app.contacts.domain;

/**
 * An Attribute of a Contact, as its details show it (R-014-06).
 *
 * @param key the key, spelled as entered
 * @param value the value
 */
public record ContactAttributeRow(String key, String value) {}
