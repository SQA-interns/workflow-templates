package app.contacts.auth;

import java.util.Map;

/**
 * The claims of an ID token whose signature, Firebase project and expiry were checked (R-001-02).
 *
 * @param uid the Firebase uid ({@code sub})
 * @param email the e-mail of the account
 * @param name the display name, may be null
 * @param emailVerified whether the e-mail is verified
 * @param signInProvider the sign-in method, e.g. {@code password} or {@code google.com}
 */
public record VerifiedToken(
    String uid, String email, String name, boolean emailVerified, String signInProvider) {

  /**
   * Reads the claims of a verified ID token.
   *
   * @param claims the claims
   * @return the token
   */
  public static VerifiedToken of(Map<String, Object> claims) {
    Object firebase = claims.get("firebase");
    Object provider = firebase instanceof Map<?, ?> map ? map.get("sign_in_provider") : null;
    return new VerifiedToken(
        (String) claims.get("sub"),
        (String) claims.get("email"),
        (String) claims.get("name"),
        Boolean.TRUE.equals(claims.get("email_verified")),
        (String) provider);
  }
}
