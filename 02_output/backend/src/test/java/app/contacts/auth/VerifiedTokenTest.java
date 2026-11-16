package app.contacts.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class VerifiedTokenTest {

  @Test
  void r00104ReadsUidEmailNameVerificationAndSignInProviderFromTheClaims() {
    VerifiedToken token =
        VerifiedToken.of(
            Map.of(
                "sub", "uid-marko",
                "email", "marko.kranjc@gmail.com",
                "name", "Marko Kranjc",
                "email_verified", true,
                "firebase", Map.of("sign_in_provider", "google.com")));

    assertThat(token)
        .isEqualTo(
            new VerifiedToken(
                "uid-marko", "marko.kranjc@gmail.com", "Marko Kranjc", true, "google.com"));
  }

  @Test
  void r00105MissingVerificationAndNameReadAsUnverifiedWithoutName() {
    VerifiedToken token =
        VerifiedToken.of(
            Map.of(
                "sub",
                "uid-ana",
                "email",
                "ana.kovac@example.si",
                "firebase",
                Map.of("sign_in_provider", "password")));

    assertThat(token.emailVerified()).isFalse();
    assertThat(token.name()).isNull();
    assertThat(token.signInProvider()).isEqualTo("password");
  }
}
