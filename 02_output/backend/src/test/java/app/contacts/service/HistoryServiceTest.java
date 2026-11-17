package app.contacts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.contacts.domain.Contact;
import app.contacts.domain.ContactFields;
import app.contacts.domain.ContactVersion;
import app.contacts.domain.User;
import app.contacts.repository.ContactVersionRepository;
import app.contacts.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HistoryServiceTest {

  private static final Instant CREATED_AT = Instant.parse("2026-03-05T09:00:00Z");
  private static final Instant EDITED_AT = Instant.parse("2026-07-01T08:00:00Z");
  private static final UUID GROUP_ID = UUID.randomUUID();
  private static final UUID PETRA = UUID.randomUUID();
  private static final UUID MARKO = UUID.randomUUID();

  private static final String CREATED_CHANGES =
      """
      [{"item":"firstName","oldValue":null,"newValue":"Janez"},
       {"item":"email","oldValue":null,"newValue":"janez.novak@example.si"}]""";
  private static final String CREATED_SNAPSHOT =
      """
      {"fields":{"firstName":"Janez","lastName":null,"email":"janez.novak@example.si"},
       "attributes":[{"key":"Kompetenca","value":"Java"}],"tags":["udelezenec2025"]}""";
  private static final String EDITED_CHANGES =
      """
      [{"item":"email","oldValue":"janez.novak@example.si","newValue":"janez@novak.si"}]""";

  private final ContactVersionRepository versions = mock(ContactVersionRepository.class);
  private final UserRepository users = mock(UserRepository.class);
  private final AccessGuard guard = mock(AccessGuard.class);
  private final ContactService contacts = mock(ContactService.class);
  private final HistoryService service = new HistoryService(versions, users, guard, contacts);

  private final User ana = User.create("uid-ana", "ana.kovac@example.si", "Ana Kovač", CREATED_AT);
  private final User marko =
      User.create("uid-marko", "marko.kranjc@gmail.com", "Marko Kranjc", CREATED_AT);
  private Contact janez;
  private ContactVersion created;
  private ContactVersion edited;

  @BeforeEach
  void janezCreatedByAnaEditedByMarko() {
    ContactFields fields = ContactFields.ofNames(Map.of("firstName", "Janez"));
    janez = Contact.create(GROUP_ID, fields, "", "", ana.getId(), CREATED_AT);
    created = ContactVersion.of(janez, ContactVersion.CREATED, CREATED_CHANGES, CREATED_SNAPSHOT);
    janez.edit(fields, "", "", marko.getId(), EDITED_AT);
    edited = ContactVersion.of(janez, ContactVersion.EDITED, EDITED_CHANGES, CREATED_SNAPSHOT);
    when(users.findAllById(any())).thenReturn(List.of(ana, marko));
  }

  @Test
  void r01001HistoryListsEveryVersionNewestFirstWithWhoWhenAndWhat() {
    when(versions.findHistory(GROUP_ID, janez.getId())).thenReturn(List.of(edited, created));

    List<VersionSummaryView> history = service.history(GROUP_ID, PETRA, janez.getId());

    assertThat(history)
        .containsExactly(
            new VersionSummaryView(
                2,
                "edited",
                "Marko Kranjc",
                EDITED_AT,
                null,
                List.of(new ChangeView("email", "janez.novak@example.si", "janez@novak.si"))),
            new VersionSummaryView(
                1,
                "created",
                "Ana Kovač",
                CREATED_AT,
                null,
                List.of(
                    new ChangeView("firstName", null, "Janez"),
                    new ChangeView("email", null, "janez.novak@example.si"))));
  }

  @Test
  void r01101HistoryAndVersionNameTheVersionARestoredVersionMadeCurrent() {
    janez.edit(janez.getFields(), "", "", marko.getId(), EDITED_AT);
    ContactVersion restored = ContactVersion.restored(janez, 1, EDITED_CHANGES, CREATED_SNAPSHOT);
    when(versions.findHistory(GROUP_ID, janez.getId()))
        .thenReturn(List.of(restored, edited, created));
    when(versions.findVersion(GROUP_ID, janez.getId(), 3)).thenReturn(Optional.of(restored));

    List<VersionSummaryView> history = service.history(GROUP_ID, PETRA, janez.getId());

    assertThat(history)
        .extracting(VersionSummaryView::kind, VersionSummaryView::restoredFrom)
        .containsExactly(tuple("restored", 1), tuple("edited", null), tuple("created", null));
    assertThat(service.version(GROUP_ID, PETRA, janez.getId(), 3).restoredFrom()).isEqualTo(1);
  }

  @Test
  void r01101RestoreVersionMakesTheContentOfVersionNCurrent() {
    when(versions.findVersion(GROUP_ID, janez.getId(), 1)).thenReturn(Optional.of(created));
    ContactView restored = mock(ContactView.class);
    ContactContent version1 =
        new ContactContent(
            ContactFields.ofNames(Map.of("firstName", "Janez", "email", "janez.novak@example.si")),
            List.of(new AttributeView("Kompetenca", "Java")),
            List.of("udelezenec2025"));
    when(contacts.restore(GROUP_ID, MARKO, janez.getId(), 2, 1, version1)).thenReturn(restored);

    assertThat(service.restoreVersion(GROUP_ID, MARKO, janez.getId(), 1, 2)).isSameAs(restored);
  }

  @Test
  void ac01103ViewerCannotRestoreAVersionAndNothingIsRead() {
    when(guard.require(GROUP_ID, PETRA, Permission.CHANGE_CONTACT))
        .thenThrow(new ForbiddenException());

    assertThatThrownBy(() -> service.restoreVersion(GROUP_ID, PETRA, janez.getId(), 1, 2))
        .isInstanceOf(ForbiddenException.class);
    verifyNoInteractions(versions, contacts);
  }

  @Test
  void r01101RestoreOfAVersionNumberThatDoesNotExistIsNotFound() {
    when(versions.findVersion(GROUP_ID, janez.getId(), 99)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.restoreVersion(GROUP_ID, MARKO, janez.getId(), 99, 2))
        .isInstanceOf(NotFoundException.class);
    verifyNoInteractions(contacts);
  }

  @Test
  void ac01003HistoryOfContactNotOfTheGroupIsNotFound() {
    when(versions.findHistory(GROUP_ID, janez.getId())).thenReturn(List.of());

    assertThatThrownBy(() -> service.history(GROUP_ID, PETRA, janez.getId()))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void ac01003NonMemberGetsNotFoundForTheHistory() {
    when(guard.require(GROUP_ID, PETRA, Permission.READ)).thenThrow(new NotFoundException());

    assertThatThrownBy(() -> service.history(GROUP_ID, PETRA, janez.getId()))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void r01002VersionShowsTheFullContentAfterIt() {
    when(versions.findVersion(GROUP_ID, janez.getId(), 1)).thenReturn(Optional.of(created));

    VersionView version = service.version(GROUP_ID, PETRA, janez.getId(), 1);

    assertThat(version.number()).isEqualTo(1);
    assertThat(version.changedBy()).isEqualTo("Ana Kovač");
    assertThat(version.snapshot().fields())
        .containsEntry("firstName", "Janez")
        .containsEntry("email", "janez.novak@example.si")
        .containsEntry("lastName", null);
    assertThat(version.snapshot().attributes())
        .containsExactly(new AttributeView("Kompetenca", "Java"));
    assertThat(version.snapshot().tags()).containsExactly("udelezenec2025");
  }

  @Test
  void ex01005VersionNumberThatDoesNotExistIsNotFound() {
    when(versions.findVersion(GROUP_ID, janez.getId(), 99)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.version(GROUP_ID, PETRA, janez.getId(), 99))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void ac01003NonMemberGetsNotFoundForVersion() {
    when(guard.require(GROUP_ID, PETRA, Permission.READ)).thenThrow(new NotFoundException());

    assertThatThrownBy(() -> service.version(GROUP_ID, PETRA, janez.getId(), 1))
        .isInstanceOf(NotFoundException.class);
  }
}
