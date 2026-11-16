package app.contacts.service;

import app.contacts.domain.ContactFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Checks the Fields of a Contact after trimming them (specification §3, R-006-02 … R-006-07). */
@Component
public class ContactValidator {

  /** Longest value of each text Field in characters (business-rules.md, Data model). */
  static final Map<String, Integer> MAX_LENGTH =
      Map.of(
          "firstName", 100,
          "lastName", 100,
          "email", 254,
          "phone", 30,
          "company", 200,
          "jobTitle", 100,
          "street", 200,
          "postalCode", 20,
          "city", 100,
          "note", 2000);

  /**
   * Syntax rules: e-mail with one {@code @}, a local part and a dot in the domain, no spaces
   * (R-006-04); phone of digits, spaces and {@code + - ( ) /} (D-23); ISO 3166-1 alpha-2 country
   * (R-006-05).
   */
  private static final Map<String, Format> FORMATS =
      Map.of(
          "email",
          new Format(
              Pattern.compile("[^@\\s]+@[^@\\s]*\\.[^@\\s]*").asMatchPredicate(),
              "field.emailInvalid"),
          "phone",
          new Format(Pattern.compile("[0-9 +\\-()/]*").asMatchPredicate(), "field.phoneInvalid"),
          "country",
          new Format(Set.of(Locale.getISOCountries())::contains, "field.countryInvalid"));

  /**
   * The Fields trimmed (R-006-07), blank ones empty, if they are valid.
   *
   * @param sent the Fields as sent
   * @return the trimmed Fields
   * @throws ValidationException with one problem per invalid Field, and {@code name} when every
   *     name part is empty (R-006-02)
   */
  public ContactFields check(ContactFields sent) {
    ContactFields fields = trimmed(sent);
    List<FieldProblem> problems = new ArrayList<>();
    if (fields.firstName() == null && fields.lastName() == null && fields.company() == null) {
      problems.add(new FieldProblem("name", "contact.nameRequired", Map.of()));
    }
    fields.byName().forEach((field, value) -> problemOf(field, value, problems));
    if (!problems.isEmpty()) {
      throw new ValidationException(problems);
    }
    return fields;
  }

  private static ContactFields trimmed(ContactFields sent) {
    Map<String, String> values = new HashMap<>();
    sent.byName()
        .forEach(
            (field, value) -> {
              String stripped = value == null ? "" : value.strip();
              values.put(field, stripped.isEmpty() ? null : stripped);
            });
    return ContactFields.ofNames(values);
  }

  private static void problemOf(String field, String value, List<FieldProblem> problems) {
    if (value == null) {
      return;
    }
    Integer max = MAX_LENGTH.get(field);
    if (max != null && value.codePointCount(0, value.length()) > max) {
      problems.add(new FieldProblem(field, "field.tooLong", Map.of("field", field, "max", max)));
      return;
    }
    Format format = FORMATS.get(field);
    if (format != null && !format.valid().test(value)) {
      problems.add(new FieldProblem(field, format.code(), Map.of()));
    }
  }

  /** A syntax rule of a Field and the code that names its breach. */
  private record Format(Predicate<String> valid, String code) {}
}
