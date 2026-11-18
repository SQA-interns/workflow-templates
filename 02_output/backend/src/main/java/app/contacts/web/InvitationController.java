package app.contacts.web;

import app.contacts.auth.CurrentUser;
import app.contacts.config.AppProperties;
import app.contacts.service.CreatedInvitation;
import app.contacts.service.InvitationService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Invitations of a Group (Owner) and of their invitee (US-004). */
@RestController
@RequestMapping("/api/v1")
public class InvitationController {

  private final InvitationService invitations;
  private final String linkBase;

  /**
   * The controller.
   *
   * @param invitations the Invitation service
   * @param properties gives the product URL of the links (ES-01)
   */
  public InvitationController(InvitationService invitations, AppProperties properties) {
    this.invitations = invitations;
    this.linkBase = properties.publicUrl().replaceAll("/+$", "") + "/invitations/";
  }

  /**
   * Creates an Invitation; its link is answered only here (R-004-01, SR-05).
   *
   * @param current the signed-in User
   * @param groupId the Group
   * @param body e-mail and Role
   * @return the Invitation and its link (201)
   */
  @PostMapping("/groups/{groupId}/invitations")
  @ResponseStatus(HttpStatus.CREATED)
  public InvitationCreatedResponse create(
      @AuthenticationPrincipal CurrentUser current,
      @PathVariable UUID groupId,
      @RequestBody InvitationRequest body) {
    CreatedInvitation created =
        invitations.create(groupId, current.id(), body.email(), body.role());
    return new InvitationCreatedResponse(
        InvitationResponse.of(created.invitation()), linkBase + created.token());
  }

  /**
   * The open Invitations of the Group; Owner only (R-004-08).
   *
   * @param current the signed-in User
   * @param groupId the Group
   * @return the Invitations
   */
  @GetMapping("/groups/{groupId}/invitations")
  public List<InvitationResponse> list(
      @AuthenticationPrincipal CurrentUser current, @PathVariable UUID groupId) {
    return invitations.listOpen(groupId, current.id()).stream()
        .map(InvitationResponse::of)
        .toList();
  }

  /**
   * Revokes an open Invitation; Owner only (R-004-09).
   *
   * @param current the signed-in User
   * @param groupId the Group
   * @param invitationId the Invitation
   */
  @DeleteMapping("/groups/{groupId}/invitations/{invitationId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void revoke(
      @AuthenticationPrincipal CurrentUser current,
      @PathVariable UUID groupId,
      @PathVariable UUID invitationId) {
    invitations.revoke(groupId, current.id(), invitationId);
  }

  /**
   * The Invitation of the link for its invitee (R-004-04 … R-004-06).
   *
   * @param current the signed-in User
   * @param token the token of the link
   * @return the Group name, Role and expiry
   */
  @GetMapping("/invitations/{token}")
  public InvitationViewResponse view(
      @AuthenticationPrincipal CurrentUser current, @PathVariable String token) {
    return InvitationViewResponse.of(invitations.view(token, current.user()));
  }

  /**
   * Accepts the Invitation; the caller becomes a Member with its Role (R-004-04).
   *
   * @param current the signed-in User
   * @param token the token of the link
   * @return the Group with the caller's Role
   */
  @PostMapping("/invitations/{token}/accept")
  public GroupResponse accept(
      @AuthenticationPrincipal CurrentUser current, @PathVariable String token) {
    return GroupResponse.of(invitations.accept(token, current.user()));
  }
}
