package app.contacts.repository;

import app.contacts.domain.Merge;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
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

  /**
   * A Merge of the Group, locked until the transaction ends so that two undos of it run one after
   * the other (EX-019-01).
   *
   * @param groupId the Group
   * @param id the Merge
   * @return the Merge, empty when it is not one of the Group's (BR-02)
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT m FROM Merge m WHERE m.groupId = :groupId AND m.id = :id")
  Optional<Merge> findForUpdate(UUID groupId, UUID id);
}
