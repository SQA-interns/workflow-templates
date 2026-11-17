package app.contacts.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Application-wide beans; scheduled jobs such as the removal job run (ADR-10). */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(AppProperties.class)
public class ApplicationConfig {

  /**
   * The clock in UTC (project-wide facts); replaced in tests to check time boundaries.
   *
   * @return the system clock in UTC
   */
  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
