package app.contacts.service;

import java.util.UUID;

/**
 * A Member as the Owner sees it on the Members screen (contract {@code Member}, R-005-01).
 *
 * @param userId the User id
 * @param displayName the User's display name
 * @param email the User's e-mail
 * @param role {@code OWNER}, {@code EDITOR} or {@code VIEWER}
 */
public record MemberView(UUID userId, String displayName, String email, String role) {}
