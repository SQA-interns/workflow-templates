package app.contacts.web;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A new Contact (contract {@code ContactWrite}); Attributes and Tags follow with US-008 and US-009.
 *
 * @param fields Field name to value; a value may be {@code null}
 */
public record ContactWriteRequest(Map<String, String> fields) {

  /**
   * A request with an unmodifiable copy of the Fields.
   *
   * @param fields Field name to value, or {@code null} when the body has none
   */
  public ContactWriteRequest {
    fields = fields == null ? null : Collections.unmodifiableMap(new HashMap<>(fields));
  }
}
