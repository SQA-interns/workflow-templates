package app.contacts.service;

import app.contacts.domain.ContactFields;
import app.contacts.domain.ContactVersion;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;
import tools.jackson.databind.json.JsonMapper;

/**
 * The full content of a Contact after a Version (contract {@code Version.snapshot}, R-010-02).
 *
 * @param fields Field name to value (in the order PostgreSQL keeps jsonb keys, not form order);
 *     {@code null} for an empty Field
 * @param attributes the Attributes by key A–Z
 * @param tags the Tag names A–Z
 */
public record SnapshotView(
    @Schema(implementation = ContactFieldsSchema.class) SequencedMap<String, String> fields,
    List<AttributeView> attributes,
    List<String> tags) {

  /** Reads the plain records of a Version; immutable and thread-safe. */
  private static final JsonMapper JSON = JsonMapper.builder().build();

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
   * The Fields.
   *
   * @return a copy of the Fields
   */
  @Override
  public SequencedMap<String, String> fields() {
    return new LinkedHashMap<>(fields);
  }

  /**
   * The snapshot a Version recorded (ADR-07).
   *
   * @param version the Version
   * @return its snapshot
   */
  static SnapshotView of(ContactVersion version) {
    return JSON.readValue(version.getSnapshot(), SnapshotView.class);
  }

  /**
   * The snapshot as the content of a Contact, to make it current again (R-011-01, R-019-01).
   *
   * @return the content
   */
  ContactContent content() {
    return new ContactContent(ContactFields.ofNames(fields), attributes, tags);
  }
}
