package app.contacts.repository;

import app.contacts.domain.Merge;
import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/** Merges of one Group (AR-04). */
public interface MergeRepository extends Repository<Merge, UUID>, GroupOwnedStore<Merge> {

  /**
   * The Merges into a Primary contact, oldest first, undone ones included (R-018-05).
   *
   * @param groupId the Group
   * @param primaryId the Primary contact
   * @return the Merges
   */
  List<Merge> findByGroupIdAndPrimaryIdOrderByMergedAt(UUID groupId, UUID primaryId);
}
