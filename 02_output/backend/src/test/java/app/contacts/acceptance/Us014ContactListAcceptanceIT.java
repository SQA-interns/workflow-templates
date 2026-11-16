package app.contacts.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import app.contacts.acceptance.support.AcceptanceTest;
import app.contacts.acceptance.support.TestUser;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** US-014 Contact list and search: API part. */
class Us014ContactListAcceptanceIT extends AcceptanceTest {

  /** Janez Novak (Podjetje d.o.o.), Maja Zupan, Žiga Čuk, and Ana Bizjak in the recycle bin. */
  private void fourContacts(Team t) {
    contact(
        t.ana(),
        t.groupId(),
        "firstName",
        "Janez",
        "lastName",
        "Novak",
        "company",
        "Podjetje d.o.o.",
        "email",
        "janez.novak@example.si");
    contact(t.ana(), t.groupId(), "firstName", "Maja", "lastName", "Zupan");
    contact(t.ana(), t.groupId(), "firstName", "Žiga", "lastName", "Čuk");
    String bizjak = contact(t.ana(), t.groupId(), "firstName", "Ana", "lastName", "Bizjak");
    moveToRecycleBin(t.ana(), t.groupId(), bizjak);
  }

  private void numbered(Team t, int count) {
    numbered(t, 1, count);
  }

  private void numbered(Team t, int from, int to) {
    for (int i = from; i <= to; i++) {
      contact(
          t.ana(), t.groupId(), "firstName", "Oseba", "lastName", String.format("Priimek%03d", i));
    }
  }

  private static List<String> numberedNames(int from, int to) {
    List<String> names = new ArrayList<>();
    for (int i = from; i <= to; i++) {
      names.add(String.format("Oseba Priimek%03d", i));
    }
    return names;
  }

  @Test
  @DisplayName("AC-014-01 the list shows active Contacts by last name in Slovenian order")
  void ac01401() {
    Team t = team();
    fourContacts(t);

    JsonNode page = list(t.petra(), t.groupId(), "");

    assertThat(page.path("total").asInt()).isEqualTo(3);
    assertThat(names(page)).containsExactly("Žiga Čuk", "Janez Novak", "Maja Zupan");
  }

  @Test
  @DisplayName("AC-014-02 search without diacritics finds a name with diacritics")
  void ac01402() {
    Team t = team();
    fourContacts(t);

    assertThat(names(list(t.petra(), t.groupId(), "q=cuk"))).containsExactly("Žiga Čuk");
  }

  @Test
  @DisplayName("AC-014-03 search ignores case and covers the company")
  void ac01403() {
    Team t = team();
    fourContacts(t);

    assertThat(names(list(t.petra(), t.groupId(), "q=PODJETJE"))).containsExactly("Janez Novak");
  }

  @Test
  @DisplayName("AC-014-04 a search without matches gives an empty list")
  void ac01404() {
    Team t = team();
    fourContacts(t);

    JsonNode page = list(t.petra(), t.groupId(), "q=xyz");

    assertThat(page.path("total").asInt()).isZero();
    assertThat(page.path("items").size()).isZero();
  }

  @Test
  @DisplayName("AC-014-05 page 3 of 120 Contacts shows Contacts 101 to 120")
  void ac01405() {
    Team t = team();
    numbered(t, 120);

    JsonNode page = list(t.petra(), t.groupId(), "page=3");

    assertThat(page.path("total").asInt()).isEqualTo(120);
    assertThat(page.path("pageSize").asInt()).isEqualTo(50);
    assertThat(names(page)).containsExactlyElementsOf(numberedNames(101, 120));
  }

  @Test
  @DisplayName("EX-014-01 50 Contacts fill one Page, 51 Contacts two Pages")
  void ex01401() {
    Team t = team();
    numbered(t, 50);

    assertThat(list(t.petra(), t.groupId(), "page=1").path("items").size()).isEqualTo(50);
    assertThat(list(t.petra(), t.groupId(), "page=2").path("items").size()).isZero();
    numbered(t, 51, 51);
    JsonNode second = list(t.petra(), t.groupId(), "page=2");
    assertThat(second.path("total").asInt()).isEqualTo(51);
    assertThat(names(second)).containsExactly("Oseba Priimek051");
  }

  @Test
  @DisplayName("EX-014-02 an empty Group has an empty list")
  void ex01402() {
    Team t = team();

    JsonNode page = list(t.petra(), t.groupId(), "");

    assertThat(page.path("total").asInt()).isZero();
    assertThat(page.path("items").size()).isZero();
  }

  @Test
  @DisplayName("EX-014-03 search text is trimmed")
  void ex01403() {
    Team t = team();
    fourContacts(t);

    assertThat(names(list(t.petra(), t.groupId(), "q=%20%20novak%20")))
        .containsExactly("Janez Novak");
  }

  @Test
  @DisplayName("EX-014-04 a Contact with only a company sorts by company in the name position")
  void ex01404() {
    Team t = team();
    fourContacts(t);
    contact(t.ana(), t.groupId(), "company", "Alfa d.o.o.");

    assertThat(names(list(t.petra(), t.groupId(), "")))
        .containsExactly("Alfa d.o.o.", "Žiga Čuk", "Janez Novak", "Maja Zupan");
  }

  @Test
  @DisplayName("EX-014-05 % and _ in the search text match literally")
  void ex01405() {
    Team t = team();
    contact(t.ana(), t.groupId(), "company", "Bio 100% d.o.o.");
    contact(t.ana(), t.groupId(), "company", "Bio 100 d.o.o.");
    contact(t.ana(), t.groupId(), "firstName", "Ana", "email", "ana_b@example.si");
    contact(t.ana(), t.groupId(), "firstName", "Anab", "email", "anab@example.si");

    assertThat(names(list(t.petra(), t.groupId(), "q=100%25"))).containsExactly("Bio 100% d.o.o.");
    assertThat(names(list(t.petra(), t.groupId(), "q=a_b"))).containsExactly("Ana");
  }

  @Test
  @DisplayName("EX-014-06 a Page beyond the last is empty with the correct total")
  void ex01406() {
    Team t = team();
    fourContacts(t);

    JsonNode page = list(t.petra(), t.groupId(), "page=9");

    assertThat(page.path("items").size()).isZero();
    assertThat(page.path("total").asInt()).isEqualTo(3);
  }

  @Test
  @DisplayName("EX-014-07 a Contact of another Group matching the search is never shown")
  void ex01407() {
    Team t = team();
    fourContacts(t);
    TestUser nina = user("nina.kos@example.si", "Nina Kos");
    String beta = group(nina, "Podjetje Beta");
    contact(nina, beta, "firstName", "Peter", "lastName", "Novak");

    assertThat(names(list(t.petra(), t.groupId(), "q=novak"))).containsExactly("Janez Novak");
  }
}
