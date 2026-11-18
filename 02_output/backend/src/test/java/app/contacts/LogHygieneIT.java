package app.contacts;

import static org.assertj.core.api.Assertions.assertThat;

import app.contacts.acceptance.support.AcceptanceTest;
import app.contacts.acceptance.support.Json;
import app.contacts.acceptance.support.Res;
import app.contacts.acceptance.support.TestUser;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * SR-08, SB-07 (ES-07): while the API serves the flows of the stories, including refused and
 * invalid requests, the application log holds no e-mail, display name, Contact Field value, ID
 * token or Invitation token; logs carry User ids and Contact ids only.
 */
@ExtendWith(OutputCaptureExtension.class)
class LogHygieneIT extends AcceptanceTest {

  /** Contact Field values of the stories (US-006, US-014, E2E-04). */
  private static final List<String> FIELD_VALUES =
      List.of(
          "janez.novak@example.si",
          "+386 41 123 456",
          "041 123 456",
          "Slovenska cesta 1",
          "Podjetje d.o.o.",
          "Ljubljana");

  @Test
  void sr08LogsHoldNoPersonalDataOrTokens(CapturedOutput output) {
    Team team = team();
    List<String> secrets = new ArrayList<>(FIELD_VALUES);
    for (TestUser user : List.of(team.ana(), team.marko(), team.petra())) {
      secrets.add(user.email());
      secrets.add(user.displayName());
      secrets.add(user.token());
    }
    secrets.add(invite(team.groupId(), team.ana(), "povabljen@example.si", "VIEWER"));
    secrets.add("povabljen@example.si");

    serveTheStoriesFlows(team);

    String log = output.getAll();
    assertThat(log).as("captured log").isNotEmpty();
    for (String secret : secrets) {
      assertThat(log).as("log holds no personal data or token (SR-08)").doesNotContain(secret);
    }
  }

  /** Accepted, refused and invalid requests across Contacts, History, Export and Merge. */
  private void serveTheStoriesFlows(Team team) {
    String group = team.groupId();
    ObjectNode janezBody =
        contactBody(
            "firstName", "Janez",
            "lastName", "Novak",
            "email", "janez.novak@example.si",
            "phone", "+386 41 123 456",
            "company", "Podjetje d.o.o.",
            "street", "Slovenska cesta 1");
    String janez = contact(team.marko(), group, janezBody);
    final String other =
        contact(
            team.marko(),
            group,
            "firstName",
            "J.",
            "lastName",
            "Novak",
            "email",
            "janez.novak@example.si",
            "phone",
            "041 123 456");
    edited(team.marko(), group, janez, field("city", "Ljubljana"));

    // refused and invalid requests (R-006-04, BR-03, BR-06, SR-01)
    api.post(team.marko(), "/groups/" + group + "/contacts", invalidEmail());
    api.put(
        team.petra(), contactPath(group, janez), updateOf(getContact(team.ana(), group, janez)));
    JsonNode stale = getContact(team.ana(), group, janez);
    edited(team.ana(), group, janez, field("phone", "041 123 456"));
    api.put(team.marko(), contactPath(group, janez), updateOf(stale));
    api.send("GET", "/me", team.ana().token() + "x", null);
    api.get(
        team.petra(), "/groups/" + group + "/contacts/email-matches?email=janez.novak@example.si");

    // reading, search, Export, History, Merge, recycle bin
    list(team.petra(), group, "q=novak");
    api.post(team.petra(), "/groups/" + group + "/exports", Json.obj());
    history(team.ana(), group, janez);
    Res merged =
        merge(
            team.marko(),
            group,
            janez,
            other,
            Json.obj("fieldChoices", Json.obj("firstName", "primary")));
    assertThat(merged.status()).as("merge: %s", merged).isEqualTo(201);
    moveToRecycleBin(team.marko(), group, janez);
  }

  private static ObjectNode invalidEmail() {
    return contactBody("firstName", "Janez", "email", "janez.novak(at)example.si");
  }
}
