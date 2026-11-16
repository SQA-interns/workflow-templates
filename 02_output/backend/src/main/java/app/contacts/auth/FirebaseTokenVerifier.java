package app.contacts.auth;

import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import java.util.Optional;

/** Verifies ID tokens with the Firebase Admin SDK, offline after the keys are fetched (ADR-03). */
public class FirebaseTokenVerifier implements TokenVerifier {

  /** The check of the Admin SDK, {@code FirebaseAuth::verifyIdToken}. */
  @FunctionalInterface
  public interface IdTokenCheck {
    /**
     * Verifies the token.
     *
     * @param idToken the token
     * @return its claims
     * @throws FirebaseAuthException when the token is invalid
     */
    FirebaseToken verifyIdToken(String idToken) throws FirebaseAuthException;
  }

  private final IdTokenCheck firebase;

  /**
   * The verifier.
   *
   * @param firebase the token check of the configured Firebase project
   */
  public FirebaseTokenVerifier(IdTokenCheck firebase) {
    this.firebase = firebase;
  }

  @Override
  public Optional<VerifiedToken> verify(String idToken) {
    try {
      return Optional.of(VerifiedToken.of(firebase.verifyIdToken(idToken).getClaims()));
    } catch (FirebaseAuthException | IllegalArgumentException invalid) {
      return Optional.empty();
    }
  }
}
