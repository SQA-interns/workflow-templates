package app.contacts.web;

import app.contacts.auth.Problems;
import app.contacts.service.ConflictException;
import app.contacts.service.FieldProblem;
import app.contacts.service.ForbiddenException;
import app.contacts.service.NotFoundException;
import app.contacts.service.ValidationException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Turns exceptions into RFC 9457 problems with the codes of specification §5 (ADR-06); never with a
 * stack trace, SQL, token or Contact data (SB-07).
 */
@RestControllerAdvice
public class ProblemHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ProblemHandler.class);

  /**
   * 404 for unknown items and other Groups' data (BR-02) and for unknown paths.
   *
   * @return the problem
   */
  @ExceptionHandler({NotFoundException.class, NoResourceFoundException.class})
  public ResponseEntity<Map<String, Object>> notFound() {
    return problem(HttpStatus.NOT_FOUND, "notFound");
  }

  /**
   * An id in the path that is not a UUID is no such item (404); a query parameter of the wrong type
   * is an invalid request (400 {@code validation}, contract responses {@code Invalid}).
   *
   * @param mismatch the exception
   * @return the problem
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<Map<String, Object>> mismatch(
      MethodArgumentTypeMismatchException mismatch) {
    if (mismatch.getParameter().hasParameterAnnotation(PathVariable.class)) {
      return notFound();
    }
    return validation(List.of());
  }

  /**
   * 405 for a method the path does not offer, e.g. changing or deleting a Version (EX-010-03).
   *
   * @return the problem
   */
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<Map<String, Object>> methodNotAllowed() {
    return problem(HttpStatus.METHOD_NOT_ALLOWED, "methodNotAllowed");
  }

  /**
   * 403 when the Role does not allow the action (BR-03, BR-04) or the Invitation is for another
   * e-mail (R-004-05), with the code of the exception.
   *
   * @param forbidden the exception
   * @return the problem
   */
  @ExceptionHandler(ForbiddenException.class)
  public ResponseEntity<Map<String, Object>> forbidden(ForbiddenException forbidden) {
    return problem(HttpStatus.FORBIDDEN, forbidden.getCode());
  }

  /**
   * 409 when the change does not fit the current state, with the values its message names (BR-06,
   * R-007-03).
   *
   * @param conflict the exception
   * @return the problem
   */
  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<Map<String, Object>> conflict(ConflictException conflict) {
    Map<String, Object> body = Problems.body(HttpStatus.CONFLICT, conflict.getCode());
    if (!conflict.getParams().isEmpty()) {
      body.put("params", conflict.getParams());
    }
    return respond(HttpStatus.CONFLICT, body);
  }

  /**
   * 400 with one error per invalid field (SB-01).
   *
   * @param invalid the exception
   * @return the problem
   */
  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<Map<String, Object>> invalid(ValidationException invalid) {
    return validation(invalid.problems());
  }

  /**
   * 400 for a body that is not the JSON of the contract.
   *
   * @return the problem
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<Map<String, Object>> unreadable() {
    return validation(List.of());
  }

  /**
   * 500 with a generic code; the cause is logged without request data (SB-07).
   *
   * @param unexpected the exception
   * @return the problem
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> unexpected(Exception unexpected) {
    LOG.error("unexpected error", unexpected);
    return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal");
  }

  private static ResponseEntity<Map<String, Object>> validation(List<FieldProblem> problems) {
    Map<String, Object> body = Problems.body(HttpStatus.BAD_REQUEST, "validation");
    body.put("errors", problems.stream().map(ProblemHandler::error).toList());
    return respond(HttpStatus.BAD_REQUEST, body);
  }

  private static Map<String, Object> error(FieldProblem problem) {
    Map<String, Object> error = new LinkedHashMap<>();
    error.put("field", problem.field());
    error.put("code", problem.code());
    if (!problem.params().isEmpty()) {
      error.put("params", problem.params());
    }
    return error;
  }

  private static ResponseEntity<Map<String, Object>> problem(HttpStatus status, String code) {
    return respond(status, Problems.body(status, code));
  }

  private static ResponseEntity<Map<String, Object>> respond(
      HttpStatus status, Map<String, Object> body) {
    return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
  }
}
