package app.contacts.service;

import app.contacts.domain.Contact;
import app.contacts.domain.ContactFields;
import app.contacts.domain.ContactVersion;
import app.contacts.repository.ContactVersionRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SequencedMap;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Writes the Version of every saved change of a Contact (BR-05, ADR-07). */
@Component
public class VersionWriter {

  /** Writes the plain records of a Version; immutable and thread-safe. */
  private static final JsonMapper JSON = JsonMapper.builder().build();

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
   * Writes the Version of the Contact's current Version number with the items that differ from
   * {@code before} and the full content after the change, in the Contact's transaction (AR-08).
   *
   * @param contact the Contact after the change
   * @param kind the kind, e.g. {@link ContactVersion#CREATED}
   * @param before the Fields before the change ({@link ContactFields#EMPTY} for a new Contact)
   */
  public void write(Contact contact, String kind, ContactFields before) {
    SequencedMap<String, String> after = contact.getFields().byName();
    Map<String, String> old = before.byName();
    List<Change> changes =
        after.entrySet().stream()
            .filter(field -> !Objects.equals(old.get(field.getKey()), field.getValue()))
            .map(field -> new Change(field.getKey(), old.get(field.getKey()), field.getValue()))
            .toList();
    Snapshot snapshot = new Snapshot(after, List.of(), List.of());
    versions.add(
        contact.getGroupId(),
        ContactVersion.of(
            contact, kind, JSON.writeValueAsString(changes), JSON.writeValueAsString(snapshot)));
  }

  /** One changed item of a Version (Data model "changes"). */
  private record Change(String item, String oldValue, String newValue) {}

  /** The full content of a Contact after a change (Data model "snapshot"). */
  private record Snapshot(
      SequencedMap<String, String> fields, List<Object> attributes, List<String> tags) {}
}
