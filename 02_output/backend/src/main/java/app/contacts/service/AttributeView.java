package app.contacts.service;

/**
 * An Attribute of a Contact (contract {@code Attribute}).
 *
 * @param key the key
 * @param value the value
 */
public record AttributeView(String key, String value) {}
