package app.contacts.domain;

import java.util.UUID;

/** Data that belongs to one Group (AR-04, ADR-02). */
public interface GroupOwned {

  /**
   * The Group the data belongs to.
   *
   * @return the Group id
   */
  UUID getGroupId();
}
