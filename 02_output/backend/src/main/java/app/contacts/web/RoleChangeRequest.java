package app.contacts.web;

/**
 * The body of a Role change (contract {@code RoleChange}).
 *
 * @param role {@code EDITOR} or {@code VIEWER}; checked by the service (EX-005-03)
 */
public record RoleChangeRequest(String role) {}
