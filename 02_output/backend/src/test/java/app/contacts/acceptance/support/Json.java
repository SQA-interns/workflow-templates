package app.contacts.acceptance.support;

import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** Small JSON helpers for request bodies and assertions. */
public final class Json {

  public static final JsonMapper MAPPER = JsonMapper.builder().build();

  private Json() {}

  /** An object from key-value pairs; a value may be a String, Number, Boolean, JsonNode or null. */
  public static ObjectNode obj(Object... pairs) {
    ObjectNode node = MAPPER.createObjectNode();
    for (int i = 0; i < pairs.length; i += 2) {
      String key = (String) pairs[i];
      Object value = pairs[i + 1];
      if (value == null) {
        node.putNull(key);
      } else if (value instanceof JsonNode json) {
        node.set(key, json);
      } else if (value instanceof Integer number) {
        node.put(key, number);
      } else if (value instanceof Long number) {
        node.put(key, number);
      } else if (value instanceof Boolean flag) {
        node.put(key, flag);
      } else {
        node.put(key, value.toString());
      }
    }
    return node;
  }

  /** An array of strings. */
  public static ArrayNode strings(String... values) {
    ArrayNode array = MAPPER.createArrayNode();
    for (String value : values) {
      array.add(value);
    }
    return array;
  }

  /** An array of Attributes from key-value pairs. */
  public static ArrayNode attributes(String... keyValues) {
    ArrayNode array = MAPPER.createArrayNode();
    for (int i = 0; i < keyValues.length; i += 2) {
      array.add(obj("key", keyValues[i], "value", keyValues[i + 1]));
    }
    return array;
  }

  /** Parses JSON text; an empty text gives a missing node. */
  public static JsonNode parse(String text) {
    if (text == null || text.isBlank()) {
      return MAPPER.missingNode();
    }
    return MAPPER.readTree(text);
  }

  /** Serialises a node. */
  public static String write(JsonNode node) {
    return MAPPER.writeValueAsString(node);
  }

  /** The text of a node, or null when it is missing or JSON null. */
  public static String text(JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return null;
    }
    return node.asString();
  }

  /** The text of the given property of every element of an array. */
  public static List<String> texts(JsonNode array, String property) {
    List<String> result = new ArrayList<>();
    for (JsonNode element : array) {
      result.add(text(element.path(property)));
    }
    return result;
  }
}
