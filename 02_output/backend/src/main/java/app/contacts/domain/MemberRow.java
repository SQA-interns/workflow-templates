package app.contacts.domain;

import java.util.UUID;

/**
 * A Member of a Group as the Owner sees it on the Members screen (R-005-01).
 *
 * @param userId the User id
 * @param displayName the User's display name
 * @param email the User's e-mail
 * @param role the Role name, e.g. {@code OWNER}
 */
public record MemberRow(UUID userId, String displayName, String email, String role) {}
