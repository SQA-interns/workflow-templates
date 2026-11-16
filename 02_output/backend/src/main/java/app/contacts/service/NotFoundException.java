package app.contacts.service;

/** The item does not exist or belongs to another Group: 404 {@code notFound} (BR-02). */
public class NotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /** The exception. */
  public NotFoundException() {
    super("not found");
  }
}
