package app.contacts.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.firebase.ErrorCode;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FirebaseTokenVerifierTest {

  private final FirebaseAuth firebase = mock(FirebaseAuth.class);
  private final FirebaseTokenVerifier verifier = new FirebaseTokenVerifier(firebase);

  @Test
  void r00102TokenAcceptedByFirebaseGivesItsClaims() throws Exception {
    FirebaseToken token = mock(FirebaseToken.class);
    when(token.getClaims())
        .thenReturn(
            Map.of(
                "sub",
                "uid-ana",
                "email",
                "ana.kovac@example.si",
                "email_verified",
                true,
                "firebase",
                Map.of("sign_in_provider", "password")));
    when(firebase.verifyIdToken("good")).thenReturn(token);

    assertThat(verifier.verify("good"))
        .contains(new VerifiedToken("uid-ana", "ana.kovac@example.si", null, true, "password"));
  }

  @Test
  void r00103ExpiredOrForeignTokenIsRejected() throws Exception {
    when(firebase.verifyIdToken("expired"))
        .thenThrow(
            new FirebaseAuthException(
                ErrorCode.INVALID_ARGUMENT, "expired", null, null, AuthErrorCode.EXPIRED_ID_TOKEN));

    assertThat(verifier.verify("expired")).isEmpty();
  }

  @Test
  void ex00104MalformedTokenIsRejected() throws Exception {
    when(firebase.verifyIdToken("garbage")).thenThrow(new IllegalArgumentException("not a JWT"));

    assertThat(verifier.verify("garbage")).isEmpty();
  }
}
