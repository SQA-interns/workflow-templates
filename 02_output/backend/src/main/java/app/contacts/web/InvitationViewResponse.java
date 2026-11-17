package app.contacts.web;

import app.contacts.service.InviteeView;
import java.time.Instant;

/**
 * An Invitation as its invitee sees it (contract {@code InvitationView}).
 *
 * @param groupName the name of the Group
 * @param role {@code EDITOR} or {@code VIEWER}
 * @param expiresAt when it expires
 */
public record InvitationViewResponse(String groupName, String role, Instant expiresAt) {

  static InvitationViewResponse of(InviteeView invitation) {
    return new InvitationViewResponse(
        invitation.groupName(), invitation.role(), invitation.expiresAt());
  }
}
