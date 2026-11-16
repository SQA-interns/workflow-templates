package app.contacts.service;

/** The Role of the User does not allow the action: 403 {@code auth.forbidden} (BR-03, BR-04). */
public class ForbiddenException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /** The exception. */
  public ForbiddenException() {
    super("forbidden");
  }
}
