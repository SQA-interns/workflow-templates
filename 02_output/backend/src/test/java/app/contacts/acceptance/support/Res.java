package app.contacts.acceptance.support;

import java.net.URLDecoder;
import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;

/** One HTTP response of the API. */
public record Res(int status, byte[] bytes, HttpHeaders headers) {

  private static final Pattern FILENAME_STAR = Pattern.compile("filename\\*=UTF-8''([^;]+)");
  private static final Pattern FILENAME = Pattern.compile("filename=\"?([^\";]+)\"?");

  /** The body as text. */
  public String body() {
    return new String(bytes, StandardCharsets.UTF_8);
  }

  /** The body as JSON (a missing node for an empty body or a binary file). */
  public JsonNode json() {
    String type = headers.firstValue("Content-Type").orElse("");
    if (!type.contains("json")) {
      return Json.parse("");
    }
    return Json.parse(body());
  }

  /** The {@code code} of an RFC 9457 problem (specification §5). */
  public String code() {
    return Json.text(json().path("code"));
  }

  /** The file name of {@code Content-Disposition} (RFC 6266, also the {@code filename*} form). */
  public String fileName() {
    String disposition = headers.firstValue("Content-Disposition").orElse("");
    Matcher star = FILENAME_STAR.matcher(disposition);
    if (star.find()) {
      return URLDecoder.decode(star.group(1).replace("+", "%2B"), StandardCharsets.UTF_8);
    }
    Matcher plain = FILENAME.matcher(disposition);
    return plain.find() ? plain.group(1) : null;
  }

  @Override
  public String toString() {
    return status + " " + (bytes.length > 2000 ? body().substring(0, 2000) : body());
  }
}
