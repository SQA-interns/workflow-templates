package app.contacts.hidden;

import static org.assertj.core.api.Assertions.assertThat;

import app.contacts.hidden.support.HiddenTest;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** Hidden cases of US-003, US-004, US-005 and BR-04, BR-10. */
class HiddenGroupsIT extends HiddenTest {

  private Person owner;
  private Person editor;
  private Person viewer;
  private String g;

  private void sejem() {
    owner = person("luka.zorman@example.org", "Luka Zorman");
    editor = person("tina.bergant@example.org", "Tina Bergant");
    viewer = person("rok.mlakar@example.org", "Rok Mlakar");
    g = group(owner, "Sejem Celje");
    join(g, owner, editor, "EDITOR");
    join(g, owner, viewer, "VIEWER");
  }

  private String gp() {
    return "/groups/" + g;
  }

  @Test
  @DisplayName("R-003-01 boundary rename to 100 characters works, to spaces is rejected")
  void r003_01_boundary() {
    sejem();

    assertThat(patch(owner, gp(), obj("name", "S".repeat(100))).status()).isEqualTo(200);
    problem(patch(owner, gp(), obj("name", "   ")), 400, "group.nameLength");
    assertThat(groups(viewer)).containsExactly("S".repeat(100));
  }

  @Test
  @DisplayName("R-003-02 other moved in summer: deletedAt is the moment of the move")
  void r003_02_other() {
    sejem();
    Instant now = clock.at("2026-07-20T16:45");

    post(owner, gp() + "/move-to-recycle-bin", null);

    JsonNode d = get(owner, "/deleted-groups").json().get(0);
    assertThat(Instant.parse(d.path("deletedAt").asString())).isEqualTo(now);
  }

  @Test
  @DisplayName("R-003-03 role in the bin the Group is 404 even for its Owner's Contact requests")
  void r003_03_role() {
    sejem();
    String c = contact(owner, g, "firstName", "Eva");
    post(owner, gp() + "/move-to-recycle-bin", null);

    assertThat(get(owner, gp() + "/contacts").status()).isEqualTo(404);
    assertThat(get(owner, path(g, c)).status()).isEqualTo(404);
    assertThat(post(owner, gp() + "/contacts", body("firstName", "Tim")).status()).isEqualTo(404);
    assertThat(get(viewer, gp() + "/contacts").status()).isEqualTo(404);
  }

  @Test
  @DisplayName("R-003-04 other only the Owner's Deleted Groups list it, removal 30 days later")
  void r003_04_other() {
    sejem();
    Instant moved = clock.at("2026-10-10T12:00");

    post(owner, gp() + "/move-to-recycle-bin", null);

    JsonNode d = get(owner, "/deleted-groups").json().get(0);
    assertThat(d.path("name").asString()).isEqualTo("Sejem Celje");
    assertThat(Instant.parse(d.path("removalAt").asString()))
        .isEqualTo(moved.plus(Duration.ofDays(30)));
    assertThat(get(editor, "/deleted-groups").json().size()).isZero();
  }

  @Test
  @DisplayName("R-003-05 combination restore keeps open Invitations and Tags")
  void r003_05_combination() {
    sejem();
    String c = contact(owner, g, body("firstName", "Eva"));
    save(owner, g, c, f -> f.set("tags", arr("razstavljavec")));
    invite(g, owner, "nina.kos@example.org", "VIEWER");
    clock.at("2026-05-01T08:00");
    post(owner, gp() + "/move-to-recycle-bin", null);
    clock.at("2026-05-20T08:00");

    assertThat(post(owner, "/deleted-groups/" + g + "/restore", null).status()).isEqualTo(200);

    assertThat(texts(get(owner, gp() + "/invitations").json(), "email"))
        .containsExactly("nina.kos@example.org");
    assertThat(texts(get(viewer, gp() + "/tags?q=razstav").json(), "name"))
        .containsExactly("razstavljavec");
  }

  @Test
  @DisplayName("R-003-06 boundary one second after the removal date it cannot be restored")
  void r003_06_boundary() {
    sejem();
    Instant moved = clock.at("2026-05-01T08:00");
    post(owner, gp() + "/move-to-recycle-bin", null);
    clock.at(moved.plus(Duration.ofDays(30)).plusSeconds(1));

    problem(post(owner, "/deleted-groups/" + g + "/restore", null), 409, "group.noLongerRestorable");
    assertThat(groups(owner)).isEmpty();
  }

  @Test
  @DisplayName("R-004-01 other a Viewer Invitation in summer expires exactly 7 days later")
  void r004_01_other() {
    sejem();
    Instant now = clock.at("2026-08-01T23:30");

    var a = post(owner, gp() + "/invitations", obj("email", "nina.kos@example.org", "role", "VIEWER"));

    assertThat(a.status()).isEqualTo(201);
    JsonNode inv = a.json().path("invitation");
    assertThat(inv.path("role").asString()).isEqualTo("VIEWER");
    assertThat(Instant.parse(inv.path("expiresAt").asString())).isEqualTo(now.plus(Duration.ofDays(7)));
  }

  @Test
  @DisplayName("R-004-02 other an Editor's e-mail in other case is already a Member")
  void r004_02_other() {
    sejem();

    var a = post(owner, gp() + "/invitations", obj("email", "TINA.Bergant@Example.org", "role", "VIEWER"));

    problem(a, 409, "invitation.alreadyMember");
    assertThat(get(owner, gp() + "/invitations").json().size()).isZero();
  }

  @Test
  @DisplayName("R-004-03 combination a new Invitation for the same e-mail in other case revokes")
  void r004_03_combination() {
    sejem();
    Person nina = person("nina.kos@example.org", "Nina Kos");
    String first = invite(g, owner, "nina.kos@example.org", "VIEWER");
    String second = invite(g, owner, "Nina.Kos@Example.org", "EDITOR");

    problem(get(nina, "/invitations/" + first), 409, "invitation.noLongerValid");
    assertThat(post(nina, "/invitations/" + second + "/accept", null).status()).isEqualTo(200);
    assertThat(get(nina, "/groups").json().get(0).path("role").asString()).isEqualTo("EDITOR");
  }

  @Test
  @DisplayName("R-004-04 role an accepted Viewer Invitation gives read-only rights")
  void r004_04_role() {
    sejem();
    Person nina = person("nina.kos@example.org", "Nina Kos");
    join(g, owner, nina, "VIEWER");

    assertThat(get(nina, gp() + "/contacts").status()).isEqualTo(200);
    assertThat(post(nina, gp() + "/contacts", body("firstName", "X")).status()).isEqualTo(403);
  }

  @Test
  @DisplayName("R-004-05 invalid a forwarded link creates no Membership and stays open")
  void r004_05_invalid() {
    sejem();
    String t = invite(g, owner, "nina.kos@example.org", "VIEWER");

    problem(post(editor, "/invitations/" + t + "/accept", null), 403, "invitation.otherEmail");
    assertThat(get(owner, gp() + "/invitations").json().size()).isEqualTo(1);
    Person nina = person("nina.kos@example.org", "Nina Kos");
    assertThat(post(nina, "/invitations/" + t + "/accept", null).status()).isEqualTo(200);
  }

  @Test
  @DisplayName("R-004-06 boundary one second after expiresAt the link is no longer valid")
  void r004_06_boundary() {
    sejem();
    Person nina = person("nina.kos@example.org", "Nina Kos");
    Instant created = clock.at("2026-08-01T09:15");
    String t = invite(g, owner, "nina.kos@example.org", "VIEWER");
    clock.at(created.plus(Duration.ofDays(7)).plusSeconds(1));

    problem(post(nina, "/invitations/" + t + "/accept", null), 409, "invitation.noLongerValid");
    assertThat(groups(nina)).isEmpty();
  }

  @Test
  @DisplayName("R-004-07 invalid an Invitation link without sign-in is 401")
  void r004_07_invalid() {
    sejem();
    String t = invite(g, owner, "nina.kos@example.org", "VIEWER");

    var a = call("GET", "/invitations/" + t, null, null);

    assertThat(a.status()).isEqualTo(401);
    assertThat(a.body()).doesNotContain("Sejem Celje");
  }

  @Test
  @DisplayName("R-004-08 role only the Owner lists the open Invitations")
  void r004_08_role() {
    sejem();
    invite(g, owner, "nina.kos@example.org", "VIEWER");
    invite(g, owner, "eva.golob@example.org", "EDITOR");

    assertThat(get(editor, gp() + "/invitations").status()).isEqualTo(403);
    JsonNode open = get(owner, gp() + "/invitations").json();
    assertThat(texts(open, "email"))
        .containsExactlyInAnyOrder("nina.kos@example.org", "eva.golob@example.org");
    assertThat(texts(open, "role")).containsExactlyInAnyOrder("VIEWER", "EDITOR");
  }

  @Test
  @DisplayName("R-004-09 other revoking one Invitation keeps the other open")
  void r004_09_other() {
    sejem();
    var first =
        post(owner, gp() + "/invitations", obj("email", "nina.kos@example.org", "role", "VIEWER"));
    invite(g, owner, "eva.golob@example.org", "EDITOR");

    delete(owner, gp() + "/invitations/" + first.json().path("invitation").path("id").asString(), null);

    assertThat(texts(get(owner, gp() + "/invitations").json(), "email"))
        .containsExactly("eva.golob@example.org");
  }

  @Test
  @DisplayName("R-005-01 role an Editor cannot read the Member list")
  void r005_01_role() {
    sejem();

    var a = get(editor, gp() + "/members");

    assertThat(a.status()).isEqualTo(403);
    assertThat(a.body()).doesNotContain("rok.mlakar@example.org");
  }

  @Test
  @DisplayName("R-005-02 other a Viewer made Editor may add Contacts at once")
  void r005_02_other() {
    sejem();

    patch(owner, gp() + "/members/" + id(viewer), obj("role", "EDITOR"));

    assertThat(post(viewer, gp() + "/contacts", body("firstName", "Eva")).status()).isEqualTo(201);
  }

  @Test
  @DisplayName("R-005-03 other a removed Viewer loses read access")
  void r005_03_other() {
    sejem();
    contact(owner, g, "firstName", "Eva");

    assertThat(delete(owner, gp() + "/members/" + id(viewer), null).status()).isEqualTo(204);

    assertThat(get(viewer, gp() + "/contacts").status()).isEqualTo(404);
    assertThat(texts(get(owner, gp() + "/members").json(), "email"))
        .doesNotContain("rok.mlakar@example.org");
  }

  @Test
  @DisplayName("R-005-04 other Version 1 by a removed Editor keeps his name")
  void r005_04_other() {
    sejem();
    String c = contact(editor, g, "firstName", "Eva");

    delete(owner, gp() + "/members/" + id(editor), null);

    assertThat(history(owner, g, c).get(0).path("changedBy").asString()).isEqualTo("Tina Bergant");
  }

  @Test
  @DisplayName("R-005-05 invalid the Owner cannot make herself Viewer")
  void r005_05_invalid() {
    sejem();

    problem(patch(owner, gp() + "/members/" + id(owner), obj("role", "VIEWER")), 409, "member.ownerRequired");
    assertThat(texts(get(owner, gp() + "/members").json(), "role")).contains("OWNER");
  }

  @Test
  @DisplayName("BR-04 role an Editor cannot rename, move, revoke, change Roles or remove Members")
  void br04_role() {
    sejem();
    var inv = post(owner, gp() + "/invitations", obj("email", "nina.kos@example.org", "role", "VIEWER"));
    String invId = inv.json().path("invitation").path("id").asString();
    String viewerId = id(viewer);

    assertThat(patch(editor, gp(), obj("name", "Drugo")).status()).isEqualTo(403);
    assertThat(post(editor, gp() + "/move-to-recycle-bin", null).status()).isEqualTo(403);
    assertThat(delete(editor, gp() + "/invitations/" + invId, null).status()).isEqualTo(403);
    assertThat(patch(editor, gp() + "/members/" + viewerId, obj("role", "EDITOR")).status())
        .isEqualTo(403);
    assertThat(delete(editor, gp() + "/members/" + viewerId, null).status()).isEqualTo(403);
    assertThat(groups(viewer)).containsExactly("Sejem Celje");
    assertThat(get(owner, gp() + "/invitations").json().size()).isEqualTo(1);
  }

  @Test
  @DisplayName("BR-10 other Role change, removal, acceptance and automatic revocation are audited")
  void br10_other() {
    sejem();
    Person nina = person("nina.kos@example.org", "Nina Kos");
    List<Map<String, Object>> before = audit(g);
    invite(g, owner, "nina.kos@example.org", "VIEWER");
    String second = invite(g, owner, "nina.kos@example.org", "EDITOR");
    post(nina, "/invitations/" + second + "/accept", null);
    patch(owner, gp() + "/members/" + id(viewer), obj("role", "EDITOR"));
    delete(owner, gp() + "/members/" + id(editor), null);

    List<Map<String, Object>> added = audit(g);
    added.removeAll(before);
    assertThat(added.size()).isGreaterThanOrEqualTo(6);
    assertThat(added.stream().map(e -> e.get("user_id"))).contains(id(owner), id(nina));
  }
}
