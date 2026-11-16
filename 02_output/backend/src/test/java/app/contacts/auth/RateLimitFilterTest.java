package app.contacts.auth;

import static org.assertj.core.api.Assertions.assertThat;

import app.contacts.service.UserView;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class RateLimitFilterTest {

  private final RateLimitFilter filter = new RateLimitFilter(3);

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void sr03RequestsWithinTheLimitPass() throws Exception {
    signIn(UUID.randomUUID());

    for (int i = 0; i < 3; i++) {
      assertThat(send().getStatus()).isEqualTo(200);
    }
  }

  @Test
  void sr03TheRequestAboveTheLimitGets429WithRetryAfter() throws Exception {
    signIn(UUID.randomUUID());
    for (int i = 0; i < 3; i++) {
      send();
    }

    MockHttpServletResponse response = send();

    assertThat(response.getStatus()).isEqualTo(429);
    assertThat(Integer.parseInt(response.getHeader("Retry-After"))).isBetween(1, 60);
    assertThat(response.getContentAsString()).contains("\"code\":\"rateLimited\"");
  }

  @Test
  void sr03EachUserHasItsOwnLimit() throws Exception {
    signIn(UUID.randomUUID());
    for (int i = 0; i < 4; i++) {
      send();
    }

    signIn(UUID.randomUUID());

    assertThat(send().getStatus()).isEqualTo(200);
  }

  @Test
  void sr03UnauthenticatedRequestsAreLeftToTheEntryPoint() throws Exception {
    for (int i = 0; i < 5; i++) {
      assertThat(send().getStatus()).isEqualTo(200);
    }
  }

  private MockHttpServletResponse send() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(
        new MockHttpServletRequest("GET", "/api/v1/groups"), response, new MockFilterChain());
    return response;
  }

  private static void signIn(UUID id) {
    CurrentUser user = new CurrentUser(new UserView(id, "ana@example.si", "Ana", "sl"), true);
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(user, null, List.of()));
  }
}
