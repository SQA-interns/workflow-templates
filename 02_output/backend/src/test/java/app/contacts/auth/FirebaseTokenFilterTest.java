package app.contacts.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.contacts.service.UserDeletedException;
import app.contacts.service.UserService;
import app.contacts.service.UserView;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class FirebaseTokenFilterTest {

  private static final VerifiedToken ANA =
      new VerifiedToken("uid-ana", "ana.kovac@example.si", "Ana Kovač", true, "password");
  private static final UserView ANA_USER =
      new UserView(UUID.randomUUID(), "ana.kovac@example.si", "Ana Kovač", "sl");

  private final TokenVerifier verifier = mock(TokenVerifier.class);
  private final UserService users = mock(UserService.class);
  private final FirebaseTokenFilter filter = new FirebaseTokenFilter(verifier, users);
  private final MockHttpServletResponse response = new MockHttpServletResponse();
  private final MockFilterChain chain = new MockFilterChain();

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void r00102ValidTokenAuthenticatesTheUserOfTheToken() throws Exception {
    when(verifier.verify("good-token")).thenReturn(Optional.of(ANA));
    when(users.signIn("uid-ana", "ana.kovac@example.si", "Ana Kovač")).thenReturn(ANA_USER);

    filter.doFilter(request("GET", "/api/v1/groups", "Bearer good-token"), response, chain);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication.isAuthenticated()).isTrue();
    CurrentUser current = (CurrentUser) authentication.getPrincipal();
    assertThat(current.user()).isEqualTo(ANA_USER);
    assertThat(current.emailVerified()).isTrue();
    assertThat(chain.getRequest()).isNotNull();
  }

  @Test
  void r00103RequestWithoutTokenStaysUnauthenticated() throws Exception {
    filter.doFilter(request("GET", "/api/v1/groups", null), response, chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(chain.getRequest()).isNotNull();
    verify(verifier, never()).verify(anyString());
  }

  @Test
  void r00103OtherAuthorizationSchemeStaysUnauthenticated() throws Exception {
    filter.doFilter(request("GET", "/api/v1/groups", "Basic YW5hOnB3"), response, chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(verifier, never()).verify(anyString());
  }

  @Test
  void r00103InvalidTokenStaysUnauthenticatedAndCreatesNoUser() throws Exception {
    when(verifier.verify("bad-token")).thenReturn(Optional.empty());

    filter.doFilter(request("GET", "/api/v1/groups", "Bearer bad-token"), response, chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(users, never()).signIn(anyString(), anyString(), anyString());
  }

  static MockHttpServletRequest request(String method, String uri, String authorization) {
    MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
    if (authorization != null) {
      request.addHeader("Authorization", authorization);
    }
    return request;
  }

  @Test
  void r00105UnverifiedPasswordAccountMayOnlyReadMe() throws Exception {
    VerifiedToken unverified =
        new VerifiedToken("uid-ana", "ana.kovac@example.si", null, false, "password");
    when(verifier.verify("unverified")).thenReturn(Optional.of(unverified));
    when(users.signIn("uid-ana", "ana.kovac@example.si", null)).thenReturn(ANA_USER);

    filter.doFilter(request("GET", "/api/v1/me", "Bearer unverified"), response, chain);

    assertThat(chain.getRequest()).isNotNull();
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  void r00105UnverifiedPasswordAccountGets403ForEveryOtherRequest() throws Exception {
    VerifiedToken unverified =
        new VerifiedToken("uid-ana", "ana.kovac@example.si", null, false, "password");
    when(verifier.verify("unverified")).thenReturn(Optional.of(unverified));
    when(users.signIn("uid-ana", "ana.kovac@example.si", null)).thenReturn(ANA_USER);

    filter.doFilter(request("POST", "/api/v1/groups", "Bearer unverified"), response, chain);

    assertThat(chain.getRequest()).isNull();
    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getContentAsString()).contains("\"code\":\"auth.emailNotVerified\"");
  }

  @Test
  void r00105UnverifiedPasswordAccountMayNotChangeMe() throws Exception {
    VerifiedToken unverified =
        new VerifiedToken("uid-ana", "ana.kovac@example.si", null, false, "password");
    when(verifier.verify("unverified")).thenReturn(Optional.of(unverified));
    when(users.signIn("uid-ana", "ana.kovac@example.si", null)).thenReturn(ANA_USER);

    filter.doFilter(request("PATCH", "/api/v1/me", "Bearer unverified"), response, chain);

    assertThat(response.getStatus()).isEqualTo(403);
  }

  @Test
  void r00105UnverifiedGoogleAccountIsNotStopped() throws Exception {
    VerifiedToken google =
        new VerifiedToken(
            "uid-marko", "marko.kranjc@gmail.com", "Marko Kranjc", false, "google.com");
    when(verifier.verify("google")).thenReturn(Optional.of(google));
    when(users.signIn("uid-marko", "marko.kranjc@gmail.com", "Marko Kranjc")).thenReturn(ANA_USER);

    filter.doFilter(request("GET", "/api/v1/groups", "Bearer google"), response, chain);

    assertThat(chain.getRequest()).isNotNull();
  }

  @Test
  void d20TokenOfADeletedUserStaysUnauthenticated() throws Exception {
    when(verifier.verify("deleted")).thenReturn(Optional.of(ANA));
    when(users.signIn("uid-ana", "ana.kovac@example.si", "Ana Kovač"))
        .thenThrow(new UserDeletedException());

    filter.doFilter(request("GET", "/api/v1/me", "Bearer deleted"), response, chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(chain.getRequest()).isNotNull();
  }
}
