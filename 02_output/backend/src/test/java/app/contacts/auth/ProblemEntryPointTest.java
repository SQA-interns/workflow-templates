package app.contacts.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;

class ProblemEntryPointTest {

  @Test
  void r00103UnauthenticatedRequestGets401ProblemWithCodeAndNoData() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();

    new ProblemEntryPoint()
        .commence(
            new MockHttpServletRequest("GET", "/api/v1/groups"),
            response,
            new InsufficientAuthenticationException("no token"));

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentType()).startsWith("application/problem+json");
    assertThat(response.getContentAsString())
        .contains("\"code\":\"auth.unauthenticated\"")
        .contains("\"status\":401")
        .doesNotContain("no token");
  }
}
