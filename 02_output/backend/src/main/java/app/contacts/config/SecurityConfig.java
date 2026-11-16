package app.contacts.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * Stateless API security: only the health check is public (SR-01). The Firebase ID token filter is
 * added by US-001 (ADR-03); until then every API request is answered with HTTP 401.
 */
@Configuration
public class SecurityConfig {

  /**
   * The filter chain.
   *
   * @param http the security builder
   * @return the filter chain
   * @throws Exception when the configuration is invalid
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .httpBasic(basic -> basic.disable())
        .formLogin(form -> form.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
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
