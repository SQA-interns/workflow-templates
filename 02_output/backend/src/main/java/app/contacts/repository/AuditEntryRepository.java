package app.contacts.repository;

import app.contacts.domain.AuditEntry;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/** Audit entries of one Group (AR-04); entries are only added, never changed or deleted (SR-04). */
public interface AuditEntryRepository
    extends Repository<AuditEntry, UUID>, GroupOwnedStore<AuditEntry> {}
