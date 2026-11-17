package app.contacts.service;

import java.time.Instant;
import java.util.UUID;

/**
 * What the Owner sees of an open Invitation: never its token ({@code Invitation} of the contract,
 * R-004-08, SR-05).
 *
 * @param id the Invitation id
 * @param email the invitee's e-mail
 * @param role {@code EDITOR} or {@code VIEWER}
 * @param expiresAt when it expires (UTC)
 */
public record InvitationView(UUID id, String email, String role, Instant expiresAt) {}
