package app.contacts.repository;

import app.contacts.domain.Invitation;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** Invitations of one Group (AR-04); a token names its Group, so no lookup crosses Groups. */
public interface InvitationRepository
    extends Repository<Invitation, UUID>, GroupOwnedStore<Invitation> {

  /**
   * The open Invitations of the Group that have not expired, oldest first (R-004-08).
   *
   * @param groupId the Group
   * @param now the current time
   * @return the Invitations
   */
  @Query(
      """
      SELECT i FROM Invitation i
      WHERE i.groupId = :groupId AND i.state = 'open' AND i.expiresAt > :now
      ORDER BY i.createdAt, i.id
      """)
  List<Invitation> findOpen(UUID groupId, Instant now);

  /**
   * The open Invitations of the Group for the e-mail, compared case-insensitively (R-004-03,
   * BR-11).
   *
   * @param groupId the Group
   * @param email the e-mail
   * @param now the current time
   * @return the Invitations, locked for the revocation
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      SELECT i FROM Invitation i
      WHERE i.groupId = :groupId AND lower(i.email) = lower(:email) AND i.state = 'open'
        AND i.expiresAt > :now
      """)
  List<Invitation> findOpenForEmail(UUID groupId, String email, Instant now);

  /**
   * The Invitation of the Group with the token hash, locked so it is used once (R-004-04, SR-05).
   *
   * @param groupId the Group the token names
   * @param tokenHash the SHA-256 hash of the token
   * @return the Invitation, in any state
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT i FROM Invitation i WHERE i.groupId = :groupId AND i.tokenHash = :tokenHash")
  Optional<Invitation> findByToken(UUID groupId, byte[] tokenHash);

  /**
   * The Invitation of the Group with the id, locked for the revocation (R-004-09).
   *
   * @param groupId the Group
   * @param id the Invitation id
   * @return the Invitation, in any state
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT i FROM Invitation i WHERE i.groupId = :groupId AND i.id = :id")
  Optional<Invitation> findInGroup(UUID groupId, UUID id);
}
