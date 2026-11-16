package app.contacts.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import app.contacts.acceptance.support.AcceptanceTest;
import app.contacts.acceptance.support.Json;
import app.contacts.acceptance.support.Res;
import app.contacts.acceptance.support.TestUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** US-002 Create and switch Groups: API part. */
class Us002GroupsAcceptanceIT extends AcceptanceTest {

  @Test
  @DisplayName("AC-002-01 the creator of a Group is its Owner and its Contact list is empty")
  void ac00201() {
    TestUser ana = ana();

    Res created = api.post(ana, "/groups", Json.obj("name", "Konferenca 2025"));

    assertThat(created.status()).isEqualTo(201);
    assertThat(Json.text(created.json().path("name"))).isEqualTo("Konferenca 2025");
    assertThat(Json.text(created.json().path("role"))).isEqualTo("OWNER");
    String groupId = Json.text(created.json().path("id"));
    assertThat(groupNames(ana)).containsExactly("Konferenca 2025");
    assertThat(list(ana, groupId, "").path("total").asInt()).isZero();
  }

  @Test
  @DisplayName("AC-002-02 the Contact list of the selected Group shows only its Contacts")
  void ac00202() {
    TestUser ana = ana();
    String konferenca = group(ana, "Konferenca 2025");
    String alfa = group(ana, "Podjetje Alfa");
    contact(ana, konferenca, "firstName", "Janez", "lastName", "Novak");
    contact(ana, alfa, "firstName", "Maja", "lastName", "Zupan");

    assertThat(names(list(ana, alfa, ""))).containsExactly("Maja Zupan");
    assertThat(names(list(ana, konferenca, ""))).containsExactly("Janez Novak");
  }

  @Test
  @DisplayName("AC-002-03 GET /groups lists only the Groups of which the caller is a Member")
  void ac00203() {
    TestUser ana = ana();
    TestUser marko = marko();
    group(ana, "Konferenca 2025");
    group(marko, "Podjetje Beta");

    assertThat(groupNames(marko)).containsExactly("Podjetje Beta");
  }

  @Test
  @DisplayName("AC-002-04 a blank Group name is rejected and nothing is created")
  void ac00204() {
    TestUser ana = ana();

    Res res = api.post(ana, "/groups", Json.obj("name", "   "));

    assertProblem(res, 400, "group.nameLength");
    assertThat(groupNames(ana)).isEmpty();
  }

  @Test
  @DisplayName("EX-002-01 a Group name of exactly 100 characters is accepted")
  void ex00201() {
    TestUser ana = ana();
    String name = "K".repeat(100);

    Res res = api.post(ana, "/groups", Json.obj("name", name));

    assertThat(res.status()).isEqualTo(201);
    assertThat(groupNames(ana)).containsExactly(name);
  }

  @Test
  @DisplayName("EX-002-02 a Group name of 101 characters is rejected and nothing is created")
  void ex00202() {
    TestUser ana = ana();

    Res res = api.post(ana, "/groups", Json.obj("name", "K".repeat(101)));

    assertProblem(res, 400, "group.nameLength");
    assertThat(groupNames(ana)).isEmpty();
  }

  @Test
  @DisplayName("EX-002-03 the Group name is trimmed")
  void ex00203() {
    TestUser ana = ana();

    Res res = api.post(ana, "/groups", Json.obj("name", "  Konferenca 2025  "));

    assertThat(res.status()).isEqualTo(201);
    assertThat(Json.text(res.json().path("name"))).isEqualTo("Konferenca 2025");
    assertThat(groupNames(ana)).containsExactly("Konferenca 2025");
  }

  @Test
  @DisplayName("EX-002-04 a second Group with the same name is created")
  void ex00204() {
    TestUser ana = ana();
    group(ana, "Konferenca 2025");

    Res res = api.post(ana, "/groups", Json.obj("name", "Konferenca 2025"));

    assertThat(res.status()).isEqualTo(201);
    assertThat(groupNames(ana)).containsExactly("Konferenca 2025", "Konferenca 2025");
  }

  @Test
  @DisplayName("EX-002-05 a non-Member gets 404 and no data for another Group")
  void ex00205() {
    TestUser ana = ana();
    TestUser marko = marko();
    String groupId = group(ana, "Konferenca 2025");

    Res res = api.get(marko, "/groups/" + groupId);

    assertThat(res.status()).isEqualTo(404);
    assertThat(res.body()).doesNotContain("Konferenca 2025");
  }

  @Test
  @DisplayName("EX-002-06 a Group moved to the recycle bin is no longer listed")
  void ex00206() {
    TestUser ana = ana();
    String gone = group(ana, "Konferenca 2025");
    group(ana, "Podjetje Alfa");

    Res moved = api.post(ana, "/groups/" + gone + "/move-to-recycle-bin", null);

    assertThat(moved.status()).isEqualTo(204);
    assertThat(groupNames(ana)).containsExactly("Podjetje Alfa");
  }
}
