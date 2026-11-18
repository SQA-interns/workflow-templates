package app.contacts.web;

import app.contacts.auth.CurrentUser;
import app.contacts.service.UserService;
import app.contacts.service.UserView;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in User ({@code /me}, US-001, US-020, US-021). */
@RestController
@RequestMapping("/api/v1/me")
public class MeController {

  private final UserService users;

  /**
   * The controller.
   *
   * @param users the User service
   */
  public MeController(UserService users) {
    this.users = users;
  }

  /**
   * The signed-in User; created by the first call (R-001-04).
   *
   * @param current the signed-in User
   * @return the User
   */
  @GetMapping
  public MeResponse me(@AuthenticationPrincipal CurrentUser current) {
    return response(current.user(), current);
  }

  /**
   * Chooses the language; kept for later sign-ins (R-020-02); 400 for any other (EX-020-01).
   *
   * @param current the signed-in User
   * @param update the chosen language
   * @return the changed User
   */
  @PatchMapping
  public MeResponse update(
      @AuthenticationPrincipal CurrentUser current, @RequestBody MeUpdate update) {
    return response(users.changeLanguage(current.id(), update.language()), current);
  }

  /**
   * Deletes the account of the signed-in User (R-021-01); 400 when the typed e-mail does not match
   * (EX-021-01), 409 {@code account.ownsGroups} for an Owner (R-021-02).
   *
   * @param current the signed-in User
   * @param deletion the typed e-mail
   */
  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
      @AuthenticationPrincipal CurrentUser current, @RequestBody AccountDeletion deletion) {
    users.deleteAccount(current.id(), deletion.confirmEmail());
  }

  private static MeResponse response(UserView user, CurrentUser current) {
    return new MeResponse(
        user.id(), user.email(), user.displayName(), user.language(), current.emailVerified());
  }
}
