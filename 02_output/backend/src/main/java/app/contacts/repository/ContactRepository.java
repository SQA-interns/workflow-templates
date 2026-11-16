package app.contacts.repository;

import app.contacts.domain.Contact;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** Contacts of one Group (AR-04). */
public interface ContactRepository extends Repository<Contact, UUID>, GroupOwnedStore<Contact> {

  /**
   * The active Contacts of the Group with this e-mail, compared case-insensitively (R-006-06,
   * BR-11), oldest first.
   *
   * @param groupId the Group
   * @param email the e-mail
   * @return the Contacts
   */
  @Query(
      """
      SELECT c FROM Contact c
      WHERE c.groupId = :groupId AND c.state = 'active' AND lower(c.fields.email) = lower(:email)
      ORDER BY c.createdAt, c.id
      """)
  List<Contact> findActiveByEmail(UUID groupId, String email);
}
