package app.contacts.service;

import java.time.Instant;

/**
 * A Contact in the recycle bin (contract {@code RecycleBinEntry}, R-012-02).
 *
 * @param contact the Contact
 * @param versionNumber its current Version number, sent back to restore it (BR-06)
 * @param deletedBy the display name of the User who moved it
 * @param deletedAt when it was moved
 * @param removalAt its removal date: {@code deletedAt} + the recycle bin period
 */
public record RecycleBinEntryView(
    ContactRefView contact,
    int versionNumber,
    String deletedBy,
    Instant deletedAt,
    Instant removalAt) {}
