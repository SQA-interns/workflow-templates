package app.contacts.web;

import app.contacts.service.MemberView;
import java.util.UUID;

/**
 * A Member with display name, e-mail and Role (contract {@code Member}, R-005-01).
 *
 * @param userId the User id
 * @param displayName the User's display name
 * @param email the User's e-mail
 * @param role {@code OWNER}, {@code EDITOR} or {@code VIEWER}
 */
public record MemberResponse(UUID userId, String displayName, String email, String role) {
  static MemberResponse of(MemberView member) {
    return new MemberResponse(member.userId(), member.displayName(), member.email(), member.role());
  }
}
