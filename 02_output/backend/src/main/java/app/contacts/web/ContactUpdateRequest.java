package app.contacts.web;

import app.contacts.service.AttributeView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Edited Fields and Attributes of a Contact (contract {@code ContactUpdate}); Tags follow with
 * US-009.
 *
 * @param fields Field name to value; a value may be {@code null}
 * @param attributes the Attributes (R-008-01), {@code null} when the body has none
 * @param versionNumber the Version number the client read (BR-06), {@code null} when missing
 */
public record ContactUpdateRequest(
    Map<String, String> fields, List<AttributeView> attributes, Integer versionNumber) {

  /**
   * A request with unmodifiable copies of the Fields and Attributes.
   *
   * @param fields Field name to value, or {@code null} when the body has none
   * @param attributes the Attributes, or {@code null} when the body has none
   * @param versionNumber the Version number read
   */
  public ContactUpdateRequest {
    fields = fields == null ? null : Collections.unmodifiableMap(new HashMap<>(fields));
    attributes =
        attributes == null ? null : Collections.unmodifiableList(new ArrayList<>(attributes));
  }
}
