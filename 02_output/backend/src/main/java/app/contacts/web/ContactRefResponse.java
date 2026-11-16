package app.contacts.web;

import java.util.UUID;

/**
 * A link to a Contact (contract {@code ContactRef}).
 *
 * @param id the Contact id
 * @param name first and last name, else company
 */
public record ContactRefResponse(UUID id, String name) {}
