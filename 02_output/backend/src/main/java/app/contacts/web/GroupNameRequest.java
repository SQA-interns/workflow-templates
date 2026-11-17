package app.contacts.web;

/**
 * The body of creating or renaming a Group (contract {@code GroupName}).
 *
 * @param name the name; checked by the service (R-002-02)
 */
public record GroupNameRequest(String name) {}
