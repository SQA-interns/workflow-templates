package app.contacts.repository;

import app.contacts.domain.Tag;
import java.util.List;
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

  /**
   * The Tags of the Group that are on at least one active Contact and whose name contains the text
   * case-insensitively, taken literally; by name A–Z in Slovenian collation (R-009-02, EX-009-05).
   *
   * @param groupId the Group
   * @param text the typed text; empty for all
   * @return the Tags
   */
  @Query(
      nativeQuery = true,
      value =
          """
          SELECT t.* FROM tag t
          WHERE t.group_id = :groupId AND strpos(lower(t.name), lower(:text)) > 0
            AND EXISTS (SELECT 1 FROM contact_tag ct JOIN contact c ON c.id = ct.contact_id
                        WHERE ct.tag_id = t.id AND c.state = 'active')
          ORDER BY t.name COLLATE "sl-x-icu", t.id
          """)
  List<Tag> suggest(UUID groupId, String text);
}
