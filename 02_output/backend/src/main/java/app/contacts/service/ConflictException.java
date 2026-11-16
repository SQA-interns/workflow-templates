package app.contacts.service;

import java.util.Map;

/**
 * The change does not fit the current state of the item: 409 with a catalog code of specification
 * §5 and its parameters (e.g. {@code version.conflict} with {@code changedBy}, BR-06).
 */
public class ConflictException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /** The catalog code. */
  private final String code;

  /** The values the message names; immutable. */
  private final transient Map<String, Object> params;

  /**
   * The exception.
   *
   * @param code the catalog code
   * @param params the values the message names, empty for none
   */
  public ConflictException(String code, Map<String, Object> params) {
    super(code);
    this.code = code;
    this.params = Map.copyOf(params);
  }

  public String getCode() {
    return code;
  }

  public Map<String, Object> getParams() {
    return params;
  }
}
