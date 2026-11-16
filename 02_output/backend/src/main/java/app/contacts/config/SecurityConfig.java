package app.contacts.config;

import app.contacts.auth.FirebaseTokenFilter;
import app.contacts.auth.ProblemEntryPoint;
import app.contacts.auth.RateLimitFilter;
import app.contacts.auth.TokenVerifier;
import app.contacts.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

/**
 * Stateless API security: only the health check is public (SR-01); every other request needs a
 * valid Firebase ID token (ADR-03, R-001-02, R-001-03).
 */
@Configuration
public class SecurityConfig {

  /**
   * The filter chain.
   *
   * @param http the security builder
   * @param verifier checks ID tokens
   * @param users provisions Users
   * @param properties the settings (rate limit)
   * @return the filter chain
   * @throws Exception when the configuration is invalid
   */
  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http, TokenVerifier verifier, UserService users, AppProperties properties)
      throws Exception {
    http.csrf(csrf -> csrf.disable())
        .httpBasic(basic -> basic.disable())
        .formLogin(form -> form.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterBefore(
            new FirebaseTokenFilter(verifier, users), AnonymousAuthenticationFilter.class)
        .addFilterAfter(
            new RateLimitFilter(properties.rateLimitPerMinute()), FirebaseTokenFilter.class)
        .exceptionHandling(e -> e.authenticationEntryPoint(new ProblemEntryPoint()))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers("/actuator/health", "/actuator/health/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated());
    return http.build();
  }

  /**
   * No local accounts exist; stops Spring Boot from generating a default password.
   *
   * @return an empty user store
   */
  @Bean
  public InMemoryUserDetailsManager noLocalUsers() {
    return new InMemoryUserDetailsManager();
  }
}
