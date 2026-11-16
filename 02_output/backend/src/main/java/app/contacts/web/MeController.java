package app.contacts.web;

import app.contacts.auth.CurrentUser;
import app.contacts.service.UserService;
import app.contacts.service.UserView;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in User ({@code /me}, US-001, US-020). */
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

  private static MeResponse response(UserView user, CurrentUser current) {
    return new MeResponse(
        user.id(), user.email(), user.displayName(), user.language(), current.emailVerified());
  }
}
