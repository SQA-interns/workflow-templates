package app.contacts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;

/**
 * The key of a Membership: one User in one Group (Data model "unique pair").
 *
 * @param groupId the Group
 * @param userId the User
 */
@Embeddable
public record MembershipId(
    @Column(name = "group_id") UUID groupId, @Column(name = "user_id") UUID userId)
    implements Serializable {}
