package app.contacts.acceptance.support;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import tools.jackson.databind.JsonNode;

/** Client of the Contacts API ({@code docs/02_contracts/openapi.yaml}, server {@code /api/v1}). */
public final class Api {

  private final HttpClient http =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  private final String base;

  /** The API at the base URL, e.g. {@code http://localhost:8080/api/v1}. */
  public Api(String base) {
    this.base = base;
  }

  /** GET as the User. */
  public Res get(TestUser user, String path) {
    return send("GET", path, token(user), null);
  }

  /** POST as the User; body may be null. */
  public Res post(TestUser user, String path, JsonNode body) {
    return send("POST", path, token(user), body);
  }

  /** PUT as the User. */
  public Res put(TestUser user, String path, JsonNode body) {
    return send("PUT", path, token(user), body);
  }

  /** PATCH as the User. */
  public Res patch(TestUser user, String path, JsonNode body) {
    return send("PATCH", path, token(user), body);
  }

  /** DELETE as the User; body may be null. */
  public Res delete(TestUser user, String path, JsonNode body) {
    return send("DELETE", path, token(user), body);
  }

  /** Any request with a raw bearer token (null: no Authorization header). */
  public Res send(String method, String path, String bearer, JsonNode body) {
    HttpRequest.Builder request =
        HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(120));
    if (bearer != null) {
      request.header("Authorization", "Bearer " + bearer);
    }
    if (body != null) {
      request.header("Content-Type", "application/json");
      request.method(method, HttpRequest.BodyPublishers.ofString(Json.write(body)));
    } else {
      request.method(method, HttpRequest.BodyPublishers.noBody());
    }
    try {
      HttpResponse<byte[]> response =
          http.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
      return new Res(response.statusCode(), response.body(), response.headers());
    } catch (IOException e) {
      throw new IllegalStateException("request failed: " + method + " " + path, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("interrupted", e);
    }
  }

  private static String token(TestUser user) {
    return user == null ? null : user.token();
  }
}
