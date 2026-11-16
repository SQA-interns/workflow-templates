package app.contacts.auth;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import tools.jackson.databind.json.JsonMapper;

/** RFC 9457 problems of the API (ADR-06): body {type, title, status, code}, no request data. */
public final class Problems {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  private Problems() {}

  /**
   * Writes a problem from a filter, before any controller runs (SB-07).
   *
   * @param response the response
   * @param status the status
   * @param code the catalog code (specification §5)
   * @throws IOException when the response cannot be written
   */
  static void write(HttpServletResponse response, HttpStatus status, String code)
      throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    response.getWriter().write(JSON.writeValueAsString(body(status, code)));
  }

  /**
   * The body of a problem; callers may add {@code params} or {@code errors}.
   *
   * @param status the status
   * @param code the catalog code (specification §5)
   * @return a mutable body
   */
  public static Map<String, Object> body(HttpStatus status, String code) {
    Map<String, Object> problem = new LinkedHashMap<>();
    problem.put("type", "about:blank");
    problem.put("title", status.getReasonPhrase());
    problem.put("status", status.value());
    problem.put("code", code);
    return problem;
  }
}
