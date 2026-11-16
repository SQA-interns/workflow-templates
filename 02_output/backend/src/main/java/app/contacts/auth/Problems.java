package app.contacts.auth;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.json.JsonMapper;

/** RFC 9457 problems written by the filters, before any controller runs (ADR-06). */
final class Problems {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  private Problems() {}

  /**
   * Writes a problem without any request data (SB-07).
   *
   * @param response the response
   * @param status the status
   * @param code the catalog code (specification §5)
   * @throws IOException when the response cannot be written
   */
  static void write(HttpServletResponse response, HttpStatus status, String code)
      throws IOException {
    Map<String, Object> problem = new LinkedHashMap<>();
    problem.put("type", "about:blank");
    problem.put("title", status.getReasonPhrase());
    problem.put("status", status.value());
    problem.put("code", code);
    response.setStatus(status.value());
    response.setContentType("application/problem+json");
    response.setCharacterEncoding("UTF-8");
    response.getWriter().write(JSON.writeValueAsString(problem));
  }
}
