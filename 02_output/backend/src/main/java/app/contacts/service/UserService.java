package app.contacts.service;

import app.contacts.domain.User;
import app.contacts.repository.UserRepository;
import java.time.Clock;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/** Users: provisioning on the first call (R-001-04); deleted Users are refused (D-20). */
@Service
public class UserService {

  /** Longest stored display name (app_user.display_name, contract {@code Me}). */
  static final int DISPLAY_NAME_MAX = 100;

  private final UserRepository users;
  private final Clock clock;

  /**
   * The service.
   *
   * @param users the User repository
   * @param clock the clock
   */
  public UserService(UserRepository users, Clock clock) {
    this.users = users;
    this.clock = clock;
  }

  /**
   * The User of a verified Firebase account; created on the first call (R-001-04, EX-001-03).
   *
   * @param firebaseUid the uid of the token
   * @param email the e-mail of the token
   * @param name the display name of the token, may be null
   * @return the User
   * @throws UserDeletedException when the User deleted the account (D-20)
   */
  public UserView signIn(String firebaseUid, String email, String name) {
    User user =
        users.findByFirebaseUid(firebaseUid).orElseGet(() -> create(firebaseUid, email, name));
    if (user.getDeletedAt() != null) {
      throw new UserDeletedException();
    }
    return UserView.of(user);
  }

  /** Creates the User; if a parallel first call created it meanwhile, returns that one. */
  private User create(String firebaseUid, String email, String name) {
    try {
      return users.saveAndFlush(
          User.create(firebaseUid, email, displayName(email, name), clock.instant()));
    } catch (DataIntegrityViolationException createdMeanwhile) {
      return users.findByFirebaseUid(firebaseUid).orElseThrow(() -> createdMeanwhile);
    }
  }

  private static String displayName(String email, String name) {
    String chosen =
        name == null || name.isBlank() ? email.substring(0, email.indexOf('@')) : name.strip();
    return chosen.length() > DISPLAY_NAME_MAX ? chosen.substring(0, DISPLAY_NAME_MAX) : chosen;
  }
}
