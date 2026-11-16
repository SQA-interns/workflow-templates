package app.contacts.service;

import java.util.List;

/** A request breaks validation rules: 400 {@code validation} with one error per field (SB-01). */
public class ValidationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final transient List<FieldProblem> problems;

  /**
   * The exception.
   *
   * @param problems the invalid fields
   */
  public ValidationException(List<FieldProblem> problems) {
    super("validation");
    this.problems = List.copyOf(problems);
  }

  /**
   * The invalid fields.
   *
   * @return the problems
   */
  public List<FieldProblem> problems() {
    return problems;
  }
}
