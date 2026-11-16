package app.contacts.repository;

import app.contacts.domain.GroupOwned;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.UUID;

/**
 * JPA implementation of {@link GroupOwnedStore}.
 *
 * @param <T> the Group-owned entity
 */
class GroupOwnedStoreImpl<T extends GroupOwned> implements GroupOwnedStore<T> {

  @PersistenceContext private EntityManager entityManager;

  @Override
  public T add(UUID groupId, T item) {
    if (!groupId.equals(item.getGroupId())) {
      throw new IllegalArgumentException("item of another Group");
    }
    entityManager.persist(item);
    return item;
  }
}
