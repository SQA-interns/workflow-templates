package app.contacts.auth;

import app.contacts.service.AccountDeleter;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuthException;

/** Deletes Firebase accounts with the Admin SDK (R-021-01, ADR-03); needs a service account. */
public class FirebaseAccountDeleter implements AccountDeleter {

  /** The deletion of the Admin SDK, {@code FirebaseAuth::deleteUser}. */
  @FunctionalInterface
  public interface UserDeletion {
    /**
     * Deletes the account.
     *
     * @param uid the Firebase uid
     * @throws FirebaseAuthException when the account cannot be deleted
     */
    void perform(String uid) throws FirebaseAuthException;
  }

  private final UserDeletion firebase;

  /**
   * The deleter.
   *
   * @param firebase the deletion of the configured Firebase project
   */
  public FirebaseAccountDeleter(UserDeletion firebase) {
    this.firebase = firebase;
  }

  /**
   * {@inheritDoc}
   *
   * @throws IllegalStateException when Firebase refuses or cannot be reached (500, rolls back)
   */
  @Override
  public void purge(String firebaseUid) {
    try {
      firebase.perform(firebaseUid);
    } catch (FirebaseAuthException failed) {
      if (failed.getAuthErrorCode() != AuthErrorCode.USER_NOT_FOUND) {
        throw new IllegalStateException("Firebase account not deleted", failed);
      }
    }
  }
}
