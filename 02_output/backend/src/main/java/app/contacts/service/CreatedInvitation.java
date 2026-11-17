package app.contacts.service;

/**
 * A new Invitation with its token, which is shown only this once (R-004-01, SR-05).
 *
 * @param invitation the Invitation
 * @param token the token of its link
 */
public record CreatedInvitation(InvitationView invitation, String token) {}
