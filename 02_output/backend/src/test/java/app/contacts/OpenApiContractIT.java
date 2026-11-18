package app.contacts;

import static org.assertj.core.api.Assertions.assertThat;

import app.contacts.acceptance.support.AcceptanceTest;
import app.contacts.acceptance.support.Json;
import app.contacts.acceptance.support.Res;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * AR-02, DoD-P04 (ADR-06): the OpenAPI document springdoc generates from the running backend has
 * the paths, operations, parameters, success statuses and schemas of {@code
 * docs/02_contracts/openapi.yaml}. Schemas are compared by their shape (property names, nested
 * objects and array items, after resolving references), so the Java class names do not matter.
 */
class OpenApiContractIT extends AcceptanceTest {

  /** The prefix of the contract's {@code servers[0].url}, which the generated paths include. */
  private static final String SERVER = "/api/v1";

  private static final Set<String> METHODS = Set.of("get", "put", "post", "patch", "delete");

  private JsonNode contract;
  private JsonNode generated;

  @BeforeEach
  void readBothDocuments() throws IOException {
    contract = readContract();
    Res res = api.get(ana(), "/openapi.json");
    assertThat(res.status()).as("GET %s/openapi.json: %s", SERVER, res).isEqualTo(200);
    generated = res.json();
  }

  @Test
  void ar02GeneratedDocumentHasTheContractsOperations() {
    assertThat(operations(generated).keySet()).isEqualTo(operations(contract).keySet());
  }

  @Test
  void ar02EveryOperationHasTheContractsParametersStatusesAndSchemas() {
    Map<String, JsonNode> expected = operations(contract);
    Map<String, JsonNode> actual = operations(generated);
    SoftAssertions softly = new SoftAssertions();
    for (Map.Entry<String, JsonNode> entry : expected.entrySet()) {
      JsonNode mine = actual.get(entry.getKey());
      if (mine == null) {
        continue; // reported by ar02GeneratedDocumentHasTheContractsOperations
      }
      String op = entry.getKey();
      JsonNode theirs = entry.getValue();
      softly
          .assertThat(parameters(generated, mine))
          .as("%s parameters", op)
          .isEqualTo(parameters(contract, theirs));
      softly
          .assertThat(successStatuses(mine))
          .as("%s success statuses", op)
          .isEqualTo(successStatuses(theirs));
      softly
          .assertThat(shape(generated, requestSchema(mine)))
          .as("%s request body", op)
          .isEqualTo(shape(contract, requestSchema(theirs)));
      for (String status : successStatuses(theirs)) {
        softly
            .assertThat(shape(generated, responseSchema(mine, status)))
            .as("%s response %s", op, status)
            .isEqualTo(shape(contract, responseSchema(theirs, status)));
      }
    }
    softly.assertAll();
  }

  // ------------------------------------------------------------ Documents

  private static JsonNode readContract() throws IOException {
    Path file = Path.of(System.getProperty("contract.dir", "../docs/02_contracts"), "openapi.yaml");
    try (Reader reader = Files.newBufferedReader(file)) {
      Object yaml = new Yaml().load(reader);
      return Json.MAPPER.valueToTree(yaml);
    }
  }

  /** {@code "GET /groups/{groupId}"} → operation, with the server prefix removed. */
  private static Map<String, JsonNode> operations(JsonNode document) {
    Map<String, JsonNode> operations = new TreeMap<>();
    for (Map.Entry<String, JsonNode> path : document.path("paths").properties()) {
      String name =
          path.getKey().startsWith(SERVER)
              ? path.getKey().substring(SERVER.length())
              : path.getKey();
      for (Map.Entry<String, JsonNode> operation : path.getValue().properties()) {
        if (METHODS.contains(operation.getKey())) {
          operations.put(
              operation.getKey().toUpperCase(Locale.ROOT) + " " + name,
              withPathParameters(path.getValue(), operation.getValue()));
        }
      }
    }
    return operations;
  }

  /** The operation with the parameters of its path item added to its own. */
  private static JsonNode withPathParameters(JsonNode pathItem, JsonNode operation) {
    ObjectNode copy = (ObjectNode) operation.deepCopy();
    ArrayNode parameters = Json.MAPPER.createArrayNode();
    pathItem.path("parameters").forEach(parameters::add);
    operation.path("parameters").forEach(parameters::add);
    copy.set("parameters", parameters);
    return copy;
  }

  /** {@code "query q"}, {@code "path groupId"} … of an operation (references resolved). */
  private static Set<String> parameters(JsonNode document, JsonNode operation) {
    Set<String> names = new TreeSet<>();
    for (JsonNode parameter : operation.path("parameters")) {
      JsonNode resolved = resolve(document, parameter);
      names.add(Json.text(resolved.path("in")) + " " + Json.text(resolved.path("name")));
    }
    return names;
  }

  private static Set<String> successStatuses(JsonNode operation) {
    Set<String> statuses = new TreeSet<>();
    for (String status : operation.path("responses").propertyNames()) {
      if (status.startsWith("2")) {
        statuses.add(status);
      }
    }
    return statuses;
  }

  private static JsonNode requestSchema(JsonNode operation) {
    return firstContentSchema(operation.path("requestBody").path("content"));
  }

  private static JsonNode responseSchema(JsonNode operation, String status) {
    return firstContentSchema(operation.path("responses").path(status).path("content"));
  }

  private static JsonNode firstContentSchema(JsonNode content) {
    for (Map.Entry<String, JsonNode> media : content.properties()) {
      return media.getValue().path("schema");
    }
    return Json.MAPPER.missingNode();
  }

  // -------------------------------------------------------------- Shapes

  /**
   * The shape of a schema: {@code "object{a,b:object{…}}"}, {@code "array[…]"} or {@code "value"}
   * for anything without properties or items (a string, a number, an enum, a binary file).
   */
  private static String shape(JsonNode document, JsonNode schema) {
    return shape(document, schema, 0);
  }

  private static String shape(JsonNode document, JsonNode schema, int depth) {
    if (schema.isMissingNode() || depth > 8) {
      return "none";
    }
    JsonNode resolved = resolve(document, schema);
    JsonNode nonNull = nonNullVariant(document, allOfMerged(document, resolved));
    if (nonNull.has("items")) {
      return "array[" + shape(document, nonNull.path("items"), depth + 1) + "]";
    }
    if (nonNull.has("properties")) {
      List<String> properties = new ArrayList<>();
      for (Map.Entry<String, JsonNode> property :
          new TreeMap<>(asMap(nonNull.path("properties"))).entrySet()) {
        String inner = shape(document, property.getValue(), depth + 1);
        properties.add("value".equals(inner) ? property.getKey() : property.getKey() + ":" + inner);
      }
      return "object{" + String.join(",", properties) + "}";
    }
    if (nonNull.has("additionalProperties") && nonNull.path("additionalProperties").isObject()) {
      return "map<" + shape(document, nonNull.path("additionalProperties"), depth + 1) + ">";
    }
    return "value";
  }

  /** One object schema with the properties of every {@code allOf} part, or the schema itself. */
  private static JsonNode allOfMerged(JsonNode document, JsonNode schema) {
    if (!schema.has("allOf")) {
      return schema;
    }
    ObjectNode properties = Json.MAPPER.createObjectNode();
    for (JsonNode part : schema.path("allOf")) {
      allOfMerged(document, resolve(document, part))
          .path("properties")
          .properties()
          .forEach(e -> properties.set(e.getKey(), e.getValue()));
    }
    return Json.obj("type", "object", "properties", properties);
  }

  /** The schema of {@code oneOf/anyOf: [X, {type: null}]}, or the schema itself. */
  private static JsonNode nonNullVariant(JsonNode document, JsonNode schema) {
    for (String combiner : List.of("oneOf", "anyOf")) {
      for (JsonNode variant : schema.path(combiner)) {
        JsonNode resolved = resolve(document, variant);
        if (!"null".equals(Json.text(resolved.path("type")))) {
          return resolved;
        }
      }
    }
    return schema;
  }

  private static JsonNode resolve(JsonNode document, JsonNode node) {
    JsonNode current = node;
    while (current.has("$ref")) {
      String ref = Json.text(current.path("$ref"));
      current = document.at(ref.substring(1));
    }
    return current;
  }

  private static Map<String, JsonNode> asMap(JsonNode object) {
    Map<String, JsonNode> map = new TreeMap<>();
    object.properties().forEach(e -> map.put(e.getKey(), e.getValue()));
    return map;
  }
}
