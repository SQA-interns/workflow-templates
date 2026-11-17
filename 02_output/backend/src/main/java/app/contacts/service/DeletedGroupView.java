package app.contacts.service;

import java.time.Instant;
import java.util.UUID;

/**
 * A Group in the Owner's "Deleted Groups" (contract {@code DeletedGroup}, R-003-04).
 *
 * @param id the Group id
 * @param name the name
 * @param deletedAt when it was moved to the recycle bin
 * @param removalAt {@code deletedAt} + the recycle bin period; restorable only before it
 */
public record DeletedGroupView(UUID id, String name, Instant deletedAt, Instant removalAt) {}
