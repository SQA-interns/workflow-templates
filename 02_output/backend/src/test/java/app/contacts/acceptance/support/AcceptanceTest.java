package app.contacts.acceptance.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** Helpers of the acceptance tests that use only the public API (people, Groups, Contacts). */
public abstract class AcceptanceTest extends StackTest {

  /** Display names and e-mails of the stories' people. */
  public static final String ANA = "ana.kovac@example.si";

  public static final String MARKO = "marko.kranjc@gmail.com";
  public static final String PETRA = "petra.horvat@example.si";

  // ---------------------------------------------------------------- Users

  /** A verified e-mail/password User who has called the API once (so the User exists). */
  protected TestUser user(String email, String displayName) {
    TestUser user = auth.verifiedUser(email, displayName);
    Res me = api.get(user, "/me");
    assertThat(me.status()).as("GET /me of %s: %s", email, me).isEqualTo(200);
    return user;
  }

  /** Ana Kovač, a verified User. */
  protected TestUser ana() {
    return user(ANA, "Ana Kovač");
  }

  /** Marko Kranjc, a verified User. */
  protected TestUser marko() {
    return user(MARKO, "Marko Kranjc");
  }

  /** Petra Horvat, a verified User. */
  protected TestUser petra() {
    return user(PETRA, "Petra Horvat");
  }

  // --------------------------------------------------------------- Groups

  /** Creates a Group as the User and returns its id. */
  protected String group(TestUser owner, String name) {
    Res res = api.post(owner, "/groups", Json.obj("name", name));
    assertThat(res.status()).as("create Group %s: %s", name, res).isEqualTo(201);
    return Json.text(res.json().path("id"));
  }

  /** Makes the User a Member with the Role through an Invitation (R-004-01, R-004-04). */
  protected void join(String groupId, TestUser owner, TestUser member, String role) {
    String token = invite(groupId, owner, member.email(), role);
    Res accepted = api.post(member, "/invitations/" + token + "/accept", null);
    assertThat(accepted.status()).as("accept Invitation: %s", accepted).isEqualTo(200);
  }

  /** Creates an Invitation and returns the token of its link. */
  protected String invite(String groupId, TestUser owner, String email, String role) {
    Res res =
        api.post(
            owner, "/groups/" + groupId + "/invitations", Json.obj("email", email, "role", role));
    assertThat(res.status()).as("create Invitation: %s", res).isEqualTo(201);
    return tokenOf(res);
  }

  /** The token at the end of the Invitation link of a 201 answer. */
  protected static String tokenOf(Res created) {
    String link = Json.text(created.json().path("link"));
    return link.substring(link.lastIndexOf('/') + 1);
  }

  /** The Group names of {@code GET /groups}. */
  protected List<String> groupNames(TestUser user) {
    Res res = api.get(user, "/groups");
    assertThat(res.status()).as("GET /groups: %s", res).isEqualTo(200);
    return Json.texts(res.json(), "name");
  }

  /** "Konferenca 2025" with Ana Owner, Marko Editor, Petra Viewer (AC-005-01). */
  protected Team team() {
    TestUser ana = ana();
    TestUser marko = marko();
    TestUser petra = petra();
    String groupId = group(ana, "Konferenca 2025");
    join(groupId, ana, marko, "EDITOR");
    join(groupId, ana, petra, "VIEWER");
    return new Team(ana, marko, petra, groupId);
  }

  /**
   * Ana (Owner), Marko (Editor), Petra (Viewer) and their Group.
   *
   * @param ana Owner
   * @param marko Editor
   * @param petra Viewer
   * @param groupId the Group "Konferenca 2025"
   */
  public record Team(TestUser ana, TestUser marko, TestUser petra, String groupId) {
    /** The path of the Group, e.g. {@code /groups/<id>}. */
    public String path() {
      return "/groups/" + groupId;
    }
  }

  // ------------------------------------------------------------- Contacts

  /** A ContactWrite body with the given Fields (name-value pairs), no Attributes, no Tags. */
  protected static ObjectNode contactBody(String... fieldPairs) {
    Object[] pairs = fieldPairs;
    return Json.obj(
        "fields", Json.obj(pairs), "attributes", Json.attributes(), "tags", Json.strings());
  }

  /** Creates a Contact with the given Fields and returns its id. */
  protected String contact(TestUser user, String groupId, String... fieldPairs) {
    return contact(user, groupId, contactBody(fieldPairs));
  }

  /** Creates a Contact from a ContactWrite body and returns its id. */
  protected String contact(TestUser user, String groupId, ObjectNode body) {
    Res res = api.post(user, "/groups/" + groupId + "/contacts", body);
    assertThat(res.status()).as("create Contact: %s", res).isEqualTo(201);
    return Json.text(res.json().path("id"));
  }

  /** {@code GET} of one Contact (asserts 200). */
  protected JsonNode getContact(TestUser user, String groupId, String contactId) {
    Res res = api.get(user, contactPath(groupId, contactId));
    assertThat(res.status()).as("GET Contact: %s", res).isEqualTo(200);
    return res.json();
  }

  /** The path of a Contact, e.g. {@code /groups/<g>/contacts/<c>}. */
  protected static String contactPath(String groupId, String contactId) {
    return "/groups/" + groupId + "/contacts/" + contactId;
  }

  /** The ContactUpdate a form would send for the Contact as it is now. */
  protected static ObjectNode updateOf(JsonNode contact) {
    ArrayNode attributes = Json.MAPPER.createArrayNode();
    for (JsonNode attribute : contact.path("attributes")) {
      attributes.add(
          Json.obj(
              "key",
              Json.text(attribute.path("key")),
              "value",
              Json.text(attribute.path("value"))));
    }
    ArrayNode tags = Json.MAPPER.createArrayNode();
    for (JsonNode tag : contact.path("tags")) {
      tags.add(Json.text(tag.path("name")));
    }
    return Json.obj(
        "fields",
        contact.path("fields").deepCopy(),
        "attributes",
        attributes,
        "tags",
        tags,
        "versionNumber",
        contact.path("versionNumber").asInt());
  }

  /** Reads the Contact, lets the editor change the update body, and saves it with PUT. */
  protected Res edit(TestUser user, String groupId, String contactId, Consumer<ObjectNode> editor) {
    ObjectNode update = updateOf(getContact(user, groupId, contactId));
    editor.accept(update);
    return api.put(user, contactPath(groupId, contactId), update);
  }

  /** Like {@link #edit} and asserts 200. */
  protected JsonNode edited(
      TestUser user, String groupId, String contactId, Consumer<ObjectNode> editor) {
    Res res = edit(user, groupId, contactId, editor);
    assertThat(res.status()).as("save Contact: %s", res).isEqualTo(200);
    return res.json();
  }

  /** Sets one Field in an update body. */
  protected static Consumer<ObjectNode> field(String name, String value) {
    return update -> ((ObjectNode) update.path("fields")).put(name, value);
  }

  /** The History (Versions newest first) of a Contact (asserts 200). */
  protected JsonNode history(TestUser user, String groupId, String contactId) {
    Res res = api.get(user, contactPath(groupId, contactId) + "/versions");
    assertThat(res.status()).as("GET History: %s", res).isEqualTo(200);
    return res.json();
  }

  /** Moves an active Contact to the recycle bin (asserts 200). */
  protected void moveToRecycleBin(TestUser user, String groupId, String contactId) {
    int version = getContact(user, groupId, contactId).path("versionNumber").asInt();
    Res res =
        api.post(
            user,
            contactPath(groupId, contactId) + "/move-to-recycle-bin",
            Json.obj("versionNumber", version));
    assertThat(res.status()).as("move to recycle bin: %s", res).isEqualTo(200);
  }

  /** One Page of the Contact list with query parameters, e.g. {@code "q=novak&page=2"}. */
  protected JsonNode list(TestUser user, String groupId, String query) {
    Res res =
        api.get(user, "/groups/" + groupId + "/contacts" + (query.isEmpty() ? "" : "?" + query));
    assertThat(res.status()).as("GET Contact list: %s", res).isEqualTo(200);
    return res.json();
  }

  /** "first last" (or the company) of every row of a Contact list Page. */
  protected static List<String> names(JsonNode page) {
    List<String> names = new ArrayList<>();
    for (JsonNode item : page.path("items")) {
      String first = Json.text(item.path("firstName"));
      String last = Json.text(item.path("lastName"));
      String name = String.join(" ", nonBlank(first), nonBlank(last)).trim();
      names.add(name.isEmpty() ? Json.text(item.path("company")) : name);
    }
    return names;
  }

  private static String nonBlank(String value) {
    return value == null ? "" : value;
  }

  /**
   * Merges the Other contact into the Primary contact with their current Version numbers; the
   * choices body may hold {@code fieldChoices} and {@code attributeChoices} (or be null).
   */
  protected Res merge(
      TestUser user, String groupId, String primaryId, String otherId, ObjectNode choices) {
    ObjectNode request =
        Json.obj(
            "primaryId",
            primaryId,
            "otherId",
            otherId,
            "primaryVersionNumber",
            getContact(user, groupId, primaryId).path("versionNumber").asInt(),
            "otherVersionNumber",
            getContact(user, groupId, otherId).path("versionNumber").asInt());
    if (choices != null) {
      request.setAll(choices);
    }
    return api.post(user, "/groups/" + groupId + "/merges", request);
  }

  /** The Tag id of a Tag name in the Group, through the suggestions (R-009-02). */
  protected String tagId(TestUser user, String groupId, String name) {
    Res res = api.get(user, "/groups/" + groupId + "/tags?q=" + name);
    assertThat(res.status()).as("GET Tags: %s", res).isEqualTo(200);
    for (JsonNode tag : res.json()) {
      if (name.equalsIgnoreCase(Json.text(tag.path("name")))) {
        return Json.text(tag.path("id"));
      }
    }
    throw new AssertionError("no Tag " + name + " in " + res);
  }
}
