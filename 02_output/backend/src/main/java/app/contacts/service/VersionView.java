package app.contacts.service;

import java.time.Instant;
import java.util.List;

/**
 * One Version with the full content after it (contract {@code Version}, R-010-02).
 *
 * @param number the Version number
 * @param kind the kind, e.g. {@code created}
 * @param changedBy the display name of the User who made it
 * @param changedAt when it was made
 * @param changes the changed items
 * @param snapshot the content of the Contact after it
 */
public record VersionView(
    int number,
    String kind,
    String changedBy,
    Instant changedAt,
    List<ChangeView> changes,
    SnapshotView snapshot) {

  /**
   * A Version with its own copy of the changes.
   *
   * @param number the Version number
   * @param kind the kind
   * @param changedBy the display name
   * @param changedAt the time
   * @param changes the changed items
   * @param snapshot the content
   */
  public VersionView {
    changes = List.copyOf(changes);
  }
}
