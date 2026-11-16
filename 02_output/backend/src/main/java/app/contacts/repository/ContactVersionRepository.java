package app.contacts.repository;

import app.contacts.domain.ContactVersion;
import app.contacts.domain.ContactVersionId;
import org.springframework.data.repository.Repository;

/** Versions of the Contacts of one Group; never updated or deleted on their own (ADR-07). */
public interface ContactVersionRepository
    extends Repository<ContactVersion, ContactVersionId>, GroupOwnedStore<ContactVersion> {}
