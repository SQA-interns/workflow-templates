package app.contacts.repository;

import app.contacts.domain.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Users; not owned by a Group, so exempt from AR-04 (ADR-02). */
public interface UserRepository extends JpaRepository<User, UUID> {

  /**
   * The User of a Firebase account.
   *
   * @param firebaseUid the Firebase uid
   * @return the User, if the account has called the API before
   */
  Optional<User> findByFirebaseUid(String firebaseUid);
}
