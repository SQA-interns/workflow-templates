package app.contacts.service;

/**
 * What the User asked of the Contact list, as sent (contract parameters {@code q}, {@code sort},
 * {@code dir}, {@code page}); {@code null} means the default.
 *
 * @param search the search text
 * @param sort {@code name}, {@code company}, {@code email} or {@code updatedAt}
 * @param dir {@code asc} or {@code desc}
 * @param page the Page number, from 1
 */
public record ContactQuery(String search, String sort, String dir, Integer page) {}
