package app.contacts.service;

import java.util.UUID;

/**
 * A Tag (contract {@code Tag}).
 *
 * @param id the Tag id
 * @param name the name
 */
public record TagView(UUID id, String name) {}
