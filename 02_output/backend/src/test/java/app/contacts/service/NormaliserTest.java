package app.contacts.service;

import static org.assertj.core.api.Assertions.assertThat;

import app.contacts.domain.ContactFields;
import org.junit.jupiter.api.Test;

class NormaliserTest {

  private final Normaliser normaliser = new Normaliser();

  private static final ContactFields ZIGA =
      new ContactFields(
          "Žiga",
          "Čuk",
          "Ziga.Cuk@Example.si",
          "+386 41 123 456",
          "Đurić Šport d.o.o.",
          "Direktor",
          "Slovenska cesta 1",
          "1000",
          "Ljubljana",
          "SI",
          "Govornik");

  @Test
  void r01404NormalisedTextIsLowerCaseWithoutSlovenianDiacritics() {
    assertThat(normaliser.text("ŽIGA Čuk Ćirić Đurić Šport"))
        .isEqualTo("ziga cuk ciric duric sport");
  }

  @Test
  void r01404SearchTextHoldsTheSearchedFieldsEachOnItsOwnLine() {
    assertThat(normaliser.searchText(ZIGA))
        .isEqualTo(
            "ziga\ncuk\nziga.cuk@example.si\n+386 41 123 456\nduric sport d.o.o.\nljubljana");
  }

  @Test
  void r01404SearchTextLeavesOutEmptyFields() {
    ContactFields company =
        new ContactFields(null, null, null, null, "Podjetje", null, null, null, null, null, null);

    assertThat(normaliser.searchText(company)).isEqualTo("podjetje");
  }

  @Test
  void r01402SortNameIsTheLastName() {
    assertThat(normaliser.sortName(ZIGA)).isEqualTo("Čuk");
  }

  @Test
  void r01402SortNameWithoutLastNameIsTheFirstName() {
    ContactFields janez =
        new ContactFields(
            "Janez", null, null, null, "Podjetje", null, null, null, null, null, null);

    assertThat(normaliser.sortName(janez)).isEqualTo("Janez");
  }

  @Test
  void ex01404SortNameOfOnlyCompanyIsTheCompany() {
    ContactFields company =
        new ContactFields(null, null, null, null, "Podjetje", null, null, null, null, null, null);

    assertThat(normaliser.sortName(company)).isEqualTo("Podjetje");
  }
}
