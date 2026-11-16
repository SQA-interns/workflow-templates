package app.contacts.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import app.contacts.acceptance.support.AcceptanceTest;
import app.contacts.acceptance.support.Json;
import app.contacts.acceptance.support.Res;
import app.contacts.acceptance.support.TestUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** US-021 Delete my account: API part. */
class Us021DeleteAccountAcceptanceIT extends AcceptanceTest {

  private Res deleteAccount(TestUser user, String typedEmail) {
    return api.delete(user, "/me", Json.obj("confirmEmail", typedEmail));
  }

  @Test
  @DisplayName("AC-021-01 a deleted account cannot be used; its Versions show Deleted user")
  void ac02101() {
    Team t = team();
    String janez = contact(t.ana(), t.groupId(), "firstName", "Janez", "lastName", "Novak");
    edited(t.marko(), t.groupId(), janez, field("city", "Maribor"));

    Res res = deleteAccount(t.marko(), MARKO);

    assertThat(res.status()).isEqualTo(204);
    assertThat(api.get(t.marko(), "/me").status()).isEqualTo(401);
    assertThat(auth.accountsWithEmail(MARKO)).isZero();
    assertThat(Json.texts(api.get(t.ana(), t.path() + "/members").json(), "email"))
        .containsExactlyInAnyOrder(ANA, PETRA);
    assertThat(Json.text(history(t.ana(), t.groupId(), janez).get(0).path("changedBy")))
        .isEqualTo("user.deleted");
  }

  @Test
  @DisplayName("AC-021-02 an Owner must delete her Groups first; nothing changes")
  void ac02102() {
    Team t = team();
    final String members = state(t.ana(), t.path() + "/members");

    Res res = deleteAccount(t.ana(), ANA);

    assertProblem(res, 409, "account.ownsGroups");
    assertThat(res.body()).contains("Konferenca 2025");
    assertThat(api.get(t.ana(), "/me").status()).isEqualTo(200);
    assertThat(state(t.ana(), t.path() + "/members")).isEqualTo(members);
    assertThat(auth.accountsWithEmail(ANA)).isEqualTo(1);
  }

  @Test
  @DisplayName("EX-021-01 a typed e-mail that does not match deletes nothing")
  void ex02101() {
    Team t = team();

    Res res = deleteAccount(t.marko(), "marko@example.si");

    assertThat(res.status()).isEqualTo(400);
    assertThat(api.get(t.marko(), "/me").status()).isEqualTo(200);
    assertThat(groupNames(t.marko())).containsExactly("Konferenca 2025");
    assertThat(auth.accountsWithEmail(MARKO)).isEqualTo(1);
  }

  @Test
  @DisplayName("EX-021-02 the Owner of a Group in the recycle bin only is rejected, Group named")
  void ex02102() {
    TestUser ana = ana();
    String groupId = group(ana, "Konferenca 2025");
    api.post(ana, "/groups/" + groupId + "/move-to-recycle-bin", null);

    Res res = deleteAccount(ana, ANA);

    assertProblem(res, 409, "account.ownsGroups");
    assertThat(res.body()).contains("Konferenca 2025");
    assertThat(api.get(ana, "/me").status()).isEqualTo(200);
  }

  @Test
  @DisplayName("EX-021-03 a request with the deleted User's old ID token is 401")
  void ex02103() {
    Team t = team();
    deleteAccount(t.petra(), PETRA);

    Res res = api.get(t.petra(), "/groups");

    assertThat(res.status()).isEqualTo(401);
    assertThat(res.body()).doesNotContain("Konferenca 2025");
  }
}
