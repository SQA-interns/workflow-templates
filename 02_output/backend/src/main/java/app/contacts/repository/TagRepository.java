package app.contacts.repository;

import app.contacts.domain.Tag;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** Tags of one Group (AR-04); a Contact's Tags are read through {@link ContactQueryRepository}. */
public interface TagRepository extends Repository<Tag, UUID>, GroupOwnedStore<Tag> {

  /**
   * The Tag of the Group with this name, compared case-insensitively (R-009-03, BR-11).
   *
   * @param groupId the Group
   * @param name the name
   * @return the Tag, if the Group has one
   */
  @Query("SELECT t FROM Tag t WHERE t.groupId = :groupId AND lower(t.name) = lower(:name)")
  Optional<Tag> findInGroup(UUID groupId, String name);
}
