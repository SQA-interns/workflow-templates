package app.contacts.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** The Tag rules of {@link ContactValidator} (specification §3, R-009-05, R-009-06). */
class ContactValidatorTagsTest {

  private final ContactValidator validator = new ContactValidator();

  private List<FieldProblem> problemsOf(String... tags) {
    try {
      validator.checkTags(Arrays.asList(tags));
    } catch (ValidationException invalid) {
      return invalid.problems();
    }
    return List.of();
  }

  @Test
  void r00905NamesAreTrimmedAndKeepTheirSpelling() {
    assertThat(validator.checkTags(List.of("  neObvescaj ", "udeležba")))
        .containsExactly("neObvescaj", "udeležba");
  }

  @Test
  void ex00901NameOfFiftyCharactersIsValid() {
    assertThat(validator.checkTags(List.of("t".repeat(50)))).containsExactly("t".repeat(50));
  }

  @ParameterizedTest
  @ValueSource(strings = {"   ", "vip, 2025", ","})
  void r00905BlankOrCommaNameIsInvalid(String name) {
    assertThat(problemsOf("vip", name))
        .containsExactly(new FieldProblem("tags[1]", "tag.invalid", Map.of()));
  }

  @Test
  void ex00901NameOfFiftyOneCharactersIsInvalid() {
    assertThat(problemsOf("t".repeat(51)))
        .containsExactly(new FieldProblem("tags[0]", "tag.invalid", Map.of()));
  }

  @Test
  void r00905MissingNameIsInvalid() {
    assertThat(problemsOf((String) null))
        .containsExactly(new FieldProblem("tags[0]", "tag.invalid", Map.of()));
  }

  @Test
  void r00905EveryInvalidNameIsReported() {
    assertThat(problemsOf("", "ok", "a,b")).hasSize(2);
  }

  @Test
  void ex00903SameNameTwiceInOneSaveIsKeptOnce() {
    assertThat(validator.checkTags(List.of("vip", "VIP ", "neObvescaj", "vip")))
        .containsExactly("vip", "neObvescaj");
  }
}
