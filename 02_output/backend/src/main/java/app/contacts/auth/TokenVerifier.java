package app.contacts.auth;

import java.util.Optional;

/** Checks a Firebase ID token (R-001-02, SR-01). */
public interface TokenVerifier {

  /**
   * Verifies signature, Firebase project and expiry.
   *
   * @param idToken the token of the Authorization header
   * @return the claims, or empty if the token is invalid, expired or for another project
   */
  Optional<VerifiedToken> verify(String idToken);
}
