package app.contacts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * A single-use link that makes the invited User a Member with a Role (glossary "Invitation", table
 * invitation, US-004); only the hash of its token is kept (SR-05).
 */
@Entity
@Table(name = "invitation")
public class Invitation implements GroupOwned {

  /** How long an Invitation is valid (R-004-01, D-17). */
  public static final Duration VALIDITY = Duration.ofDays(7);

  /** Open: can still be accepted or revoked until {@code expiresAt}. */
  public static final String OPEN = "open";

  /** Revoked by the Owner or by a newer Invitation (R-004-03, R-004-09). */
  public static final String REVOKED = "revoked";

  @Id private UUID id;

  @Column(name = "group_id", nullable = false, updatable = false)
  private UUID groupId;

  @Column(nullable = false, updatable = false)
  private String email;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false)
  private Role role;

  @Column(name = "token_hash", nullable = false, updatable = false)
  private byte[] tokenHash;

  @Column(nullable = false)
  private String state;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "created_by", nullable = false, updatable = false)
  private UUID createdBy;

  @Column(name = "expires_at", nullable = false, updatable = false)
  private Instant expiresAt;

  /** For JPA. */
  protected Invitation() {}

  /**
   * A new open Invitation, valid for 7 days (R-004-01).
   *
   * @param groupId the Group
   * @param email the invitee's e-mail as entered
   * @param role {@code EDITOR} or {@code VIEWER}
   * @param tokenHash the hash of the token (SR-05)
   * @param createdBy the Owner
   * @param now the time of creation
   * @return the unsaved Invitation
   */
  public static Invitation create(
      UUID groupId, String email, Role role, byte[] tokenHash, UUID createdBy, Instant now) {
    Invitation invitation = new Invitation();
    invitation.id = UUID.randomUUID();
    invitation.groupId = groupId;
    invitation.email = email;
    invitation.role = role;
    invitation.tokenHash = tokenHash.clone();
    invitation.state = OPEN;
    invitation.createdAt = now;
    invitation.createdBy = createdBy;
    invitation.expiresAt = now.plus(VALIDITY);
    return invitation;
  }

  /** Revokes the Invitation (R-004-03, R-004-09). */
  public void revoke() {
    state = REVOKED;
  }

  public UUID getId() {
    return id;
  }

  @Override
  public UUID getGroupId() {
    return groupId;
  }

  public String getEmail() {
    return email;
  }

  public Role getRole() {
    return role;
  }

  public String getState() {
    return state;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }
}
