package app.contacts.web;

/**
 * A change of the signed-in User ({@code MeUpdate} of the contract).
 *
 * @param language {@code sl} or {@code en} (R-020-02)
 */
public record MeUpdate(String language) {}
