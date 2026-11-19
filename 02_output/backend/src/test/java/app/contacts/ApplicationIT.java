package app.contacts;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Skeleton check: the schema migrates (persistence contract), health is public, the API is not. */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "DB_URL=jdbc:postgresql://replaced-by-service-connection/contacts",
      "DB_USER=unused",
      "DB_PASSWORD=unused",
      "APP_PUBLIC_URL=http://localhost:8080",
      "FIREBASE_PROJECT_ID=demo-contacts"
    })
class ApplicationIT {

  @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16.15");

  static {
    POSTGRES.start();
  }

  private final HttpClient http = HttpClient.newHttpClient();

  @LocalServerPort private int port;

  @Autowired private Flyway flyway;

  @Test
  void schemaMigratesToVersion1() {
    assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("1");
  }

  @Test
  void es09HealthIsPublicAndUp() throws Exception {
    HttpResponse<String> response = get("/actuator/health");
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("\"UP\"");
  }

  @Test
  void sr01ApiWithoutTokenIsUnauthenticated() throws Exception {
    assertThat(get("/api/v1/groups").statusCode()).isEqualTo(401);
  }

  @Test
  void d30ApiSetsNoCookie() throws Exception {
    HttpResponse<String> api = get("/api/v1/groups");
    HttpResponse<String> health = get("/actuator/health");
    assertThat(api.headers().allValues("Set-Cookie")).isEmpty();
    assertThat(health.headers().allValues("Set-Cookie")).isEmpty();
  }

  @Test
  void sr09PreflightFromTheFrontendOriginIsAllowed() throws Exception {
    HttpResponse<String> response = preflight("http://localhost:8080");
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.headers().firstValue("Access-Control-Allow-Origin"))
        .contains("http://localhost:8080");
    assertThat(response.headers().firstValue("Access-Control-Allow-Headers").orElse(""))
        .containsIgnoringCase("authorization");
  }

  @Test
  void sr09PreflightFromAnotherOriginIsRefused() throws Exception {
    HttpResponse<String> response = preflight("https://andere.example.si");
    assertThat(response.statusCode()).isEqualTo(403);
    assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
  }

  private HttpResponse<String> preflight(String origin) throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/groups"))
            .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
            .header("Origin", origin)
            .header("Access-Control-Request-Method", "PUT")
            .header("Access-Control-Request-Headers", "authorization, content-type")
            .build();
    return http.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> get(String path) throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
    return http.send(request, HttpResponse.BodyHandlers.ofString());
  }
}
