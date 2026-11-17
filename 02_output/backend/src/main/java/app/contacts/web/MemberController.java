package app.contacts.web;

import app.contacts.auth.CurrentUser;
import app.contacts.service.MembershipService;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The Members of a Group; Owner only (US-005). */
@RestController
@RequestMapping("/api/v1/groups/{groupId}/members")
public class MemberController {

  private final MembershipService memberships;

  /**
   * The controller.
   *
   * @param memberships the Membership service
   */
  public MemberController(MembershipService memberships) {
    this.memberships = memberships;
  }

  /**
   * The Members with display name, e-mail and Role (R-005-01).
   *
   * @param current the signed-in User
   * @param groupId the Group
   * @return the Members
   */
  @GetMapping
  public List<MemberResponse> list(
      @AuthenticationPrincipal CurrentUser current, @PathVariable UUID groupId) {
    return memberships.list(groupId, current.id()).stream().map(MemberResponse::of).toList();
  }
}
