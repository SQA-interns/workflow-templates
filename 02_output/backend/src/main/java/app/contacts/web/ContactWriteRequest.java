package app.contacts.web;

import app.contacts.service.AttributeView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A new Contact (contract {@code ContactWrite}); Tags follow with US-009.
 *
 * @param fields Field name to value; a value may be {@code null}
 * @param attributes the Attributes (R-008-01), {@code null} when the body has none
 */
public record ContactWriteRequest(Map<String, String> fields, List<AttributeView> attributes) {

  /**
   * A request with unmodifiable copies of the Fields and Attributes.
   *
   * @param fields Field name to value, or {@code null} when the body has none
   * @param attributes the Attributes, or {@code null} when the body has none
   */
  public ContactWriteRequest {
    fields = fields == null ? null : Collections.unmodifiableMap(new HashMap<>(fields));
    attributes =
        attributes == null ? null : Collections.unmodifiableList(new ArrayList<>(attributes));
  }
}
