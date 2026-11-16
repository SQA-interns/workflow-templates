package app.contacts.auth;

import app.contacts.service.UserService;
import app.contacts.service.UserView;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates a request by its Firebase ID token and provisions its User (ADR-03). A request
 * without a valid token stays unauthenticated and is answered 401 (R-001-03).
 */
public class FirebaseTokenFilter extends OncePerRequestFilter {

  private static final String BEARER = "Bearer ";

  private final TokenVerifier verifier;
  private final UserService users;

  /**
   * The filter.
   *
   * @param verifier checks ID tokens
   * @param users provisions Users
   */
  public FirebaseTokenFilter(TokenVerifier verifier, UserService users) {
    this.verifier = verifier;
    this.users = users;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    bearerToken(request).flatMap(verifier::verify).ifPresent(this::authenticate);
    chain.doFilter(request, response);
  }

  private void authenticate(VerifiedToken token) {
    UserView user = users.signIn(token.uid(), token.email(), token.name());
    CurrentUser current = new CurrentUser(user, token.emailVerified());
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(current, null, List.of()));
  }

  private static Optional<String> bearerToken(HttpServletRequest request) {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header == null || !header.startsWith(BEARER)) {
      return Optional.empty();
    }
    return Optional.of(header.substring(BEARER.length()).strip()).filter(t -> !t.isEmpty());
  }
}
