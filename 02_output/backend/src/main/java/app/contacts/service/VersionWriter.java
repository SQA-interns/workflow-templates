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

  /** The prefix of a Tag's changed item (contract {@code Change}). */
  private static final String TAG = "tag:";

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
   * {@code before} and {@code after} (Fields, then Attributes as {@code attribute:<key>}, R-008-01,
   * then Tags as {@code tag:<name>}, R-009-01) and the full content after the change, in the
   * Contact's transaction (AR-08).
   *
   * @param contact the Contact after the change
   * @param kind the kind, e.g. {@link ContactVersion#CREATED}
   * @param before the content before the change ({@link ContactContent#EMPTY} for a new Contact)
   * @param after the content after the change
   */
  public void record(Contact contact, String kind, ContactContent before, ContactContent after) {
    versions.add(
        contact.getGroupId(),
        ContactVersion.of(contact, kind, changes(before, after), snapshot(after)));
  }

  /**
   * Writes the Version {@link ContactVersion#RESTORED} of the Contact's current Version number that
   * names the restored Version and lists each changed item as {@link #record} does (R-011-01).
   *
   * @param contact the Contact after the restore
   * @param restoredFrom the number of the restored Version
   * @param before the content before the restore
   * @param after the content of the restored Version
   */
  public void recordRestored(
      Contact contact, int restoredFrom, ContactContent before, ContactContent after) {
    versions.add(
        contact.getGroupId(),
        ContactVersion.restored(contact, restoredFrom, changes(before, after), snapshot(after)));
  }

  /** JSON of the items that differ: Fields, then Attributes, then Tags. */
  private static String changes(ContactContent before, ContactContent after) {
    List<ChangeView> changes = new ArrayList<>();
    changes.addAll(changed(before.fields().byName(), after.fields().byName(), ""));
    changes.addAll(changed(byKey(before), byKey(after), ATTRIBUTE));
    changes.addAll(changed(byName(before), byName(after), TAG));
    return JSON.writeValueAsString(changes);
  }

  /** JSON of the full content after the change. */
  private static String snapshot(ContactContent after) {
    SequencedMap<String, String> fields = after.fields().byName();
    return JSON.writeValueAsString(new SnapshotView(fields, after.attributes(), after.tags()));
  }

  /** The items whose value differs, in the order of {@code old} and then of new items. */
  private static List<ChangeView> changed(
      Map<String, String> old, Map<String, String> current, String prefix) {
    Set<String> items = new LinkedHashSet<>(old.keySet());
    items.addAll(current.keySet());
    return items.stream()
        .filter(item -> !Objects.equals(old.get(item), current.get(item)))
        .map(item -> new ChangeView(prefix + item, old.get(item), current.get(item)))
        .toList();
  }

  private static SequencedMap<String, String> byKey(ContactContent content) {
    SequencedMap<String, String> attributes = new LinkedHashMap<>();
    content.attributes().forEach(attribute -> attributes.put(attribute.key(), attribute.value()));
    return attributes;
  }

  /** Each Tag name to itself: an added Tag has the name as new value, a removed one as old. */
  private static SequencedMap<String, String> byName(ContactContent content) {
    SequencedMap<String, String> tags = new LinkedHashMap<>();
    content.tags().forEach(tag -> tags.put(tag, tag));
    return tags;
  }
}
