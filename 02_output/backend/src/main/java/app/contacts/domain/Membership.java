package app.contacts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

/** A User's Membership of a Group with a Role (glossary "Member", table membership). */
@Entity
@Table(name = "membership")
public class Membership {

  @EmbeddedId private MembershipId id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role;

  /** For JPA. */
  protected Membership() {}

  /**
   * A new Membership.
   *
   * @param groupId the Group
   * @param userId the User
   * @param role the Role
   * @return the new, unsaved Membership
   */
  public static Membership of(UUID groupId, UUID userId, Role role) {
    Membership membership = new Membership();
    membership.id = new MembershipId(groupId, userId);
    membership.role = role;
    return membership;
  }

  /**
   * Gives the Member another Role (R-005-02).
   *
   * @param newRole the Role
   */
  public void changeRole(Role newRole) {
    this.role = newRole;
  }

  public MembershipId getId() {
    return id;
  }

  public Role getRole() {
    return role;
  }
}
