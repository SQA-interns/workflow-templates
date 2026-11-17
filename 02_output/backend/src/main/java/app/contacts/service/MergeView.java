package app.contacts.service;

import app.contacts.domain.Merge;
import java.time.Instant;
import java.util.UUID;

/**
 * A Merge (contract {@code Merge}).
 *
 * @param id the Merge id
 * @param primaryId the Primary contact
 * @param otherId the Other contact
 * @param mergedAt when it was merged
 * @param undone whether it was undone (US-019)
 */
public record MergeView(UUID id, UUID primaryId, UUID otherId, Instant mergedAt, boolean undone) {

  static MergeView of(Merge merge) {
    return new MergeView(
        merge.getId(),
        merge.getPrimaryId(),
        merge.getMergedId(),
        merge.getMergedAt(),
        merge.isUndone());
  }
}
