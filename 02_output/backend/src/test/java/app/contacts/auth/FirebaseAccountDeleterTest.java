package app.contacts.auth;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.firebase.ErrorCode;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import org.junit.jupiter.api.Test;

class FirebaseAccountDeleterTest {

  private final FirebaseAuth firebase = mock(FirebaseAuth.class);
  private final FirebaseAccountDeleter deleter = new FirebaseAccountDeleter(firebase::deleteUser);

  @Test
  void r02101DeletesTheFirebaseAccountOfTheUid() throws Exception {
    deleter.purge("uid-marko");

    verify(firebase).deleteUser("uid-marko");
  }

  @Test
  void r02101AnAccountThatNoLongerExistsCountsAsDeleted() throws Exception {
    doThrow(failure(ErrorCode.NOT_FOUND, AuthErrorCode.USER_NOT_FOUND))
        .when(firebase)
        .deleteUser("uid-gone");

    assertThatCode(() -> deleter.purge("uid-gone")).doesNotThrowAnyException();
  }

  @Test
  void r02101AnyOtherFailureIsRaisedSoTheDeletionRollsBack() throws Exception {
    doThrow(failure(ErrorCode.UNAVAILABLE, null)).when(firebase).deleteUser("uid-marko");

    assertThatThrownBy(() -> deleter.purge("uid-marko"))
        .isInstanceOf(IllegalStateException.class)
        .hasCauseInstanceOf(FirebaseAuthException.class);
  }

  private static FirebaseAuthException failure(ErrorCode code, AuthErrorCode authCode) {
    return new FirebaseAuthException(code, "failed", null, null, authCode);
  }
}
