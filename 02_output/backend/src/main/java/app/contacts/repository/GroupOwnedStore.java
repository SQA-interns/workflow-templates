package app.contacts.repository;

import app.contacts.domain.GroupOwned;
import java.util.UUID;

/**
 * Adds data of one Group; shared by the Group-owned repositories in place of a generic {@code save}
 * (AR-04).
 *
 * @param <T> the Group-owned entity
 */
public interface GroupOwnedStore<T extends GroupOwned> {

  /**
   * Stores new data of the Group.
   *
   * @param groupId the Group the caller works in
   * @param item the new item
   * @return the stored item
   * @throws org.springframework.dao.InvalidDataAccessApiUsageException when the item belongs to
   *     another Group
   */
  T add(UUID groupId, T item);
}
