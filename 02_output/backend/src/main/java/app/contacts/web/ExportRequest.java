package app.contacts.web;

import java.util.List;
import java.util.UUID;

/**
 * Body of {@code POST /groups/{groupId}/exports} (contract {@code ExportRequest}): the list's
 * search, sort and filters, and the Checked contacts for "Export checked"; {@code null} is not set.
 *
 * @param q the search text
 * @param sort the sort key
 * @param dir the order
 * @param hasTagIds Tags the Contact has every one of
 * @param withoutTagIds Tags the Contact has none of
 * @param attributeKey an Attribute key the Contact has
 * @param attributeValue that Attribute's value
 * @param company the company
 * @param country the country code
 * @param contactIds the Checked contacts (R-016-02); {@code null} for the whole list
 */
public record ExportRequest(
    String q,
    String sort,
    String dir,
    List<UUID> hasTagIds,
    List<UUID> withoutTagIds,
    String attributeKey,
    String attributeValue,
    String company,
    String country,
    List<UUID> contactIds) {

  /** Keeps unmodifiable copies of the lists. */
  public ExportRequest {
    hasTagIds = hasTagIds == null ? null : List.copyOf(hasTagIds);
    withoutTagIds = withoutTagIds == null ? null : List.copyOf(withoutTagIds);
    contactIds = contactIds == null ? null : List.copyOf(contactIds);
  }
}
