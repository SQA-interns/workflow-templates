package app.contacts.service;

import app.contacts.domain.ContactFilters;
import app.contacts.domain.ContactSort;

/**
 * The checked search, filters and sort of the Contact list, without the Page (R-014-02 … R-014-04,
 * R-015-01 … R-015-05): the same list for the Page, its count and the Export scope (R-016-01).
 *
 * @param pattern the LIKE pattern of the Normalised search text, {@code \} escaping
 * @param filters the Active filters, with Tag ids of the Group only
 * @param sort the sort key
 * @param descending whether the sort key runs Z–A
 */
record ContactListScope(
    String pattern, ContactFilters filters, ContactSort sort, boolean descending) {}
