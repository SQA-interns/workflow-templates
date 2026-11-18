package app.contacts.web;

import app.contacts.service.AttributeView;
import app.contacts.service.ContactFieldsSchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A new Contact (contract {@code ContactWrite}).
 *
 * @param fields Field name to value; a value may be {@code null}
 * @param attributes the Attributes (R-008-01), {@code null} when the body has none
 * @param tags the Tag names (R-009-01), {@code null} when the body has none
 */
public record ContactWriteRequest(
    @Schema(implementation = ContactFieldsSchema.class) Map<String, String> fields,
    List<AttributeView> attributes,
    List<String> tags) {

  /**
   * A request with unmodifiable copies of the Fields, Attributes and Tag names.
   *
   * @param fields Field name to value, or {@code null} when the body has none
   * @param attributes the Attributes, or {@code null} when the body has none
   * @param tags the Tag names, or {@code null} when the body has none
   */
  public ContactWriteRequest {
    fields = fields == null ? null : Collections.unmodifiableMap(new HashMap<>(fields));
    attributes =
        attributes == null ? null : Collections.unmodifiableList(new ArrayList<>(attributes));
    tags = tags == null ? null : Collections.unmodifiableList(new ArrayList<>(tags));
  }
}
