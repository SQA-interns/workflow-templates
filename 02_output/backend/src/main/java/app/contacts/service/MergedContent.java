package app.contacts.service;

import app.contacts.domain.ContactFields;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The content of the Primary contact after a Merge: the chosen Field values, the Attributes and the
 * Tags of both Contacts (R-018-01 … R-018-03, R-018-06).
 */
final class MergedContent {

  /** Keep the Primary contact's value. */
  static final String PRIMARY = "primary";

  /** Keep the Other contact's value. */
  static final String OTHER = "other";

  private static final Set<String> CHOICES = Set.of(PRIMARY, OTHER);

  private MergedContent() {}

  /**
   * The merged content.
   *
   * @param primary the Primary contact's content
   * @param other the Other contact's content
   * @param fieldChoices Field name to {@code primary} or {@code other}; a Field without a choice
   *     keeps the Primary contact's value unless it is blank (R-018-01); {@code null} for none
   * @param attributeChoices Attribute key to {@code primary} or {@code other}, needed for every key
   *     with two different values (R-018-03); {@code null} for none
   * @return the content, Fields not yet checked together
   * @throws ValidationException for an unknown Field name or choice
   * @throws ConflictException {@code merge.tooManyAttributes} for more than 50 Attributes together
   *     (R-018-06)
   * @throws InvalidRequestException {@code merge.choiceRequired} with the first key, A–Z, that has
   *     two values and no choice (R-018-03)
   */
  static ContactContent of(
      ContactContent primary,
      ContactContent other,
      Map<String, String> fieldChoices,
      Map<String, String> attributeChoices) {
    Map<String, String> fields = orNone(fieldChoices);
    Map<String, String> attributes = orNone(attributeChoices);
    requireKnown(fields, ContactFields.NAMES);
    requireKnown(attributes, null);
    return new ContactContent(
        fields(primary, other, fields),
        attributes(primary.attributes(), other.attributes(), attributes),
        tags(primary.tags(), other.tags()));
  }

  private static Map<String, String> orNone(Map<String, String> choices) {
    return choices == null ? Map.of() : choices;
  }

  /** Rejects a name outside {@code names} (when given) or a choice other than primary or other. */
  private static void requireKnown(Map<String, String> choices, List<String> names) {
    boolean unknown =
        (names != null && !names.containsAll(choices.keySet()))
            || !CHOICES.containsAll(choices.values());
    if (unknown) {
      throw new ValidationException(List.of());
    }
  }

  private static ContactFields fields(
      ContactContent primary, ContactContent other, Map<String, String> choices) {
    Map<String, String> kept = primary.fields().byName();
    Map<String, String> others = other.fields().byName();
    Map<String, String> merged = new LinkedHashMap<>();
    kept.forEach(
        (name, value) -> {
          String choice = choices.getOrDefault(name, value == null ? OTHER : PRIMARY);
          merged.put(name, OTHER.equals(choice) ? others.get(name) : value);
        });
    return ContactFields.ofNames(merged);
  }

  /** The Attributes of both, one per key ignoring case, the Primary contact's spelling first. */
  private static List<AttributeView> attributes(
      List<AttributeView> primary, List<AttributeView> other, Map<String, String> choices) {
    Map<String, AttributeView> byKey = new LinkedHashMap<>();
    primary.forEach(attribute -> byKey.put(lower(attribute.key()), attribute));
    List<String> conflicts = new ArrayList<>();
    for (AttributeView attribute : other) {
      AttributeView kept = byKey.putIfAbsent(lower(attribute.key()), attribute);
      if (kept != null && !kept.value().equals(attribute.value())) {
        String choice = choiceFor(choices, kept.key());
        if (choice == null) {
          conflicts.add(kept.key());
        } else if (OTHER.equals(choice)) {
          byKey.put(lower(kept.key()), new AttributeView(kept.key(), attribute.value()));
        }
      }
    }
    if (byKey.size() > ContactValidator.MAX_ATTRIBUTES) {
      throw new ConflictException("merge.tooManyAttributes", Map.of());
    }
    if (!conflicts.isEmpty()) {
      String first = conflicts.stream().min(String.CASE_INSENSITIVE_ORDER).orElseThrow();
      throw new InvalidRequestException("merge.choiceRequired", Map.of("key", first));
    }
    return List.copyOf(byKey.values());
  }

  /** The choice for a key, matched ignoring case. */
  private static String choiceFor(Map<String, String> choices, String key) {
    return choices.entrySet().stream()
        .filter(choice -> lower(choice.getKey()).equals(lower(key)))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElse(null);
  }

  /** The Tags of both, each name once ignoring case (EX-018-07). */
  private static List<String> tags(List<String> primary, List<String> other) {
    Map<String, String> byName = new LinkedHashMap<>();
    primary.forEach(name -> byName.put(lower(name), name));
    other.forEach(name -> byName.putIfAbsent(lower(name), name));
    return List.copyOf(byName.values());
  }

  private static String lower(String text) {
    return text.toLowerCase(Locale.ROOT);
  }
}
