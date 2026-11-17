package app.contacts.service;

import app.contacts.domain.ContactFilters;

/**
 * What the User asked of the Contact list, as sent (contract parameters {@code q}, {@code sort},
 * {@code dir}, {@code page} and the filters); {@code null} means the default.
 *
 * @param search the search text
 * @param sort {@code name}, {@code company}, {@code email} or {@code updatedAt}
 * @param dir {@code asc} or {@code desc}
 * @param page the Page number, from 1
 * @param filters the Active filters as sent, not yet trimmed or checked
 */
public record ContactQuery(
    String search, String sort, String dir, Integer page, ContactFilters filters) {}
