package app.contacts.acceptance.support;

/**
 * A person with a Firebase account in the Auth emulator and a current ID token.
 *
 * @param email e-mail of the account
 * @param password password (null for a Google account)
 * @param uid Firebase uid
 * @param displayName display name of the account
 * @param token current ID token
 */
public record TestUser(
    String email, String password, String uid, String displayName, String token) {

  /** The same person with another token (e.g. after signing in again). */
  public TestUser withToken(String newToken) {
    return new TestUser(email, password, uid, displayName, newToken);
  }
}
