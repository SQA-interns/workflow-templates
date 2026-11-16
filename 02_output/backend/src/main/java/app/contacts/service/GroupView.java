package app.contacts.service;

import java.util.UUID;

/**
 * A Group as its Member sees it (contract {@code Group}).
 *
 * @param id the Group id
 * @param name the name
 * @param role the caller's Role, e.g. {@code OWNER}
 */
public record GroupView(UUID id, String name, String role) {}
