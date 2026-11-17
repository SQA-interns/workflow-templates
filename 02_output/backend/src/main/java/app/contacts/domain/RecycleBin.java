package app.contacts.domain;

import java.time.Duration;
import java.time.Instant;

/** The recycle bin period of Contacts and Groups (project-wide facts, BR-08, D-18, ADR-10). */
public final class RecycleBin {

  /** How long a Contact or Group stays restorable, counted from {@code deletedAt} in UTC. */
  public static final Duration PERIOD = Duration.ofDays(30);

  private RecycleBin() {}

  /**
   * The removal date of something moved to the recycle bin.
   *
   * @param deletedAt when it was moved
   * @return {@code deletedAt} + {@link #PERIOD}
   */
  public static Instant removalAt(Instant deletedAt) {
    return deletedAt.plus(PERIOD);
  }
}
