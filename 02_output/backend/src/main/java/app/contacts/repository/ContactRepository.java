package app.contacts.repository;

import app.contacts.domain.Contact;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/** Contacts of one Group (AR-04). */
public interface ContactRepository extends Repository<Contact, UUID>, GroupOwnedStore<Contact> {}
