package app.contacts.repository;

import app.contacts.domain.ContactVersion;
import app.contacts.domain.ContactVersionId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** Versions of the Contacts of one Group; never updated or deleted on their own (ADR-07). */
public interface ContactVersionRepository
    extends Repository<ContactVersion, ContactVersionId>, GroupOwnedStore<ContactVersion> {

  /**
   * The History of a Contact of the Group (R-010-01).
   *
   * @param groupId the Group the caller works in
   * @param contactId the Contact
   * @return its Versions, newest first; none for a Contact of another Group
   */
  @Query(
      "SELECT v FROM ContactVersion v WHERE v.groupId = :groupId AND v.contactId = :contactId"
          + " ORDER BY v.number DESC")
  List<ContactVersion> findHistory(UUID groupId, UUID contactId);

  /**
   * One Version of a Contact of the Group (R-010-02).
   *
   * @param groupId the Group the caller works in
   * @param contactId the Contact
   * @param number the Version number
   * @return the Version, if the Contact of the Group has it
   */
  @Query(
      "SELECT v FROM ContactVersion v WHERE v.groupId = :groupId AND v.contactId = :contactId"
          + " AND v.number = :number")
  Optional<ContactVersion> findVersion(UUID groupId, UUID contactId, int number);
}
