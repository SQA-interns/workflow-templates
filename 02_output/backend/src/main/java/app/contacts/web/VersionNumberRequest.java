package app.contacts.web;

/**
 * The current Version number the client read (contract {@code VersionNumberRequest}, BR-06).
 *
 * @param versionNumber the Version number, {@code null} when missing
 */
public record VersionNumberRequest(Integer versionNumber) {}
