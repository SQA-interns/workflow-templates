package app.contacts.repository;

import app.contacts.domain.ContactTag;
import app.contacts.domain.ContactTagId;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** The Tags put on the Contacts of one Group (AR-04). */
public interface ContactTagRepository
    extends Repository<ContactTag, ContactTagId>, GroupOwnedStore<ContactTag> {

  /**
   * Takes the given Tags off the Contact (R-009-01); the Tags themselves stay in the Group.
   *
   * @param groupId the Group
   * @param contactId the Contact
   * @param tagIds the Tags to take off
   */
  @Modifying(flushAutomatically = true)
  @Query(
      "DELETE FROM ContactTag ct WHERE ct.groupId = :groupId AND ct.contactId = :contactId"
          + " AND ct.tagId IN :tagIds")
  void discardOfContact(UUID groupId, UUID contactId, Collection<UUID> tagIds);
}
