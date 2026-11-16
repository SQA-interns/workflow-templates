package app.contacts.service;

import app.contacts.domain.User;
import java.util.UUID;

/**
 * What the API shows of a User ({@code Me} of the contract).
 *
 * @param id the User id
 * @param email the e-mail
 * @param displayName the display name
 * @param language the language, {@code sl} or {@code en}
 */
public record UserView(UUID id, String email, String displayName, String language) {

  static UserView of(User user) {
    return new UserView(user.getId(), user.getEmail(), user.getDisplayName(), user.getLanguage());
  }
}
