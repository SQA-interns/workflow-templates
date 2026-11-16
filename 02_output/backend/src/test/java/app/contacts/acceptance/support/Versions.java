package app.contacts.acceptance.support;

import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;

/** Reading Versions of the History (R-010-01). */
public final class Versions {

  private Versions() {}

  /** "item: old → new" for every change of a Version; an empty value is written "∅". */
  public static List<String> changes(JsonNode version) {
    List<String> result = new ArrayList<>();
    for (JsonNode change : version.path("changes")) {
      result.add(
          Json.text(change.path("item"))
              + ": "
              + orEmpty(Json.text(change.path("oldValue")))
              + " → "
              + orEmpty(Json.text(change.path("newValue"))));
    }
    return result;
  }

  /** The items of the changes of a Version. */
  public static List<String> items(JsonNode version) {
    return Json.texts(version.path("changes"), "item");
  }

  /** "∅" for null or an empty text, else the text. */
  public static String orEmpty(String value) {
    return value == null || value.isEmpty() ? "∅" : value;
  }

  /** "number kind" of every Version, newest first. */
  public static List<String> kinds(JsonNode history) {
    List<String> result = new ArrayList<>();
    for (JsonNode version : history) {
      result.add(version.path("number").asInt() + " " + Json.text(version.path("kind")));
    }
    return result;
  }
}
