package app.contacts.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings that differ between environments (environments.md, ES-01, ADR-05).
 *
 * @param publicUrl the product URL, used for CORS and Invitation links
 * @param rateLimitPerMinute the API requests allowed per User and minute (SR-03)
 * @param firebase the Firebase settings
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(String publicUrl, int rateLimitPerMinute, Firebase firebase) {

  /**
   * Firebase settings.
   *
   * @param projectId the Firebase project id
   * @param authEmulatorHost the Auth emulator host, set only in test and local
   * @param serviceAccountFile path of the service account file, needed only to delete accounts
   */
  public record Firebase(String projectId, String authEmulatorHost, String serviceAccountFile) {}
}
