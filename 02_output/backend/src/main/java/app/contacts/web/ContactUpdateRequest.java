package app.contacts.web;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Edited Fields of a Contact (contract {@code ContactUpdate}); Attributes and Tags follow with
 * US-008 and US-009.
 *
 * @param fields Field name to value; a value may be {@code null}
 * @param versionNumber the Version number the client read (BR-06), {@code null} when missing
 */
public record ContactUpdateRequest(Map<String, String> fields, Integer versionNumber) {

  /**
   * A request with an unmodifiable copy of the Fields.
   *
   * @param fields Field name to value, or {@code null} when the body has none
   * @param versionNumber the Version number read
   */
  public ContactUpdateRequest {
    fields = fields == null ? null : Collections.unmodifiableMap(new HashMap<>(fields));
  }
}
