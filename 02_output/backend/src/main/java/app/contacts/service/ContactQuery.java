package app.contacts.service;

import java.util.List;
import java.util.UUID;

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
public record ContactQuery(String search, String sort, String dir, Integer page, Filters filters) {

  /**
   * The filters as sent (contract parameters, R-015-01 … R-015-04); {@code null} is not set.
   *
   * @param hasTagIds Tags the Contact has every one of
   * @param withoutTagIds Tags the Contact has none of
   * @param attributeKey an Attribute key the Contact has
   * @param attributeValue that Attribute's value
   * @param company the company
   * @param country the country code
   */
  public record Filters(
      List<UUID> hasTagIds,
      List<UUID> withoutTagIds,
      String attributeKey,
      String attributeValue,
      String company,
      String country) {

    /** No filter sent. */
    public static final Filters NONE = new Filters(null, null, null, null, null, null);

    /** Keeps the lists as sent; {@code null} for none. */
    public Filters {
      hasTagIds = hasTagIds == null ? null : List.copyOf(hasTagIds);
      withoutTagIds = withoutTagIds == null ? null : List.copyOf(withoutTagIds);
    }
  }
}
