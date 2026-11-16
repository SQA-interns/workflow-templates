package app.contacts.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Application-wide beans. */
@Configuration
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
