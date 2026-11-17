package app.contacts.web;

/**
 * The body of creating an Invitation (contract {@code InvitationRequest}).
 *
 * @param email the invitee's e-mail; checked by the service (EX-004-06)
 * @param role {@code EDITOR} or {@code VIEWER}; checked by the service (EX-004-07)
 */
public record InvitationRequest(String email, String role) {}
