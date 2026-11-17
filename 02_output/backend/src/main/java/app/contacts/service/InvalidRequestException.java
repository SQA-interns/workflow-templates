package app.contacts.service;

import java.util.Map;

/**
 * The request cannot be carried out as sent: 400 with a catalog code of specification §5 and its
 * parameters (e.g. {@code merge.choiceRequired} with {@code key}, R-018-03).
 */
public class InvalidRequestException extends RuntimeException {

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
  public InvalidRequestException(String code, Map<String, Object> params) {
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
