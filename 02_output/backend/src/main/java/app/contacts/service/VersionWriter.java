package app.contacts.service;

import app.contacts.domain.Contact;
import app.contacts.domain.ContactVersion;
import app.contacts.repository.ContactVersionRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SequencedMap;
import java.util.Set;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Writes the Version of every saved change of a Contact (BR-05, ADR-07). */
@Component
public class VersionWriter {

  /** Writes the plain records of a Version; immutable and thread-safe. */
  private static final JsonMapper JSON = JsonMapper.builder().build();

  /** The prefix of an Attribute's changed item (contract {@code Change}). */
  private static final String ATTRIBUTE = "attribute:";

  private final ContactVersionRepository versions;

  /**
   * The writer.
   *
   * @param versions the Version repository
   */
  public VersionWriter(ContactVersionRepository versions) {
    this.versions = versions;
  }

  /**
   * Writes the Version of the Contact's current Version number with the items that differ between
   * {@code before} and {@code after} (Fields, then Attributes as {@code attribute:<key>}, R-008-01)
   * and the full content after the change, in the Contact's transaction (AR-08).
   *
   * @param contact the Contact after the change
   * @param kind the kind, e.g. {@link ContactVersion#CREATED}
   * @param before the content before the change ({@link ContactContent#EMPTY} for a new Contact)
   * @param after the content after the change
   */
  public void record(Contact contact, String kind, ContactContent before, ContactContent after) {
    SequencedMap<String, String> fields = after.fields().byName();
    List<Change> changes = new ArrayList<>();
    changes.addAll(changed(before.fields().byName(), fields, ""));
    changes.addAll(changed(byKey(before), byKey(after), ATTRIBUTE));
    Snapshot snapshot = new Snapshot(fields, after.attributes(), List.of());
    versions.add(
        contact.getGroupId(),
        ContactVersion.of(
            contact, kind, JSON.writeValueAsString(changes), JSON.writeValueAsString(snapshot)));
  }

  /** The items whose value differs, in the order of {@code old} and then of new items. */
  private static List<Change> changed(
      Map<String, String> old, Map<String, String> current, String prefix) {
    Set<String> items = new LinkedHashSet<>(old.keySet());
    items.addAll(current.keySet());
    return items.stream()
        .filter(item -> !Objects.equals(old.get(item), current.get(item)))
        .map(item -> new Change(prefix + item, old.get(item), current.get(item)))
        .toList();
  }

  private static SequencedMap<String, String> byKey(ContactContent content) {
    SequencedMap<String, String> attributes = new LinkedHashMap<>();
    content.attributes().forEach(attribute -> attributes.put(attribute.key(), attribute.value()));
    return attributes;
  }

  /** One changed item of a Version (Data model "changes"). */
  private record Change(String item, String oldValue, String newValue) {}

  /** The full content of a Contact after a change (Data model "snapshot"). */
  private record Snapshot(
      SequencedMap<String, String> fields, List<AttributeView> attributes, List<String> tags) {}
}
