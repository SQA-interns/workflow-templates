package app.contacts.service;

import app.contacts.domain.Contact;
import app.contacts.domain.ContactTag;
import app.contacts.domain.Tag;
import app.contacts.repository.ContactTagRepository;
import app.contacts.repository.TagRepository;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tags of a Group: finds or creates them by name, puts them on Contacts and suggests them (US-009).
 */
@Service
public class TagService {

  /** Longest typed text, as long as a Tag name (contract parameter {@code q}, R-009-05). */
  static final int TEXT_MAX = 50;

  private final TagRepository tags;
  private final ContactTagRepository contactTags;
  private final AccessGuard guard;

  /**
   * The service.
   *
   * @param tags the Tags
   * @param contactTags the Tags put on Contacts
   * @param guard the access check
   */
  public TagService(TagRepository tags, ContactTagRepository contactTags, AccessGuard guard) {
    this.tags = tags;
    this.contactTags = contactTags;
    this.guard = guard;
  }

  /**
   * The Tags to suggest for the typed text: those of the Group on at least one active Contact whose
   * name contains the text case-insensitively (R-009-02); any Member may ask.
   *
   * @param groupId the Group
   * @param userId the signed-in User
   * @param text the typed text, trimmed; {@code null} or empty for every such Tag
   * @return the Tags by name A–Z
   * @throws NotFoundException for a non-Member (BR-02)
   * @throws ValidationException {@code field.tooLong} on {@code q} for a text of more than 50
   *     characters (contract parameter {@code q})
   */
  @Transactional(readOnly = true)
  public List<TagView> suggest(UUID groupId, UUID userId, String text) {
    guard.require(groupId, userId, Permission.READ);
    String typed = text == null ? "" : text.strip();
    if (typed.codePointCount(0, typed.length()) > TEXT_MAX) {
      throw new ValidationException(
          List.of(new FieldProblem("q", "field.tooLong", Map.of("field", "q", "max", TEXT_MAX))));
    }
    return tags.suggest(groupId, typed).stream()
        .map(tag -> new TagView(tag.getId(), tag.getName()))
        .toList();
  }

  /**
   * The Tags of the Group with these names: the existing Tag of a name compared case-insensitively
   * with its spelling (R-009-03), else a new Tag spelled as entered (R-009-04); each Tag once
   * (R-009-06). Called inside the save's transaction.
   *
   * @param groupId the Group
   * @param names the checked names
   * @return the Tags in the order of the names
   */
  public List<Tag> resolve(UUID groupId, List<String> names) {
    Map<UUID, Tag> resolved = new LinkedHashMap<>();
    for (String name : names) {
      Tag tag =
          tags.findInGroup(groupId, name)
              .orElseGet(() -> tags.add(groupId, Tag.create(groupId, name)));
      resolved.putIfAbsent(tag.getId(), tag);
    }
    return List.copyOf(resolved.values());
  }

  /**
   * Puts the wanted Tags on the Contact that it does not have and takes off the ones it has that
   * are not wanted (R-009-01).
   *
   * @param contact the Contact
   * @param current the ids of the Tags the Contact has
   * @param wanted the Tags it shall have, of its Group
   */
  public void retag(Contact contact, Collection<UUID> current, List<Tag> wanted) {
    Set<UUID> wantedIds = wanted.stream().map(Tag::getId).collect(Collectors.toSet());
    Set<UUID> removed =
        current.stream().filter(id -> !wantedIds.contains(id)).collect(Collectors.toSet());
    if (!removed.isEmpty()) {
      contactTags.discardOfContact(contact.getGroupId(), contact.getId(), removed);
    }
    wanted.stream()
        .filter(tag -> !current.contains(tag.getId()))
        .forEach(tag -> contactTags.add(contact.getGroupId(), ContactTag.of(contact, tag)));
  }
}
