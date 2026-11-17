package app.contacts.web;

import app.contacts.service.InvitationView;
import java.time.Instant;
import java.util.UUID;

/**
 * An open Invitation as the Owner sees it, without its link (contract {@code Invitation}).
 *
 * @param id the Invitation id
 * @param email the invitee's e-mail
 * @param role {@code EDITOR} or {@code VIEWER}
 * @param expiresAt when it expires
 */
public record InvitationResponse(UUID id, String email, String role, Instant expiresAt) {

  static InvitationResponse of(InvitationView invitation) {
    return new InvitationResponse(
        invitation.id(), invitation.email(), invitation.role(), invitation.expiresAt());
  }
}
