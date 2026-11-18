package app.contacts.web;

import java.lang.reflect.Method;
import java.util.Arrays;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** The status a controller method answers with, from its {@link ResponseStatus} (200 without). */
final class ResponseStatuses {

  private ResponseStatuses() {}

  /**
   * The status of the controller's handler method with this name.
   *
   * @param controller the controller class
   * @param method the handler method's name (unique in the controller)
   * @return the status of its {@code @ResponseStatus}, or 200 OK without one
   */
  static HttpStatus of(Class<?> controller, String method) {
    Method handler =
        Arrays.stream(controller.getDeclaredMethods())
            .filter(m -> m.getName().equals(method))
            .findFirst()
            .orElseThrow();
    ResponseStatus status = handler.getAnnotation(ResponseStatus.class);
    return status == null ? HttpStatus.OK : status.value();
  }
}
