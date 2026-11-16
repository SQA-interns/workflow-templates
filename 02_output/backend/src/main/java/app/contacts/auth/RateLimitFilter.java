package app.contacts.auth;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limits each signed-in User to a number of API requests per minute; above it 429 {@code
 * rateLimited} with {@code Retry-After} in seconds (SR-03, ADR-03). In memory: one backend instance
 * (ADR-11).
 */
public class RateLimitFilter extends OncePerRequestFilter {

  private static final Duration PERIOD = Duration.ofMinutes(1);

  private final int requestsPerMinute;
  private final Map<UUID, Bucket> buckets = new ConcurrentHashMap<>();

  /**
   * The filter.
   *
   * @param requestsPerMinute requests allowed per User and minute ({@code RATE_LIMIT_PER_MINUTE})
   */
  public RateLimitFilter(int requestsPerMinute) {
    this.requestsPerMinute = requestsPerMinute;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof CurrentUser user) {
      ConsumptionProbe probe =
          buckets.computeIfAbsent(user.id(), id -> newBucket()).tryConsumeAndReturnRemaining(1);
      if (!probe.isConsumed()) {
        response.setHeader(HttpHeaders.RETRY_AFTER, retryAfterSeconds(probe));
        Problems.write(response, HttpStatus.TOO_MANY_REQUESTS, "rateLimited");
        return;
      }
    }
    chain.doFilter(request, response);
  }

  private Bucket newBucket() {
    return Bucket.builder()
        .addLimit(
            Bandwidth.builder()
                .capacity(requestsPerMinute)
                .refillIntervally(requestsPerMinute, PERIOD)
                .build())
        .build();
  }

  private static String retryAfterSeconds(ConsumptionProbe probe) {
    long seconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill() - 1) + 1;
    return Long.toString(Math.max(1, seconds));
  }
}
