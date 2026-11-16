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

/** Tags of a Group: finds or creates them by name and puts them on Contacts (US-009). */
@Service
public class TagService {

  private final TagRepository tags;
  private final ContactTagRepository contactTags;

  /**
   * The service.
   *
   * @param tags the Tags
   * @param contactTags the Tags put on Contacts
   */
  public TagService(TagRepository tags, ContactTagRepository contactTags) {
    this.tags = tags;
    this.contactTags = contactTags;
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
