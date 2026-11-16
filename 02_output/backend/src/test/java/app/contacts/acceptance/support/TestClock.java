package app.contacts.acceptance.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * The clock of the backend under test (ADR-10: a {@code java.time.Clock} bean). Runs with the
 * system time until a test sets it; then it stands still until the test moves it.
 */
public final class TestClock extends Clock {

  /** Time zone of everything shown to a User (project-wide facts). */
  public static final ZoneId LJUBLJANA = ZoneId.of("Europe/Ljubljana");

  private volatile Instant fixed;

  @Override
  public ZoneId getZone() {
    return ZoneOffset.UTC;
  }

  @Override
  public Clock withZone(ZoneId zone) {
    TestClock self = this;
    return new Clock() {
      @Override
      public ZoneId getZone() {
        return zone;
      }

      @Override
      public Clock withZone(ZoneId other) {
        return self.withZone(other);
      }

      @Override
      public Instant instant() {
        return self.instant();
      }
    };
  }

  @Override
  public Instant instant() {
    Instant current = fixed;
    return current != null ? current : Instant.now();
  }

  /** Sets the time to a wall-clock time in Europe/Ljubljana, e.g. {@code 2026-03-05T11:00}. */
  public Instant setLjubljana(String localDateTime) {
    return set(LocalDateTime.parse(localDateTime).atZone(LJUBLJANA).toInstant());
  }

  /** Sets the time to a UTC instant. */
  public Instant set(Instant instant) {
    fixed = instant;
    return instant;
  }

  /** Moves the time forward. */
  public Instant advance(Duration duration) {
    return set(instant().plus(duration));
  }

  /** Back to the system time. */
  public void reset() {
    fixed = null;
  }

  /** Registers the clock as the primary {@link Clock} bean of the application context. */
  @TestConfiguration(proxyBeanMethods = false)
  public static class Config {
    @Bean
    @Primary
    TestClock testClock() {
      return new TestClock();
    }
  }
}
