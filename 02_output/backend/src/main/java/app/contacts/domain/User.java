package app.contacts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** A person with a Firebase account who uses the product (glossary "User", table app_user). */
@Entity
@Table(name = "app_user")
public class User {

  /** Language of a new User (R-001-04, project-wide facts: Slovenian is the default). */
  public static final String DEFAULT_LANGUAGE = "sl";

  /** The languages of the product (project-wide facts, R-020-02). */
  public static final Set<String> LANGUAGES = Set.of("sl", "en");

  @Id private UUID id;

  @Column(name = "firebase_uid", nullable = false, updatable = false)
  private String firebaseUid;

  private String email;

  @Column(name = "display_name")
  private String displayName;

  @Column(nullable = false)
  private String language;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  /** For JPA. */
  protected User() {}

  /**
   * A new User in the default language (R-001-04).
   *
   * @param firebaseUid the Firebase uid of the account
   * @param email the e-mail of the account
   * @param displayName the display name
   * @param now the creation time
   * @return the new, unsaved User
   */
  public static User create(String firebaseUid, String email, String displayName, Instant now) {
    User user = new User();
    user.id = UUID.randomUUID();
    user.firebaseUid = firebaseUid;
    user.email = email;
    user.displayName = displayName;
    user.language = DEFAULT_LANGUAGE;
    user.createdAt = now;
    return user;
  }

  /**
   * Chooses the language of the product for this User (R-020-02).
   *
   * @param language one of {@link #LANGUAGES}, checked by the caller
   */
  public void changeLanguage(String language) {
    this.language = language;
  }

  /**
   * Erases the e-mail and display name and marks the User deleted; id and Firebase uid stay, so
   * Versions keep their User and the account's tokens stay refused (D-20, R-021-03).
   *
   * @param now the deletion time
   */
  public void anonymise(Instant now) {
    this.email = null;
    this.displayName = null;
    this.deletedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public String getFirebaseUid() {
    return firebaseUid;
  }

  public String getEmail() {
    return email;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getLanguage() {
    return language;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getDeletedAt() {
    return deletedAt;
  }
}
