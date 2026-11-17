package app.contacts.web;

/**
 * A new Invitation with its link, returned only here (contract {@code InvitationCreated}, SR-05).
 *
 * @param invitation the Invitation
 * @param link {@code <APP_PUBLIC_URL>/invitations/<token>}
 */
public record InvitationCreatedResponse(InvitationResponse invitation, String link) {}
