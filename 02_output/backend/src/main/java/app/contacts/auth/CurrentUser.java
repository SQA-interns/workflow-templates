package app.contacts.auth;

import app.contacts.service.UserView;
import java.util.UUID;

/**
 * The signed-in User of the current request.
 *
 * @param user the User
 * @param emailVerified whether the e-mail of the token is verified
 */
public record CurrentUser(UserView user, boolean emailVerified) {

  /**
   * The User id.
   *
   * @return the id
   */
  public UUID id() {
    return user.id();
  }
}
