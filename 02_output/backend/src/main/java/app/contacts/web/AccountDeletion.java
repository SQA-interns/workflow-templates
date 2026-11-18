package app.contacts.web;

/**
 * The confirmation of "Delete my account" ({@code AccountDeletion} of the contract).
 *
 * @param confirmEmail the e-mail address the User typed (R-021-01, EX-021-01)
 */
public record AccountDeletion(String confirmEmail) {}
