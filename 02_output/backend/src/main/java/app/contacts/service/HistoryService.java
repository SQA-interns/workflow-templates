package app.contacts.service;

import app.contacts.domain.ContactFields;
import app.contacts.domain.ContactVersion;
import app.contacts.repository.ContactVersionRepository;
import app.contacts.repository.UserRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** The History of a Contact, its Versions and restoring one (US-010, US-011, ADR-07). */
@Service
public class HistoryService {

  /** Reads the plain records of a Version; immutable and thread-safe. */
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private static final TypeReference<List<ChangeView>> CHANGES = new TypeReference<>() {};

  private final ContactVersionRepository versions;
  private final UserRepository users;
  private final AccessGuard guard;
  private final ContactService contacts;

  /**
   * The service.
   *
   * @param versions the Version repository
   * @param users names the User of each Version
   * @param guard the access check
   * @param contacts changes the Contact to the restored content
   */
  public HistoryService(
      ContactVersionRepository versions,
      UserRepository users,
      AccessGuard guard,
      ContactService contacts) {
    this.versions = versions;
    this.users = users;
    this.guard = guard;
    this.contacts = contacts;
  }

  /**
   * Every Version of a Contact of the Group, newest first, with who made it, when and what changed
   * (R-010-01); any Member may read it.
   *
   * @param groupId the Group
   * @param userId the signed-in User
   * @param contactId the Contact
   * @return the History
   * @throws NotFoundException for a non-Member or a Contact that is not the Group's (BR-02)
   */
  @Transactional(readOnly = true)
  public List<VersionSummaryView> history(UUID groupId, UUID userId, UUID contactId) {
    guard.require(groupId, userId, Permission.READ);
    List<ContactVersion> found = versions.findHistory(groupId, contactId);
    if (found.isEmpty()) {
      throw new NotFoundException();
    }
    Map<UUID, String> names = namesOf(found);
    return found.stream()
        .map(
            version ->
                new VersionSummaryView(
                    version.getNumber(),
                    version.getKind(),
                    names.get(version.getChangedBy()),
                    version.getChangedAt(),
                    version.getRestoredFrom(),
                    changesOf(version)))
        .toList();
  }

  /**
   * One Version of a Contact of the Group with the full content after it (R-010-02); any Member may
   * read it.
   *
   * @param groupId the Group
   * @param userId the signed-in User
   * @param contactId the Contact
   * @param number the Version number
   * @return the Version
   * @throws NotFoundException for a non-Member, a Contact that is not the Group's (BR-02) or a
   *     Version number it does not have (EX-010-05)
   */
  @Transactional(readOnly = true)
  public VersionView version(UUID groupId, UUID userId, UUID contactId, int number) {
    guard.require(groupId, userId, Permission.READ);
    ContactVersion version =
        versions.findVersion(groupId, contactId, number).orElseThrow(NotFoundException::new);
    return new VersionView(
        version.getNumber(),
        version.getKind(),
        namesOf(List.of(version)).get(version.getChangedBy()),
        version.getChangedAt(),
        version.getRestoredFrom(),
        changesOf(version),
        JSON.readValue(version.getSnapshot(), SnapshotView.class));
  }

  /**
   * Makes the content of Version n current as a new Version {@code restored} that names n; every
   * Version stays (R-011-01, R-011-02); the Owner and Editors may do it (BR-03, D-27).
   *
   * @param groupId the Group
   * @param userId the signed-in User
   * @param contactId the Contact
   * @param number the number of the Version to restore
   * @param versionNumber the current Version number the client read (BR-06)
   * @return the restored Contact
   * @throws NotFoundException for a non-Member, a Contact that is not the Group's (BR-02) or a
   *     Version number it does not have
   * @throws ForbiddenException for a Viewer (BR-03, AC-011-03)
   * @throws ConflictException as {@link ContactService#restore}
   */
  @Transactional
  public ContactView restoreVersion(
      UUID groupId, UUID userId, UUID contactId, int number, int versionNumber) {
    guard.require(groupId, userId, Permission.CHANGE_CONTACT);
    ContactVersion version =
        versions.findVersion(groupId, contactId, number).orElseThrow(NotFoundException::new);
    SnapshotView snapshot = JSON.readValue(version.getSnapshot(), SnapshotView.class);
    ContactContent content =
        new ContactContent(
            ContactFields.ofNames(snapshot.fields()), snapshot.attributes(), snapshot.tags());
    return contacts.restore(groupId, userId, contactId, versionNumber, number, content);
  }

  /** The display name of the User of each Version, removed Members too (EX-010-02). */
  private Map<UUID, String> namesOf(List<ContactVersion> found) {
    Map<UUID, String> names = new HashMap<>();
    users
        .findAllById(found.stream().map(ContactVersion::getChangedBy).distinct().toList())
        .forEach(user -> names.put(user.getId(), user.getDisplayName()));
    return names;
  }

  private static List<ChangeView> changesOf(ContactVersion version) {
    return JSON.readValue(version.getChanges(), CHANGES);
  }
}
