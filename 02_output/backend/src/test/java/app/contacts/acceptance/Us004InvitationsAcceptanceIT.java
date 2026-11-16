package app.contacts.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import app.contacts.acceptance.support.AcceptanceTest;
import app.contacts.acceptance.support.Json;
import app.contacts.acceptance.support.Res;
import app.contacts.acceptance.support.TestUser;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** US-004 Invite Members: API part. */
class Us004InvitationsAcceptanceIT extends AcceptanceTest {

  private TestUser ana;
  private String groupId;

  private void anaOwnsKonferenca() {
    ana = ana();
    groupId = group(ana, "Konferenca 2025");
  }

  private String invitations() {
    return "/groups/" + groupId + "/invitations";
  }

  private Res inviteRes(TestUser owner, String email, String role) {
    return api.post(owner, invitations(), Json.obj("email", email, "role", role));
  }

  @Test
  @DisplayName("AC-004-01 Owner invites an Editor: link once, open list with expiry +7 days")
  void ac00401() {
    anaOwnsKonferenca();
    final List<Map<String, Object>> before = auditEntries(groupId);
    final Instant now = clock.setLjubljana("2026-03-05T10:00");

    Res res = inviteRes(ana, MARKO, "EDITOR");

    assertThat(res.status()).isEqualTo(201);
    assertThat(Json.text(res.json().path("link"))).matches(".*/invitations/[A-Za-z0-9_-]{22,128}");
    JsonNode open = api.get(ana, invitations()).json();
    assertThat(open.size()).isEqualTo(1);
    assertThat(Json.text(open.get(0).path("email"))).isEqualTo(MARKO);
    assertThat(Json.text(open.get(0).path("role"))).isEqualTo("EDITOR");
    assertThat(Instant.parse(Json.text(open.get(0).path("expiresAt"))))
        .isEqualTo(Instant.parse("2026-03-12T09:00:00Z"));
    assertThat(open.get(0).has("link")).isFalse();
    assertAudited(groupId, ana, now, before);
  }

  @Test
  @DisplayName("AC-004-02 the invitee accepts and becomes Editor; the Invitation is no longer open")
  void ac00402() {
    anaOwnsKonferenca();
    TestUser marko = marko();
    String token = invite(groupId, ana, MARKO, "EDITOR");

    assertThat(api.get(marko, "/invitations/" + token).status()).isEqualTo(200);
    Res res = api.post(marko, "/invitations/" + token + "/accept", null);

    assertThat(res.status()).isEqualTo(200);
    JsonNode groups = api.get(marko, "/groups").json();
    assertThat(Json.texts(groups, "name")).containsExactly("Konferenca 2025");
    assertThat(Json.text(groups.get(0).path("role"))).isEqualTo("EDITOR");
    assertThat(api.get(ana, invitations()).json().size()).isZero();
  }

  @Test
  @DisplayName("AC-004-03 a User with another e-mail cannot use the Invitation")
  void ac00403() {
    anaOwnsKonferenca();
    TestUser petra = petra();
    String token = invite(groupId, ana, MARKO, "EDITOR");

    Res view = api.get(petra, "/invitations/" + token);
    Res accept = api.post(petra, "/invitations/" + token + "/accept", null);

    assertProblem(view, 403, "invitation.otherEmail");
    assertProblem(accept, 403, "invitation.otherEmail");
    assertThat(groupNames(petra)).isEmpty();
    assertThat(api.get(ana, invitations()).json().size()).isEqualTo(1);
  }

  @Test
  @DisplayName("AC-004-04 an accepted Invitation is no longer valid")
  void ac00404() {
    anaOwnsKonferenca();
    TestUser marko = marko();
    String token = invite(groupId, ana, MARKO, "EDITOR");
    api.post(marko, "/invitations/" + token + "/accept", null);

    assertProblem(api.get(marko, "/invitations/" + token), 409, "invitation.noLongerValid");
    assertProblem(
        api.post(marko, "/invitations/" + token + "/accept", null),
        409,
        "invitation.noLongerValid");
  }

  @Test
  @DisplayName("AC-004-05 an Editor cannot create an Invitation (403, none exists)")
  void ac00405() {
    anaOwnsKonferenca();
    TestUser marko = marko();
    join(groupId, ana, marko, "EDITOR");

    Res res = inviteRes(marko, PETRA, "VIEWER");

    assertProblem(res, 403, "auth.forbidden");
    assertThat(api.get(ana, invitations()).json().size()).isZero();
  }

  @Test
  @DisplayName("AC-004-06 a revoked Invitation is no longer valid; the revocation is audited")
  void ac00406() {
    anaOwnsKonferenca();
    TestUser petra = petra();
    Res created = inviteRes(ana, PETRA, "VIEWER");
    String token = tokenOf(created);
    String id = Json.text(created.json().path("invitation").path("id"));
    final List<Map<String, Object>> before = auditEntries(groupId);
    final Instant now = clock.setLjubljana("2026-03-06T09:00");

    Res revoked = api.delete(ana, invitations() + "/" + id, null);

    assertThat(revoked.status()).isEqualTo(204);
    assertThat(api.get(ana, invitations()).json().size()).isZero();
    assertProblem(api.get(petra, "/invitations/" + token), 409, "invitation.noLongerValid");
    assertProblem(
        api.post(petra, "/invitations/" + token + "/accept", null),
        409,
        "invitation.noLongerValid");
    assertThat(groupNames(petra)).isEmpty();
    assertAudited(groupId, ana, now, before);
  }

  @Test
  @DisplayName("EX-004-01 accepted one minute before it expires")
  void ex00401() {
    anaOwnsKonferenca();
    TestUser marko = marko();
    final Instant created = clock.setLjubljana("2026-03-05T10:00");
    String token = invite(groupId, ana, MARKO, "EDITOR");
    clock.set(created.plus(Duration.ofDays(7)).minus(Duration.ofMinutes(1)));

    Res res = api.post(marko, "/invitations/" + token + "/accept", null);

    assertThat(res.status()).isEqualTo(200);
    assertThat(groupNames(marko)).containsExactly("Konferenca 2025");
  }

  @Test
  @DisplayName("EX-004-02 opened at expiresAt the Invitation is no longer valid")
  void ex00402() {
    anaOwnsKonferenca();
    TestUser marko = marko();
    final Instant created = clock.setLjubljana("2026-03-05T10:00");
    String token = invite(groupId, ana, MARKO, "EDITOR");
    clock.set(created.plus(Duration.ofDays(7)));

    assertProblem(api.get(marko, "/invitations/" + token), 409, "invitation.noLongerValid");
    assertProblem(
        api.post(marko, "/invitations/" + token + "/accept", null),
        409,
        "invitation.noLongerValid");
    assertThat(groupNames(marko)).isEmpty();
  }

  @Test
  @DisplayName("EX-004-03 the invitee's e-mail matches case-insensitively")
  void ex00403() {
    anaOwnsKonferenca();
    TestUser marko = marko();
    String token = invite(groupId, ana, "Marko.Kranjc@Gmail.com", "EDITOR");

    Res res = api.post(marko, "/invitations/" + token + "/accept", null);

    assertThat(res.status()).isEqualTo(200);
    assertThat(groupNames(marko)).containsExactly("Konferenca 2025");
  }

  @Test
  @DisplayName("EX-004-04 inviting an existing Member is rejected, nothing created")
  void ex00404() {
    anaOwnsKonferenca();

    Res res = inviteRes(ana, ANA, "EDITOR");

    assertProblem(res, 409, "invitation.alreadyMember");
    assertThat(api.get(ana, invitations()).json().size()).isZero();
  }

  @Test
  @DisplayName("EX-004-05 a second Invitation for the same e-mail revokes the first")
  void ex00405() {
    anaOwnsKonferenca();
    TestUser petra = petra();
    String first = invite(groupId, ana, PETRA, "VIEWER");
    String second = invite(groupId, ana, PETRA, "EDITOR");

    assertThat(api.get(ana, invitations()).json().size()).isEqualTo(1);
    assertProblem(
        api.post(petra, "/invitations/" + first + "/accept", null),
        409,
        "invitation.noLongerValid");
    Res res = api.post(petra, "/invitations/" + second + "/accept", null);
    assertThat(res.status()).isEqualTo(200);
    assertThat(Json.text(api.get(petra, "/groups").json().get(0).path("role"))).isEqualTo("EDITOR");
  }

  @Test
  @DisplayName("EX-004-06 an invalid e-mail is rejected, nothing created")
  void ex00406() {
    anaOwnsKonferenca();

    Res res = inviteRes(ana, "marko@", "EDITOR");

    assertProblem(res, 400, "field.emailInvalid");
    assertThat(api.get(ana, invitations()).json().size()).isZero();
  }

  @Test
  @DisplayName("EX-004-07 the Role Owner cannot be invited (400, nothing created)")
  void ex00407() {
    anaOwnsKonferenca();

    Res res = inviteRes(ana, MARKO, "OWNER");

    assertThat(res.status()).isEqualTo(400);
    assertThat(api.get(ana, invitations()).json().size()).isZero();
  }

  @Test
  @DisplayName("EX-004-08 a token that does not exist is no longer valid")
  void ex00408() {
    anaOwnsKonferenca();
    TestUser marko = marko();

    Res res = api.get(marko, "/invitations/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");

    assertProblem(res, 409, "invitation.noLongerValid");
  }
}
