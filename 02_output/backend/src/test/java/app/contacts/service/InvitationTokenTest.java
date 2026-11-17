package app.contacts.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class InvitationTokenTest {

  private static final UUID GROUP = UUID.fromString("0b6a3c1e-8f0a-4c55-9d7e-2a1b3c4d5e6f");

  @Test
  void r00401TokenIsUrlSafeAndNamesItsGroup() {
    InvitationToken token = InvitationToken.issue(GROUP);

    assertThat(token.value()).matches("[A-Za-z0-9_-]{22,128}");
    assertThat(token.groupId()).isEqualTo(GROUP);
  }

  @Test
  void sr05TokenCarriesAtLeast128RandomBits() {
    InvitationToken token = InvitationToken.issue(GROUP);
    String random = token.value().substring(22);

    assertThat(Base64.getUrlDecoder().decode(random)).hasSizeGreaterThanOrEqualTo(16);
    Set<String> values = new HashSet<>();
    for (int i = 0; i < 1000; i++) {
      values.add(InvitationToken.issue(GROUP).value());
    }
    assertThat(values).hasSize(1000);
  }

  @Test
  void sr05HashIsSha256OfTheTokenAndNotTheToken() {
    InvitationToken token = InvitationToken.issue(GROUP);

    assertThat(token.hash()).hasSize(32).isNotEqualTo(token.value().getBytes());
    assertThat(InvitationToken.parse(token.value()).orElseThrow().hash()).isEqualTo(token.hash());
  }

  @Test
  void r00406ParseReadsTheGroupOfAnIssuedToken() {
    InvitationToken token = InvitationToken.issue(GROUP);

    assertThat(InvitationToken.parse(token.value())).contains(token);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(
      strings = {
        "",
        "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
        "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
        "AAAAAAAAAAAAAAAAAAAAAA*AAAAAAAAAAAAAAAAAAAAA",
        "AAAAAAAAAAAAAAAAAAAAA/AAAAAAAAAAAAAAAAAAAAAA"
      })
  void ex00408ParseRejectsWhatWasNotIssued(String value) {
    assertThat(InvitationToken.parse(value)).isEmpty();
  }
}
