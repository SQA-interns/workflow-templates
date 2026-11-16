package app.contacts.web;

import app.contacts.auth.CurrentUser;
import app.contacts.service.UserView;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in User ({@code /me}, US-001). */
@RestController
@RequestMapping("/api/v1/me")
public class MeController {

  /**
   * The signed-in User; created by the first call (R-001-04).
   *
   * @param current the signed-in User
   * @return the User
   */
  @GetMapping
  public MeResponse me(@AuthenticationPrincipal CurrentUser current) {
    UserView user = current.user();
    return new MeResponse(
        user.id(), user.email(), user.displayName(), user.language(), current.emailVerified());
  }
}
