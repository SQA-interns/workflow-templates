package app.contacts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.contacts.domain.ContactFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MergedContentTest {

  private static final ContactContent JANEZ =
      new ContactContent(
          ContactFields.ofNames(
              Map.of("firstName", "Janez", "lastName", "Novak", "email", "janez.novak@example.si")),
          List.of(),
          List.of("udelezenec2025"));

  private static final ContactContent J_NOVAK =
      new ContactContent(
          ContactFields.ofNames(
              Map.of(
                  "firstName", "J.",
                  "lastName", "Novak",
                  "email", "janez.novak@example.si",
                  "phone", "+386 41 123 456")),
          List.of(),
          List.of("vip"));

  private static ContactContent withAttributes(String firstName, List<AttributeView> attributes) {
    return new ContactContent(
        ContactFields.ofNames(Map.of("firstName", firstName)), attributes, List.of());
  }

  private static List<AttributeView> keys(String prefix, int count) {
    List<AttributeView> attributes = new ArrayList<>();
    for (int i = 1; i <= count; i++) {
      attributes.add(new AttributeView(prefix + i, "v"));
    }
    return attributes;
  }

  @Test
  void r01802PrimaryGetsTheChosenFieldValuesAndAllTagsOfBoth() {
    ContactContent merged =
        MergedContent.of(JANEZ, J_NOVAK, Map.of("phone", "other", "firstName", "primary"), null);

    assertThat(merged.fields().byName())
        .containsEntry("firstName", "Janez")
        .containsEntry("phone", "+386 41 123 456");
    assertThat(merged.tags()).containsExactly("udelezenec2025", "vip");
  }

  @Test
  void r01801WithoutChoiceTheNonBlankPrimaryValueIsKeptElseTheOthers() {
    ContactContent merged = MergedContent.of(JANEZ, J_NOVAK, null, null);

    assertThat(merged.fields().byName())
        .containsEntry("firstName", "Janez")
        .containsEntry("phone", "+386 41 123 456");
  }

  @Test
  void r01802ChoosingTheOthersBlankValueEmptiesTheField() {
    ContactContent merged = MergedContent.of(J_NOVAK, JANEZ, Map.of("phone", "other"), null);

    assertThat(merged.fields().byName()).containsEntry("phone", null);
  }

  @Test
  void ex01807TheSameTagOnBothIsKeptOnceIgnoringCase() {
    ContactContent vip =
        new ContactContent(
            ContactFields.ofNames(Map.of("firstName", "J.")), List.of(), List.of("VIP"));
    ContactContent primary =
        new ContactContent(JANEZ.fields(), List.of(), List.of("vip", "udelezenec2025"));

    assertThat(MergedContent.of(primary, vip, null, null).tags())
        .containsExactly("udelezenec2025", "vip");
  }

  @Test
  void r01802AllAttributesOfBothAreKept() {
    ContactContent merged =
        MergedContent.of(
            withAttributes("Janez", List.of(new AttributeView("kompetenca", "java"))),
            withAttributes("J.", List.of(new AttributeView("podjetje", "Alfa"))),
            null,
            null);

    assertThat(merged.attributes())
        .containsExactly(
            new AttributeView("kompetenca", "java"), new AttributeView("podjetje", "Alfa"));
  }

  @Test
  void ex01801TheSameKeyWithTheSameValueIsKeptOnceWithoutChoice() {
    ContactContent merged =
        MergedContent.of(
            withAttributes("Janez", List.of(new AttributeView("kompetenca", "java"))),
            withAttributes("J.", List.of(new AttributeView("Kompetenca", "java"))),
            null,
            null);

    assertThat(merged.attributes()).containsExactly(new AttributeView("kompetenca", "java"));
  }

  @Test
  void r01803TwoValuesOfOneKeyNeedChoice() {
    ContactContent java = withAttributes("Janez", List.of(new AttributeView("kompetenca", "java")));
    ContactContent python =
        withAttributes("J.", List.of(new AttributeView("kompetenca", "python")));

    assertThatThrownBy(() -> MergedContent.of(java, python, null, Map.of()))
        .isInstanceOfSatisfying(
            InvalidRequestException.class,
            invalid -> {
              assertThat(invalid.getCode()).isEqualTo("merge.choiceRequired");
              assertThat(invalid.getParams()).containsEntry("key", "kompetenca");
            });
  }

  @Test
  void r01803TheChosenValueOfOneKeyIsKept() {
    ContactContent java = withAttributes("Janez", List.of(new AttributeView("kompetenca", "java")));
    ContactContent python =
        withAttributes("J.", List.of(new AttributeView("kompetenca", "python")));

    assertThat(MergedContent.of(java, python, null, Map.of("kompetenca", "other")).attributes())
        .containsExactly(new AttributeView("kompetenca", "python"));
  }

  @Test
  void ex01802FiftyCombinedAttributesAreMergedFiftyOneRejected() {
    assertThat(
            MergedContent.of(
                    withAttributes("Janez", keys("a", 25)),
                    withAttributes("J.", keys("b", 25)),
                    null,
                    null)
                .attributes())
        .hasSize(50);
    ContactContent maja = withAttributes("Maja", keys("c", 25));
    ContactContent other = withAttributes("M.", keys("d", 26));

    assertThatThrownBy(() -> MergedContent.of(maja, other, null, null))
        .isInstanceOfSatisfying(
            ConflictException.class,
            conflict -> assertThat(conflict.getCode()).isEqualTo("merge.tooManyAttributes"));
  }

  @Test
  void mergeRequestAnUnknownFieldOrChoiceIsInvalid() {
    Map<String, String> unknownChoice = new HashMap<>();
    unknownChoice.put("phone", "both");

    assertThatThrownBy(() -> MergedContent.of(JANEZ, J_NOVAK, Map.of("age", "other"), null))
        .isInstanceOf(ValidationException.class);
    assertThatThrownBy(() -> MergedContent.of(JANEZ, J_NOVAK, unknownChoice, null))
        .isInstanceOf(ValidationException.class);
  }
}
