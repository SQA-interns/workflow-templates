package app.contacts.auth;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import java.util.Optional;

/** Verifies ID tokens with the Firebase Admin SDK, offline after the keys are fetched (ADR-03). */
public class FirebaseTokenVerifier implements TokenVerifier {

  private final FirebaseAuth firebase;

  /**
   * The verifier.
   *
   * @param firebase the Firebase Auth client of the configured project
   */
  public FirebaseTokenVerifier(FirebaseAuth firebase) {
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
