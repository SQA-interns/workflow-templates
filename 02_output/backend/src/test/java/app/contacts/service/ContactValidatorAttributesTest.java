package app.contacts.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** The Attribute rules of {@link ContactValidator} (specification §3, R-008-02 … R-008-05). */
class ContactValidatorAttributesTest {

  private final ContactValidator validator = new ContactValidator();

  private static List<AttributeView> attributesOf(String... keyValues) {
    List<AttributeView> attributes = new ArrayList<>();
    for (int i = 0; i < keyValues.length; i += 2) {
      attributes.add(new AttributeView(keyValues[i], keyValues[i + 1]));
    }
    return attributes;
  }

  private static List<AttributeView> numbered(int count) {
    List<AttributeView> attributes = new ArrayList<>();
    for (int i = 1; i <= count; i++) {
      attributes.add(new AttributeView("kljuc" + i, "v"));
    }
    return attributes;
  }

  private List<FieldProblem> problemsOf(List<AttributeView> attributes) {
    try {
      validator.checkAttributes(attributes);
    } catch (ValidationException invalid) {
      return invalid.problems();
    }
    return List.of();
  }

  @Test
  void r00801KeysAndValuesAreTrimmedAndKeepTheirSpelling() {
    List<AttributeView> checked =
        validator.checkAttributes(attributesOf("  Kompetenca ", " spletnoProgramiranje  "));

    assertThat(checked).containsExactly(new AttributeView("Kompetenca", "spletnoProgramiranje"));
  }

  @Test
  void ex00801KeyOf50CharactersIsAcceptedOf51Rejected() {
    assertThat(problemsOf(attributesOf("k".repeat(50), "v"))).isEmpty();
    assertThat(problemsOf(attributesOf("k".repeat(51), "v")))
        .containsExactly(new FieldProblem("attributes[0].key", "attribute.keyLength", Map.of()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   "})
  void ex00802BlankKeyIsRejected(String key) {
    assertThat(problemsOf(attributesOf(key, "v")))
        .containsExactly(new FieldProblem("attributes[0].key", "attribute.keyLength", Map.of()));
  }

  @Test
  void r00802MissingKeyIsRejected() {
    assertThat(problemsOf(attributesOf(null, "v")))
        .extracting(FieldProblem::code)
        .containsExactly("attribute.keyLength");
  }

  @Test
  void r00802MissingAttributeIsABlankKey() {
    List<AttributeView> sent = new ArrayList<>();
    sent.add(null);

    assertThat(problemsOf(sent))
        .containsExactly(new FieldProblem("attributes[0].key", "attribute.keyLength", Map.of()));
  }

  @Test
  void ex00803ValueOf500CharactersIsAcceptedOf501Rejected() {
    assertThat(problemsOf(attributesOf("opis", "v".repeat(500)))).isEmpty();
    assertThat(problemsOf(attributesOf("opis", "v".repeat(501))))
        .containsExactly(
            new FieldProblem("attributes[0].value", "attribute.valueTooLong", Map.of()));
  }

  @Test
  void ex00804EmptyOrMissingValueIsAnEmptyValue() {
    assertThat(validator.checkAttributes(attributesOf("kompetenca", "", "opis", null)))
        .containsExactly(new AttributeView("kompetenca", ""), new AttributeView("opis", ""));
  }

  @Test
  void ac00803SameKeyIgnoringCaseAndSpacesIsRejectedWithTheSecondKey() {
    assertThat(problemsOf(attributesOf("kompetenca", "mobilno", " Kompetenca ", "java")))
        .containsExactly(
            new FieldProblem(
                "attributes[1].key", "attribute.keyTwice", Map.of("key", "Kompetenca")));
  }

  @Test
  void ex00805FiftyAttributesAreAcceptedFiftyOneRejected() {
    assertThat(problemsOf(numbered(50))).isEmpty();
    assertThat(problemsOf(numbered(51)))
        .containsExactly(new FieldProblem("attributes", "attribute.tooMany", Map.of()));
  }

  @Test
  void r00802EveryInvalidAttributeIsReportedAtOnce() {
    assertThat(problemsOf(attributesOf(" ", "v", "opis", "v".repeat(501))))
        .extracting(FieldProblem::field)
        .containsExactly("attributes[0].key", "attributes[1].value");
  }
}
