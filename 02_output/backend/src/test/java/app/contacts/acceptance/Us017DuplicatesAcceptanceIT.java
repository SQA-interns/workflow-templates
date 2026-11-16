package app.contacts.acceptance;

import static app.contacts.acceptance.support.Versions.kinds;
import static org.assertj.core.api.Assertions.assertThat;

import app.contacts.acceptance.support.AcceptanceTest;
import app.contacts.acceptance.support.Json;
import app.contacts.acceptance.support.Res;
import app.contacts.acceptance.support.TestUser;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** US-017 Find Possible duplicates: API part. */
class Us017DuplicatesAcceptanceIT extends AcceptanceTest {

  /** "id1 + id2: reason, reason" per pair, ids and reasons sorted. */
  private List<String> pairs(TestUser user, String groupId) {
    Res res = api.get(user, "/groups/" + groupId + "/duplicates");
    assertThat(res.status()).as("duplicates: %s", res).isEqualTo(200);
    List<String> pairs = new ArrayList<>();
    for (JsonNode pair : res.json()) {
      List<String> ids =
          Stream.of(
                  Json.text(pair.path("first").path("id")),
                  Json.text(pair.path("second").path("id")))
              .sorted()
              .toList();
      List<String> reasons = new ArrayList<>();
      pair.path("reasons").forEach(r -> reasons.add(r.asString()));
      reasons.sort(null);
      pairs.add(ids.get(0) + " + " + ids.get(1) + ": " + String.join(", ", reasons));
    }
    return pairs;
  }

  private static String pair(String a, String b, String reasons) {
    return a.compareTo(b) < 0 ? a + " + " + b + ": " + reasons : b + " + " + a + ": " + reasons;
  }

  @Test
  @DisplayName("AC-017-01 two Contacts with the same e-mail (other case) are a pair: Same e-mail")
  void ac01701() {
    Team t = team();
    String a =
        contact(
            t.ana(),
            t.groupId(),
            "firstName",
            "Janez",
            "lastName",
            "Novak",
            "email",
            "janez.novak@example.si");
    String b =
        contact(
            t.ana(),
            t.groupId(),
            "firstName",
            "J.",
            "lastName",
            "Novak",
            "email",
            "Janez.Novak@example.si");

    assertThat(pairs(t.marko(), t.groupId())).containsExactly(pair(a, b, "sameEmail"));
  }

  @Test
  @DisplayName("AC-017-02 phones equal after normalising are a pair: Same phone")
  void ac01702() {
    Team t = team();
    String a =
        contact(
            t.ana(),
            t.groupId(),
            "firstName",
            "Janez",
            "lastName",
            "Novak",
            "phone",
            "+386 41 123 456");
    String b =
        contact(
            t.ana(),
            t.groupId(),
            "firstName",
            "Janez",
            "lastName",
            "N.",
            "phone",
            "+386-41-123-456");

    assertThat(pairs(t.marko(), t.groupId())).containsExactly(pair(a, b, "samePhone"));
  }

  @Test
  @DisplayName("AC-017-03 similar names with the same company are a pair")
  void ac01703() {
    Team t = team();
    String a =
        contact(
            t.ana(),
            t.groupId(),
            "firstName",
            "Janez",
            "lastName",
            "Novak",
            "company",
            "Podjetje d.o.o.");
    String b =
        contact(
            t.ana(),
            t.groupId(),
            "firstName",
            "Janez",
            "lastName",
            "Novack",
            "company",
            "Podjetje d.o.o.");

    assertThat(pairs(t.marko(), t.groupId())).containsExactly(pair(a, b, "similarNameSameCompany"));
  }

  @Test
  @DisplayName("AC-017-04 the same name with different companies is not a pair")
  void ac01704() {
    Team t = team();
    contact(
        t.ana(),
        t.groupId(),
        "firstName",
        "Janez",
        "lastName",
        "Novak",
        "company",
        "Podjetje d.o.o.");
    contact(
        t.ana(), t.groupId(), "firstName", "Janez", "lastName", "Novak", "company", "Alfa d.o.o.");

    assertThat(pairs(t.marko(), t.groupId())).isEmpty();
  }

  @Test
  @DisplayName("AC-017-05 opening Possible duplicates changes no Contact")
  void ac01705() {
    Team t = team();
    final String a = contact(t.ana(), t.groupId(), "firstName", "Janez", "email", "j@n.si");
    final String b = contact(t.ana(), t.groupId(), "firstName", "J.", "email", "j@n.si");

    assertThat(pairs(t.marko(), t.groupId())).hasSize(1);
    assertThat(pairs(t.marko(), t.groupId())).hasSize(1);

    assertThat(kinds(history(t.ana(), t.groupId(), a))).containsExactly("1 created");
    assertThat(kinds(history(t.ana(), t.groupId(), b))).containsExactly("1 created");
  }

  @Test
  @DisplayName("EX-017-01 two blank e-mails do not make a pair")
  void ex01701() {
    Team t = team();
    contact(t.ana(), t.groupId(), "firstName", "Janez", "lastName", "Novak", "email", "");
    contact(t.ana(), t.groupId(), "firstName", "Maja", "lastName", "Zupan");

    assertThat(pairs(t.marko(), t.groupId())).isEmpty();
  }

  @Test
  @DisplayName("EX-017-02 phones with 5 digits do not make a pair")
  void ex01702() {
    Team t = team();
    contact(t.ana(), t.groupId(), "firstName", "Janez", "lastName", "Novak", "phone", "12345");
    contact(t.ana(), t.groupId(), "firstName", "Maja", "lastName", "Zupan", "phone", "12345");

    assertThat(pairs(t.marko(), t.groupId())).isEmpty();
  }

  @Test
  @DisplayName("EX-017-03 same e-mail and same phone give one pair with both reasons")
  void ex01703() {
    Team t = team();
    String a =
        contact(
            t.ana(), t.groupId(), "firstName", "Janez", "email", "j@n.si", "phone", "041 123 456");
    String b =
        contact(t.ana(), t.groupId(), "firstName", "Maja", "email", "J@N.si", "phone", "041123456");

    assertThat(pairs(t.marko(), t.groupId())).containsExactly(pair(a, b, "sameEmail, samePhone"));
  }

  @Test
  @DisplayName("EX-017-04 a Contact in the recycle bin or merged is not in a pair")
  void ex01704() {
    Team t = team();
    String a = contact(t.ana(), t.groupId(), "firstName", "Janez", "email", "j@n.si");
    String b = contact(t.ana(), t.groupId(), "firstName", "Janko", "email", "j@n.si");
    String c = contact(t.ana(), t.groupId(), "firstName", "Jan", "email", "j@n.si");
    String d = contact(t.ana(), t.groupId(), "firstName", "Jani", "email", "j@n.si");
    moveToRecycleBin(t.ana(), t.groupId(), d);
    assertThat(merge(t.marko(), t.groupId(), a, b, null).status()).isEqualTo(201);

    assertThat(pairs(t.marko(), t.groupId())).containsExactly(pair(a, c, "sameEmail"));
  }

  @Test
  @DisplayName("EX-017-05 the same e-mail in two Groups is not a pair")
  void ex01705() {
    Team t = team();
    contact(t.ana(), t.groupId(), "firstName", "Janez", "email", "j@n.si");
    String other = group(t.ana(), "Podjetje Alfa");
    contact(t.ana(), other, "firstName", "Janez", "email", "j@n.si");

    assertThat(pairs(t.ana(), t.groupId())).isEmpty();
    assertThat(pairs(t.ana(), other)).isEmpty();
  }

  @Test
  @DisplayName("EX-017-06 Žiga Čuk and Ziga Cuk with the same company are a pair")
  void ex01706() {
    Team t = team();
    String a =
        contact(
            t.ana(), t.groupId(), "firstName", "Žiga", "lastName", "Čuk", "company", "Alfa d.o.o.");
    String b =
        contact(
            t.ana(), t.groupId(), "firstName", "Ziga", "lastName", "Cuk", "company", "alfa d.o.o.");

    assertThat(pairs(t.marko(), t.groupId())).containsExactly(pair(a, b, "similarNameSameCompany"));
  }

  @Test
  @DisplayName("EX-017-07 without pairs the list is empty")
  void ex01707() {
    Team t = team();
    contact(t.ana(), t.groupId(), "firstName", "Janez", "lastName", "Novak");

    assertThat(pairs(t.petra(), t.groupId())).isEmpty();
  }
}
