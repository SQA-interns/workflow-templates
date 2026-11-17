package app.contacts.service;

import app.contacts.domain.RecycleBin;
import app.contacts.repository.ContactRepository;
import app.contacts.repository.GroupRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Permanently deletes what stayed in the recycle bin for the recycle bin period (BR-08, R-012-05,
 * ADR-10); a Contact's period keeps running while its Group is in the recycle bin (D-18).
 */
@Component
public class RemovalJob {

  private final GroupRepository groups;
  private final ContactRepository contacts;
  private final Clock clock;

  /**
   * The job.
   *
   * @param groups lists every Group
   * @param contacts deletes the due Contacts of one Group
   * @param clock the clock
   */
  public RemovalJob(GroupRepository groups, ContactRepository contacts, Clock clock) {
    this.groups = groups;
    this.contacts = contacts;
    this.clock = clock;
  }

  /**
   * Deletes, Group by Group, every Contact moved to the recycle bin at least the recycle bin period
   * ago; runs every 15 minutes (R-012-05: at least hourly; ADR-10).
   */
  @Scheduled(fixedDelayString = "PT15M")
  public void removeDueContacts() {
    Instant cutoff = clock.instant().minus(RecycleBin.PERIOD);
    groups.findAllIds().forEach(groupId -> contacts.permanentlyDeleteDue(groupId, cutoff));
  }
}
