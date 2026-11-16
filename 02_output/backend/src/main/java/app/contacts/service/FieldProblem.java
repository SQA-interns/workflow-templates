package app.contacts.service;

import java.util.Map;

/**
 * One invalid field of a request (specification §3 and §5).
 *
 * @param field the field name of the request
 * @param code the catalog code, e.g. {@code field.tooLong}
 * @param params the values of the message, e.g. {@code max}
 */
public record FieldProblem(String field, String code, Map<String, Object> params) {

  /**
   * An immutable copy.
   *
   * @param field the field name
   * @param code the code
   * @param params the message values
   */
  public FieldProblem {
    params = Map.copyOf(params);
  }
}
