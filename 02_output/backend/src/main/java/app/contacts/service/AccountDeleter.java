package app.contacts.service;

/** Deletes the sign-in account of a User at the identity provider (R-021-01, ADR-03). */
public interface AccountDeleter {

  /**
   * Deletes the account; an account that no longer exists counts as deleted.
   *
   * @param firebaseUid the Firebase uid of the account
   */
  void delete(String firebaseUid);
}
