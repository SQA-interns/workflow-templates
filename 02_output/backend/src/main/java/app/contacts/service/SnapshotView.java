package app.contacts.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

/**
 * The full content of a Contact after a Version (contract {@code Version.snapshot}, R-010-02).
 *
 * @param fields Field name to value in form order; {@code null} for an empty Field
 * @param attributes the Attributes by key A–Z
 * @param tags the Tag names A–Z
 */
public record SnapshotView(
    SequencedMap<String, String> fields, List<AttributeView> attributes, List<String> tags) {

  /**
   * A snapshot with its own copies of the Fields, Attributes and Tags.
   *
   * @param fields the Fields
   * @param attributes the Attributes
   * @param tags the Tag names
   */
  public SnapshotView {
    fields = new LinkedHashMap<>(fields);
    attributes = List.copyOf(attributes);
    tags = List.copyOf(tags);
  }

  /**
   * The Fields in form order.
   *
   * @return a copy of the Fields
   */
  @Override
  public SequencedMap<String, String> fields() {
    return new LinkedHashMap<>(fields);
  }
}
