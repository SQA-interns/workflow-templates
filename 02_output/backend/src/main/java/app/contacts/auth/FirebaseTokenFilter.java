package app.contacts.auth;

import app.contacts.service.UserDeletedException;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates a request by its Firebase ID token and provisions its User (ADR-03). A request
 * without a valid token, or with the token of a deleted User (D-20), stays unauthenticated and is
 * answered 401 (R-001-03). An e-mail/password account whose e-mail is not verified may only read
 * {@code /me}; everything else is 403 (R-001-05, D-16).
 */
public class FirebaseTokenFilter extends OncePerRequestFilter {

  private static final String BEARER = "Bearer ";
  private static final String PASSWORD_PROVIDER = "password";
  private static final String ME = "/api/v1/me";

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
    Optional<VerifiedToken> token = bearerToken(request).flatMap(verifier::verify);
    if (token.isPresent() && authenticate(token.get()) && mustVerifyEmail(token.get(), request)) {
      Problems.write(response, HttpStatus.FORBIDDEN, "auth.emailNotVerified");
      return;
    }
    chain.doFilter(request, response);
  }

  /** Sets the signed-in User; false for a deleted User (D-20). */
  private boolean authenticate(VerifiedToken token) {
    UserView user;
    try {
      user = users.signIn(token.uid(), token.email(), token.name());
    } catch (UserDeletedException deleted) {
      return false;
    }
    CurrentUser current = new CurrentUser(user, token.emailVerified());
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(current, null, List.of()));
    return true;
  }

  private static boolean mustVerifyEmail(VerifiedToken token, HttpServletRequest request) {
    boolean readsMe = "GET".equals(request.getMethod()) && ME.equals(request.getRequestURI());
    return !token.emailVerified() && PASSWORD_PROVIDER.equals(token.signInProvider()) && !readsMe;
  }

  private static Optional<String> bearerToken(HttpServletRequest request) {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header == null || !header.startsWith(BEARER)) {
      return Optional.empty();
    }
    return Optional.of(header.substring(BEARER.length()).strip()).filter(t -> !t.isEmpty());
  }
}
