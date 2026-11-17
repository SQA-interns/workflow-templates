package app.contacts.domain;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The Active filters of the Contact list, checked and trimmed (R-015-01 … R-015-04); {@code null}
 * or an empty list means the filter is not set. All filters combine with AND (R-015-05).
 *
 * @param hasTagIds the Contact has every one of these Tags
 * @param withoutTagIds the Contact has none of these Tags
 * @param attributeKey the Contact has an Attribute with this key, case-insensitively
 * @param attributeValue with the key: that Attribute's value equals this, case-insensitively
 * @param company the company equals this, case-insensitively
 * @param country the ISO 3166-1 alpha-2 country
 */
public record ContactFilters(
    List<UUID> hasTagIds,
    List<UUID> withoutTagIds,
    String attributeKey,
    String attributeValue,
    String company,
    String country) {

  /** No filter: the whole list. */
  public static final ContactFilters NONE =
      new ContactFilters(List.of(), List.of(), null, null, null, null);

  /** Keeps each Tag id once, so "has every Tag" can count them. */
  public ContactFilters {
    hasTagIds = List.copyOf(distinct(hasTagIds));
    withoutTagIds = List.copyOf(distinct(withoutTagIds));
  }

  private static Set<UUID> distinct(List<UUID> ids) {
    return ids == null ? Set.of() : new LinkedHashSet<>(ids);
  }
}
