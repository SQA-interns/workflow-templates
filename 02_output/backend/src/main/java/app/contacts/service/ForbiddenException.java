package app.contacts.service;

/**
 * The User may not do this: 403 with a catalog code, {@code auth.forbidden} when the Role does not
 * allow the action (BR-03, BR-04).
 */
public class ForbiddenException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /** The catalog code of a Role without the permission. */
  public static final String ROLE = "auth.forbidden";

  /** The exception for a Role without the permission. */
  public ForbiddenException() {
    this(ROLE);
  }

  /**
   * The exception with another catalog code, e.g. {@code invitation.otherEmail} (R-004-05).
   *
   * @param code the catalog code
   */
  public ForbiddenException(String code) {
    super(code);
  }
}
