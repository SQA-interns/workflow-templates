package app.contacts.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/** Answers a request without a valid ID token: 401 {@code auth.unauthenticated} (R-001-03). */
public class ProblemEntryPoint implements AuthenticationEntryPoint {

  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException reason)
      throws IOException {
    Problems.write(response, HttpStatus.UNAUTHORIZED, "auth.unauthenticated");
  }
}
