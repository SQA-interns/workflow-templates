package app.contacts.service;

/** The User of the token deleted the account; the token is refused for good (D-20). */
public class UserDeletedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /** The exception. */
  public UserDeletedException() {
    super("User deleted");
  }
}
