package app.contacts;

import static org.assertj.core.api.Assertions.assertThat;

import app.contacts.acceptance.support.AcceptanceTest;
import app.contacts.acceptance.support.Json;
import app.contacts.acceptance.support.Res;
import app.contacts.acceptance.support.TestUser;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * SR-02 (specification §6.1, ADR-04): every Group endpoint of {@code openapi.yaml} answers 404 to a
 * non-Member and 403 to a Role without the permission, and changes nothing.
 */
class AccessMatrixIT extends AcceptanceTest {

  /** One request of the matrix: the contract's operation, method, path and body. */
  private record Call(String operationId, String method, String path, Supplier<JsonNode> body) {}

  /** The Group and its data that every call of the matrix names. */
  private record Fixture(
      Team team,
      String contactId,
      String binnedContactId,
      String invitationId,
      List<String> watchedPaths) {}

  @Test
  void sr02NonMemberGets404OnEveryGroupEndpoint() {
    Fixture fixture = fixture();
    TestUser outsider = user("eva.outsider@example.si", "Eva Outsider");
    group(outsider, "Podjetje Beta");

    assertEveryCall(fixture, outsider, allCalls(fixture), 404, "notFound");
  }

  @Test
  void sr02ViewerGets403WithoutThePermission() {
    Fixture fixture = fixture();

    List<Call> refused = new ArrayList<>(manageGroupCalls(fixture));
    refused.addAll(changeContactCalls(fixture));
    refused.addAll(viewRecycleBinCalls(fixture));
    refused.addAll(permanentlyDeleteCalls(fixture));

    assertEveryCall(fixture, fixture.team().petra(), refused, 403, "auth.forbidden");
  }

  @Test
  void sr02EditorGets403WithoutThePermission() {
    Fixture fixture = fixture();

    List<Call> refused = new ArrayList<>(manageGroupCalls(fixture));
    refused.addAll(permanentlyDeleteCalls(fixture));

    assertEveryCall(fixture, fixture.team().marko(), refused, 403, "auth.forbidden");
  }

  // ---------------------------------------------------------------- Calls

  private List<Call> allCalls(Fixture fixture) {
    List<Call> calls = new ArrayList<>(readCalls(fixture));
    calls.addAll(manageGroupCalls(fixture));
    calls.addAll(changeContactCalls(fixture));
    calls.addAll(viewRecycleBinCalls(fixture));
    calls.addAll(permanentlyDeleteCalls(fixture));
    return calls;
  }

  /** READ and EXPORT: every Role may (ADR-04). */
  private List<Call> readCalls(Fixture fixture) {
    String group = fixture.team().path();
    String contact = contactPath(fixture.team().groupId(), fixture.contactId());
    return List.of(
        get("getGroup", group),
        get("listContacts", group + "/contacts"),
        get("findContactsByEmail", group + "/contacts/email-matches?email=" + JANEZ_EMAIL),
        get("getContact", contact),
        get("getHistory", contact + "/versions"),
        get("getVersion", contact + "/versions/1"),
        get("suggestTags", group + "/tags?q=vip"),
        new Call("exportContacts", "POST", group + "/exports", Json::obj));
  }

  /** MANAGE_GROUP: Owner only (BR-04). */
  private List<Call> manageGroupCalls(Fixture fixture) {
    String group = fixture.team().path();
    String petraId = idOf(fixture.team().petra());
    return List.of(
        new Call("renameGroup", "PATCH", group, () -> Json.obj("name", "Konferenca 2026")),
        new Call("moveGroupToRecycleBin", "POST", group + "/move-to-recycle-bin", () -> null),
        get("listMembers", group + "/members"),
        new Call(
            "changeRole", "PATCH", group + "/members/" + petraId, () -> Json.obj("role", "EDITOR")),
        new Call("removeMember", "DELETE", group + "/members/" + petraId, () -> null),
        get("listOpenInvitations", group + "/invitations"),
        new Call(
            "createInvitation",
            "POST",
            group + "/invitations",
            () -> Json.obj("email", "nova.clanica@example.si", "role", "VIEWER")),
        new Call(
            "revokeInvitation",
            "DELETE",
            group + "/invitations/" + fixture.invitationId(),
            () -> null));
  }

  /** CHANGE_CONTACT: Owner and Editor (BR-03). */
  private List<Call> changeContactCalls(Fixture fixture) {
    String group = fixture.team().path();
    String contact = contactPath(fixture.team().groupId(), fixture.contactId());
    TestUser owner = fixture.team().ana();
    JsonNode current = getContact(owner, fixture.team().groupId(), fixture.contactId());
    JsonNode binned = getContact(owner, fixture.team().groupId(), fixture.binnedContactId());
    return List.of(
        new Call(
            "createContact",
            "POST",
            group + "/contacts",
            () -> contactBody("firstName", "Maja", "lastName", "Zupan")),
        new Call("updateContact", "PUT", contact, () -> updateOf(current)),
        new Call(
            "moveContactToRecycleBin",
            "POST",
            contact + "/move-to-recycle-bin",
            () -> Json.obj("versionNumber", current.path("versionNumber").asInt())),
        new Call(
            "restoreVersion",
            "POST",
            contact + "/versions/1/restore",
            () -> Json.obj("versionNumber", current.path("versionNumber").asInt())),
        new Call(
            "restoreContactFromRecycleBin",
            "POST",
            group + "/recycle-bin/" + fixture.binnedContactId() + "/restore",
            () -> Json.obj("versionNumber", binned.path("versionNumber").asInt())));
  }

  /** VIEW_RECYCLE_BIN: Owner and Editor (ADR-04). */
  private static List<Call> viewRecycleBinCalls(Fixture fixture) {
    return List.of(get("listRecycleBin", fixture.team().path() + "/recycle-bin"));
  }

  /** PERMANENTLY_DELETE: Owner only (BR-04, R-013-01). */
  private static List<Call> permanentlyDeleteCalls(Fixture fixture) {
    return List.of(
        new Call(
            "permanentlyDeleteContact",
            "DELETE",
            fixture.team().path() + "/recycle-bin/" + fixture.binnedContactId(),
            () -> null));
  }

  private static Call get(String operationId, String path) {
    return new Call(operationId, "GET", path, () -> null);
  }

  // -------------------------------------------------------------- Fixture

  private static final String JANEZ_EMAIL = "janez.novak@example.si";

  /**
   * "Konferenca 2025" (Ana Owner, Marko Editor, Petra Viewer) with "Janez Novak" (an edit, so two
   * Versions), "Maja Zupan" in the recycle bin and an open Invitation.
   */
  private Fixture fixture() {
    Team team = team();
    String groupId = team.groupId();
    String janez = contact(team.ana(), groupId, "firstName", "Janez", "lastName", "Novak");
    edited(team.ana(), groupId, janez, field("email", JANEZ_EMAIL));
    String maja = contact(team.ana(), groupId, "firstName", "Maja", "lastName", "Zupan");
    moveToRecycleBin(team.ana(), groupId, maja);
    invite(groupId, team.ana(), "povabljen@example.si", "VIEWER");
    Res invitations = api.get(team.ana(), team.path() + "/invitations");
    String invitationId = Json.text(invitations.json().path(0).path("id"));
    List<String> watched =
        List.of(
            team.path(),
            team.path() + "/members",
            team.path() + "/invitations",
            team.path() + "/contacts",
            team.path() + "/recycle-bin",
            contactPath(groupId, janez),
            contactPath(groupId, janez) + "/versions");
    return new Fixture(team, janez, maja, invitationId, watched);
  }

  // ----------------------------------------------------------- Assertions

  /** Every call answers the status and code, and the Owner sees the Group's data unchanged. */
  private void assertEveryCall(
      Fixture fixture, TestUser caller, List<Call> calls, int status, String code) {
    TestUser owner = fixture.team().ana();
    List<String> before = fixture.watchedPaths().stream().map(p -> state(owner, p)).toList();
    SoftAssertions softly = new SoftAssertions();
    for (Call call : calls) {
      Res res = api.send(call.method(), call.path(), caller.token(), call.body().get());
      softly.assertThat(res.status()).as("%s %s", call.operationId(), res).isEqualTo(status);
      softly.assertThat(res.code()).as("%s code", call.operationId()).isEqualTo(code);
    }
    softly.assertAll();
    List<String> after = fixture.watchedPaths().stream().map(p -> state(owner, p)).toList();
    assertThat(after).as("the Group's data after refused calls (SR-02)").isEqualTo(before);
  }
}
