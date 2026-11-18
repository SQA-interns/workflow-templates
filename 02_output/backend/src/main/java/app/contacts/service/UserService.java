package app.contacts.service;

import app.contacts.domain.User;
import app.contacts.repository.GroupRepository;
import app.contacts.repository.MembershipRepository;
import app.contacts.repository.UserRepository;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Users: provisioning on the first call (R-001-04), language (R-020-02), deleting the account
 * (R-021-01, R-021-02); deleted Users are refused (D-20).
 */
@Service
public class UserService {

  private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

  /** Longest stored display name (app_user.display_name, contract {@code Me}). */
  static final int DISPLAY_NAME_MAX = 100;

  private final UserRepository users;
  private final GroupRepository groups;
  private final MembershipRepository memberships;
  private final AccountDeleter accounts;
  private final TransactionOperations transactions;
  private final Clock clock;

  /**
   * The service.
   *
   * @param users the User repository
   * @param groups the Group repository
   * @param memberships the Membership repository
   * @param accounts deletes the Firebase account
   * @param transactions runs the database part of a deletion in its own transaction
   * @param clock the clock
   */
  public UserService(
      UserRepository users,
      GroupRepository groups,
      MembershipRepository memberships,
      AccountDeleter accounts,
      TransactionOperations transactions,
      Clock clock) {
    this.users = users;
    this.groups = groups;
    this.memberships = memberships;
    this.accounts = accounts;
    this.transactions = transactions;
    this.clock = clock;
  }

  /**
   * The User of a verified Firebase account; created on the first call (R-001-04, EX-001-03).
   *
   * @param firebaseUid the uid of the token
   * @param email the e-mail of the token
   * @param name the display name of the token, may be null
   * @return the User
   * @throws UserDeletedException when the User deleted the account (D-20); its Firebase account is
   *     deleted again first, in case that failed at the deletion (D-80)
   */
  public UserView signIn(String firebaseUid, String email, String name) {
    User user =
        users.findByFirebaseUid(firebaseUid).orElseGet(() -> create(firebaseUid, email, name));
    if (user.getDeletedAt() != null) {
      purgeFirebaseAccount(user);
      throw new UserDeletedException();
    }
    return UserView.of(user);
  }

  /**
   * Chooses the language of the product; kept for later sign-ins (R-020-02).
   *
   * @param userId the signed-in User
   * @param language {@code sl} or {@code en}
   * @return the changed User
   * @throws ValidationException {@code me.languageInvalid} for any other value (EX-020-01)
   */
  @Transactional
  public UserView changeLanguage(UUID userId, String language) {
    if (language == null || !User.LANGUAGES.contains(language)) {
      throw new ValidationException(
          List.of(new FieldProblem("language", "me.languageInvalid", Map.of())));
    }
    User user = users.findById(userId).orElseThrow(NotFoundException::new);
    user.changeLanguage(language);
    users.save(user);
    return UserView.of(user);
  }

  /**
   * Deletes the User's account: the Memberships and the User's personal data (anonymised, D-20) in
   * one transaction, then, after the commit, the Firebase account (R-021-01). A failed Firebase
   * deletion keeps the rest; it is repeated at the next request with that account (D-80).
   *
   * @param userId the signed-in User
   * @param confirmEmail the e-mail the User typed to confirm
   * @throws InvalidRequestException {@code validation} when the typed e-mail does not match
   *     (EX-021-01)
   * @throws ConflictException {@code account.ownsGroups} with the {@code names} of the Groups the
   *     User owns, active or in the recycle bin (R-021-02, EX-021-02)
   */
  public void closeAccount(UUID userId, String confirmEmail) {
    User user = transactions.execute(status -> anonymise(userId, confirmEmail));
    purgeFirebaseAccount(user);
  }

  private User anonymise(UUID userId, String confirmEmail) {
    User user = users.findById(userId).orElseThrow(NotFoundException::new);
    if (!sameEmail(confirmEmail, user.getEmail())) {
      throw new InvalidRequestException("validation", Map.of());
    }
    List<String> owned = groups.findNamesOwnedBy(userId);
    if (!owned.isEmpty()) {
      throw new ConflictException("account.ownsGroups", Map.of("names", String.join(", ", owned)));
    }
    memberships.dropAllOfUser(userId);
    user.anonymise(clock.instant());
    users.saveAndFlush(user);
    return user;
  }

  /** Deletes the Firebase account of a deleted User; a failure is logged without data (SR-08). */
  private void purgeFirebaseAccount(User user) {
    try {
      accounts.purge(user.getFirebaseUid());
    } catch (RuntimeException failed) {
      LOG.warn("Firebase account of a deleted user not deleted; repeated at its next request");
    }
  }

  /** E-mails are compared ignoring case (BR-11); outer spaces of the typed one are ignored. */
  private static boolean sameEmail(String typed, String email) {
    return typed != null && lower(typed.strip()).equals(lower(email));
  }

  private static String lower(String text) {
    return text.toLowerCase(Locale.ROOT);
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
