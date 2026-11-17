package app.contacts.repository;

import app.contacts.domain.Merge;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/** Merges of one Group (AR-04). */
public interface MergeRepository extends Repository<Merge, UUID>, GroupOwnedStore<Merge> {}
