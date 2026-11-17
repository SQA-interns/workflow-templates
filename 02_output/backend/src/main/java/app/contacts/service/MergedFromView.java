package app.contacts.service;

import java.util.UUID;

/**
 * A Contact merged into the Primary contact, for "Merged from" (R-018-05; contract {@code
 * Contact.mergedFrom}).
 *
 * @param mergeId the Merge
 * @param contact the Other contact
 * @param undone whether the Merge was undone (US-019)
 */
public record MergedFromView(UUID mergeId, ContactRefView contact, boolean undone) {}
