package app.contacts.repository;

import app.contacts.domain.Attribute;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** Attributes of the Contacts of one Group (AR-04); read through {@link ContactQueryRepository}. */
public interface AttributeRepository
    extends Repository<Attribute, UUID>, GroupOwnedStore<Attribute> {

  /**
   * Removes every Attribute of the Contact at once, so that a save can store its Attributes anew
   * (R-008-01).
   *
   * @param groupId the Group
   * @param contactId the Contact
   */
  @Modifying(flushAutomatically = true)
  @Query("DELETE FROM Attribute a WHERE a.groupId = :groupId AND a.contactId = :contactId")
  void discardOfContact(UUID groupId, UUID contactId);
}
