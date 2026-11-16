package app.contacts.service;

import java.util.UUID;

/**
 * A link to a Contact (contract {@code ContactRef}).
 *
 * @param id the Contact id
 * @param name first and last name, else company
 */
public record ContactRefView(UUID id, String name) {}
