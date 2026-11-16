package app.contacts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.contacts.domain.ContactFields;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ContactValidatorTest {

  private final ContactValidator validator = new ContactValidator();

  /** Fields from name/value pairs; the others are empty. */
  private static ContactFields fieldsOf(String... pairs) {
    Map<String, String> values = new HashMap<>();
    for (int i = 0; i < pairs.length; i += 2) {
      values.put(pairs[i], pairs[i + 1]);
    }
    return new ContactFields(
        values.get("firstName"),
        values.get("lastName"),
        values.get("email"),
        values.get("phone"),
        values.get("company"),
        values.get("jobTitle"),
        values.get("street"),
        values.get("postalCode"),
        values.get("city"),
        values.get("country"),
        values.get("note"));
  }

  private static ContactFields janezWith(String field, String value) {
    return field.equals("firstName")
        ? fieldsOf("firstName", value)
        : fieldsOf("firstName", "Janez", field, value);
  }

  private List<FieldProblem> problemsOf(ContactFields fields) {
    try {
      validator.check(fields);
    } catch (ValidationException invalid) {
      return invalid.problems();
    }
    return List.of();
  }

  @Test
  void r00601AllFieldsOfAc00601AreAccepted() {
    ContactFields all =
        new ContactFields(
            "Janez",
            "Novak",
            "janez.novak@example.si",
            "+386 41 123 456",
            "Podjetje d.o.o.",
            "Direktor",
            "Slovenska cesta 1",
            "1000",
            "Ljubljana",
            "SI",
            "Govornik");

    assertThat(validator.check(all)).isEqualTo(all);
  }

  @Test
  void r00602OnlyCompanyIsEnough() {
    ContactFields company = fieldsOf("company", "Podjetje d.o.o.");

    assertThat(validator.check(company).company()).isEqualTo("Podjetje d.o.o.");
  }

  @Test
  void r00602NoNamePartIsRejected() {
    ContactFields onlyEmail = fieldsOf("email", "info@example.si");

    assertThat(problemsOf(onlyEmail))
        .containsExactly(new FieldProblem("name", "contact.nameRequired", Map.of()));
  }

  @Test
  void ex00605NamePartsOfOnlySpacesAreRejected() {
    ContactFields blank = fieldsOf("firstName", "   ", "lastName", "", "company", "  ");

    assertThat(problemsOf(blank))
        .containsExactly(new FieldProblem("name", "contact.nameRequired", Map.of()));
  }

  @Test
  void r00607EveryTextIsTrimmedAndBlankBecomesEmpty() {
    ContactFields spaced = fieldsOf("firstName", "  Janez ", "lastName", " ", "city", "Ljubljana ");

    ContactFields checked = validator.check(spaced);

    assertThat(checked.firstName()).isEqualTo("Janez");
    assertThat(checked.city()).isEqualTo("Ljubljana");
    assertThat(checked.lastName()).isNull();
  }

  @ParameterizedTest
  @CsvSource({
    "firstName, 100",
    "lastName, 100",
    "email, 254",
    "phone, 30",
    "company, 200",
    "jobTitle, 100",
    "street, 200",
    "postalCode, 20",
    "city, 100",
    "note, 2000"
  })
  void r00603EachFieldAcceptsItsMaximumLength(String field, int max) {
    assertThat(problemsOf(janezWith(field, valueOfLength(field, max)))).isEmpty();
  }

  @ParameterizedTest
  @CsvSource({
    "firstName, 100",
    "lastName, 100",
    "email, 254",
    "phone, 30",
    "company, 200",
    "jobTitle, 100",
    "street, 200",
    "postalCode, 20",
    "city, 100",
    "note, 2000"
  })
  void r00603EachFieldRejectsOneCharacterMore(String field, int max) {
    assertThat(problemsOf(janezWith(field, valueOfLength(field, max + 1))))
        .containsExactly(
            new FieldProblem(field, "field.tooLong", Map.of("field", field, "max", max)));
  }

  @Test
  void r00603LengthCountsCharactersNotUtf16Units() {
    String hundredEmoji = "😀".repeat(100);

    assertThat(problemsOf(fieldsOf("firstName", hundredEmoji))).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "janez@",
        "janez",
        "@example.si",
        "janez@example",
        "ja nez@example.si",
        "janez@@example.si",
        "janez@novak@example.si"
      })
  void r00604InvalidEmailIsRejected(String email) {
    assertThat(problemsOf(janezWith("email", email)))
        .containsExactly(new FieldProblem("email", "field.emailInvalid", Map.of()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"janez.novak@example.si", "Janez.Novak@example.si", "info@a.b.si"})
  void r00604ValidEmailIsAccepted(String email) {
    assertThat(problemsOf(janezWith("email", email))).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"XX", "si", "SVN", "S"})
  void r00605UnknownCountryIsRejected(String country) {
    assertThat(problemsOf(janezWith("country", country)))
        .containsExactly(new FieldProblem("country", "field.countryInvalid", Map.of()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"SI", "AT", "DE"})
  void r00605IsoCountryCodeIsAccepted(String country) {
    assertThat(problemsOf(janezWith("country", country))).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"041 123 456", "+386 (1) 123-45-67", "01/123 45 67"})
  void d23PhoneOfDigitsSpacesAndSignsIsAccepted(String phone) {
    assertThat(problemsOf(janezWith("phone", phone))).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"041 123 456 ext. 2", "041.123.456", "+386 41 123 456#"})
  void d23PhoneWithOtherCharactersIsRejected(String phone) {
    assertThat(problemsOf(janezWith("phone", phone)))
        .containsExactly(new FieldProblem("phone", "field.phoneInvalid", Map.of()));
  }

  @Test
  void r00603EveryInvalidFieldIsReportedAtOnce() {
    ContactFields twoErrors = fieldsOf("firstName", "J".repeat(101), "email", "janez@");

    assertThatThrownBy(() -> validator.check(twoErrors))
        .isInstanceOfSatisfying(
            ValidationException.class,
            invalid ->
                assertThat(invalid.problems())
                    .extracting(FieldProblem::field)
                    .containsExactly("firstName", "email"));
  }

  private static String valueOfLength(String field, int length) {
    if ("email".equals(field)) {
      return "j".repeat(length - "@example.si".length()) + "@example.si";
    }
    if ("phone".equals(field)) {
      return "1".repeat(length);
    }
    return "a".repeat(length);
  }
}
