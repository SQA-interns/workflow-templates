package app.contacts.service;

import app.contacts.domain.ContactFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Checks the Fields, Attributes and Tag names of a Contact after trimming them (specification §3,
 * R-006-02 … R-006-07, R-008-02 … R-008-05, R-009-05).
 */
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

  /** Longest Attribute key and value in characters, most Attributes per Contact (Data model). */
  private static final int MAX_KEY_LENGTH = 50;

  private static final int MAX_VALUE_LENGTH = 500;

  private static final int MAX_ATTRIBUTES = 50;

  /** Longest Tag name in characters (Data model, R-009-05). */
  private static final int MAX_TAG_LENGTH = 50;

  /** A missing Attribute of the request, checked as one with a blank key. */
  private static final AttributeView BLANK = new AttributeView("", "");

  /**
   * E-mail syntax: one {@code @}, a local part and a dot in the domain, no spaces (R-006-04); also
   * used for Invitations (EX-004-06).
   */
  static final Predicate<String> EMAIL =
      Pattern.compile("[^@\\s]+@[^@\\s]*\\.[^@\\s]*").asMatchPredicate();

  /**
   * Syntax rules: e-mail with one {@code @}, a local part and a dot in the domain, no spaces
   * (R-006-04); phone of digits, spaces and {@code + - ( ) /} (D-23); ISO 3166-1 alpha-2 country
   * (R-006-05).
   */
  private static final Map<String, Format> FORMATS =
      Map.of(
          "email",
          new Format(EMAIL, "field.emailInvalid"),
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

  /**
   * The Tag names trimmed (R-006-07), each name once compared case-insensitively with the first
   * spelling kept (R-009-06, BR-11), if they are valid.
   *
   * @param sent the Tag names as sent
   * @return the trimmed names in the order sent
   * @throws ValidationException with {@code tag.invalid} on {@code tags[i]} for each name that is
   *     blank, longer than 50 characters or contains a comma (R-009-05)
   */
  public List<String> checkTags(List<String> sent) {
    List<FieldProblem> problems = new ArrayList<>();
    Map<String, String> names = new LinkedHashMap<>();
    for (int i = 0; i < sent.size(); i++) {
      String name = strip(sent.get(i));
      int length = name.codePointCount(0, name.length());
      if (length < 1 || length > MAX_TAG_LENGTH || name.contains(",")) {
        problems.add(new FieldProblem("tags[" + i + "]", "tag.invalid", Map.of()));
      }
      names.putIfAbsent(name.toLowerCase(Locale.ROOT), name);
    }
    if (!problems.isEmpty()) {
      throw new ValidationException(problems);
    }
    return List.copyOf(names.values());
  }

  /**
   * The Attributes with key and value trimmed (R-006-07), a missing value empty (R-008-03), if they
   * are valid.
   *
   * @param sent the Attributes as sent
   * @return the trimmed Attributes in the order sent
   * @throws ValidationException with one problem per invalid key or value ({@code
   *     attributes[i].key}, {@code attributes[i].value}) and {@code attributes} for too many
   *     (R-008-02 … R-008-05)
   */
  public List<AttributeView> checkAttributes(List<AttributeView> sent) {
    List<AttributeView> attributes =
        sent.stream()
            .map(attribute -> attribute == null ? BLANK : attribute)
            .map(attribute -> new AttributeView(strip(attribute.key()), strip(attribute.value())))
            .toList();
    List<FieldProblem> problems = new ArrayList<>();
    if (attributes.size() > MAX_ATTRIBUTES) {
      problems.add(new FieldProblem("attributes", "attribute.tooMany", Map.of()));
    }
    Set<String> keys = new HashSet<>();
    for (int i = 0; i < attributes.size(); i++) {
      attributeProblemOf(i, attributes.get(i), keys, problems);
    }
    if (!problems.isEmpty()) {
      throw new ValidationException(problems);
    }
    return attributes;
  }

  private static void attributeProblemOf(
      int index, AttributeView attribute, Set<String> keys, List<FieldProblem> problems) {
    String key = attribute.key();
    String at = "attributes[" + index + "].";
    int keyLength = key.codePointCount(0, key.length());
    if (keyLength < 1 || keyLength > MAX_KEY_LENGTH) {
      problems.add(new FieldProblem(at + "key", "attribute.keyLength", Map.of()));
    } else if (!keys.add(key.toLowerCase(Locale.ROOT))) {
      problems.add(new FieldProblem(at + "key", "attribute.keyTwice", Map.of("key", key)));
    }
    String value = attribute.value();
    if (value.codePointCount(0, value.length()) > MAX_VALUE_LENGTH) {
      problems.add(new FieldProblem(at + "value", "attribute.valueTooLong", Map.of()));
    }
  }

  private static String strip(String text) {
    return text == null ? "" : text.strip();
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
