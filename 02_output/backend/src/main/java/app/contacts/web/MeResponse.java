package app.contacts.web;

import java.util.UUID;

/**
 * The signed-in User ({@code Me} of the contract).
 *
 * @param id the User id
 * @param email the e-mail
 * @param displayName the display name
 * @param language {@code sl} or {@code en}
 * @param emailVerified whether the e-mail is verified (R-001-05)
 */
public record MeResponse(
    UUID id, String email, String displayName, String language, boolean emailVerified) {}
