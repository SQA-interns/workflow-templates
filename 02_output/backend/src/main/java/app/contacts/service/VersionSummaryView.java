package app.contacts.service;

import java.time.Instant;
import java.util.List;

/**
 * One entry of a Contact's History (contract {@code VersionSummary}, R-010-01).
 *
 * @param number the Version number
 * @param kind the kind, e.g. {@code created}
 * @param changedBy the display name of the User who made it
 * @param changedAt when it was made
 * @param changes the changed items
 */
public record VersionSummaryView(
    int number, String kind, String changedBy, Instant changedAt, List<ChangeView> changes) {

  /**
   * An entry with its own copy of the changes.
   *
   * @param number the Version number
   * @param kind the kind
   * @param changedBy the display name
   * @param changedAt the time
   * @param changes the changed items
   */
  public VersionSummaryView {
    changes = List.copyOf(changes);
  }
}
