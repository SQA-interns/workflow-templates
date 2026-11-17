package app.contacts.service;

import java.time.Instant;

/**
 * What the invitee sees of an Invitation before accepting it ({@code InvitationView} of the
 * contract, R-004-04).
 *
 * @param groupName the name of the Group
 * @param role {@code EDITOR} or {@code VIEWER}
 * @param expiresAt when it expires (UTC)
 */
public record InviteeView(String groupName, String role, Instant expiresAt) {}
