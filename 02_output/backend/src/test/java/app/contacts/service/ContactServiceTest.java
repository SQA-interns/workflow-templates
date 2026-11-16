package app.contacts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.contacts.domain.Attribute;
import app.contacts.domain.Contact;
import app.contacts.domain.ContactAttributeRow;
import app.contacts.domain.ContactFields;
import app.contacts.domain.ContactTag;
import app.contacts.domain.ContactTagRow;
import app.contacts.domain.ContactVersion;
import app.contacts.domain.Role;
import app.contacts.domain.Tag;
import app.contacts.domain.User;
import app.contacts.repository.AttributeRepository;
import app.contacts.repository.ContactQueryRepository;
import app.contacts.repository.ContactRepository;
import app.contacts.repository.ContactTagRepository;
import app.contacts.repository.TagRepository;
import app.contacts.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class ContactServiceTest {

  private static final Instant NOW = Instant.parse("2026-03-05T10:00:00Z");
  private static final UUID GROUP_ID = UUID.randomUUID();
  private static final UUID MARKO = UUID.randomUUID();
  private static final UUID PETRA = UUID.randomUUID();
  private static final UUID ANA = UUID.randomUUID();

  private final ContactRepository contacts = mock(ContactRepository.class);
  private final VersionWriter versionWriter = mock(VersionWriter.class);
  private final AccessGuard guard = mock(AccessGuard.class);
  private final ContactQueryRepository contactQueries = mock(ContactQueryRepository.class);
  private final UserRepository users = mock(UserRepository.class);
  private final AttributeRepository attributes = mock(AttributeRepository.class);
  private final TagRepository tags = mock(TagRepository.class);
  private final ContactTagRepository contactTags = mock(ContactTagRepository.class);
  private final ContactService service =
      new ContactService(
          contacts,
          attributes,
          new TagService(tags, contactTags, guard),
          contactQueries,
          users,
          versionWriter,
          guard,
          new ContactValidator(),
          new Normaliser(),
          Clock.fixed(NOW, ZoneOffset.UTC));

  @BeforeEach
  void markoIsEditor() {
    when(guard.require(GROUP_ID, MARKO, Permission.CHANGE_CONTACT)).thenReturn(Role.EDITOR);
    when(contacts.add(eq(GROUP_ID), any(Contact.class))).thenAnswer(call -> call.getArgument(1));
    when(tags.findInGroup(eq(GROUP_ID), anyString())).thenReturn(Optional.empty());
    when(tags.add(eq(GROUP_ID), any(Tag.class))).thenAnswer(call -> call.getArgument(1));
  }

  @Test
  void r00601EditorCreatesTheContactWithVersionOneCreated() {
    ContactView created =
        service.create(
            GROUP_ID,
            MARKO,
            Map.of("firstName", "Janez", "lastName", "Novak", "country", "SI"),
            List.of(),
            null);

    ArgumentCaptor<Contact> stored = ArgumentCaptor.forClass(Contact.class);
    verify(contacts).add(eq(GROUP_ID), stored.capture());
    verify(versionWriter)
        .record(
            eq(stored.getValue()),
            eq(ContactVersion.CREATED),
            eq(ContactContent.EMPTY),
            any(ContactContent.class));
    assertThat(created.id()).isEqualTo(stored.getValue().getId());
    assertThat(created.versionNumber()).isEqualTo(1);
    assertThat(created.state()).isEqualTo("active");
    assertThat(created.fields()).containsEntry("lastName", "Novak").containsEntry("email", null);
    assertThat(created.attributes()).isEmpty();
    assertThat(created.tags()).isEmpty();
    assertThat(created.createdAt()).isEqualTo(NOW);
    assertThat(created.updatedAt()).isEqualTo(NOW);
  }

  @Test
  void r00601StoresTheDerivedSearchTextAndSortName() {
    service.create(
        GROUP_ID, MARKO, Map.of("firstName", "Žiga", "lastName", "Čuk"), List.of(), null);

    ArgumentCaptor<Contact> stored = ArgumentCaptor.forClass(Contact.class);
    verify(contacts).add(eq(GROUP_ID), stored.capture());
    assertThat(stored.getValue().getSearchText()).isEqualTo("ziga cuk");
    assertThat(stored.getValue().getSortName()).isEqualTo("Čuk");
  }

  @Test
  void r00607StoresTheTrimmedFields() {
    ContactView created =
        service.create(GROUP_ID, MARKO, Map.of("firstName", "  Janez "), List.of(), null);

    assertThat(created.fields()).containsEntry("firstName", "Janez");
  }

  @Test
  void r00602InvalidFieldsStoreNothing() {
    assertThatThrownBy(
            () ->
                service.create(
                    GROUP_ID, MARKO, Map.of("email", "info@example.si"), List.of(), null))
        .isInstanceOf(ValidationException.class);
    verifyNoInteractions(contacts, versionWriter);
  }

  @Test
  void r00601UnknownFieldNamesAreRejected() {
    assertThatThrownBy(
            () ->
                service.create(
                    GROUP_ID,
                    MARKO,
                    Map.of("firstName", "Janez", "nickname", "J"),
                    List.of(),
                    null))
        .isInstanceOf(ValidationException.class);
    verifyNoInteractions(contacts, versionWriter);
  }

  @Test
  void r00601MissingFieldsAreRejected() {
    assertThatThrownBy(() -> service.create(GROUP_ID, MARKO, null, List.of(), null))
        .isInstanceOf(ValidationException.class);
    verifyNoInteractions(contacts, versionWriter);
  }

  @Test
  void r00601NullFieldValuesAreEmptyFields() {
    Map<String, String> fields = new HashMap<>();
    fields.put("company", "Podjetje d.o.o.");
    fields.put("firstName", null);

    assertThat(service.create(GROUP_ID, MARKO, fields, List.of(), null).fields())
        .containsEntry("company", "Podjetje d.o.o.");
  }

  @Test
  void ac00606ViewerIsForbiddenBeforeAnyCheckAndNothingIsStored() {
    when(guard.require(GROUP_ID, PETRA, Permission.CHANGE_CONTACT))
        .thenThrow(new ForbiddenException());

    assertThatThrownBy(() -> service.create(GROUP_ID, PETRA, Map.of(), List.of(), null))
        .isInstanceOf(ForbiddenException.class);
    verifyNoInteractions(contacts, versionWriter);
  }

  @Test
  void ex00609NonMemberGetsNotFoundAndNothingIsStored() {
    UUID other = UUID.randomUUID();
    when(guard.require(other, MARKO, Permission.CHANGE_CONTACT)).thenThrow(new NotFoundException());

    assertThatThrownBy(
            () -> service.create(other, MARKO, Map.of("firstName", "Janez"), List.of(), null))
        .isInstanceOf(NotFoundException.class);
    verifyNoInteractions(contacts, versionWriter);
  }

  @Test
  void r00606ListsTheActiveContactsWithTheEmailForAnyMember() {
    when(guard.require(GROUP_ID, PETRA, Permission.READ)).thenReturn(Role.VIEWER);
    Contact janez = contact(Map.of("firstName", "Janez", "lastName", "Novak"));
    Contact company = contact(Map.of("company", "Podjetje d.o.o."));
    Contact first = contact(Map.of("firstName", "Janez"));
    when(contacts.findActiveByEmail(GROUP_ID, "Janez.Novak@example.si"))
        .thenReturn(List.of(janez, company, first));

    assertThat(service.findByEmail(GROUP_ID, PETRA, " Janez.Novak@example.si "))
        .containsExactly(
            new ContactRefView(janez.getId(), "Janez Novak"),
            new ContactRefView(company.getId(), "Podjetje d.o.o."),
            new ContactRefView(first.getId(), "Janez"));
  }

  @Test
  void r00606BlankEmailMatchesNothing() {
    when(guard.require(GROUP_ID, PETRA, Permission.READ)).thenReturn(Role.VIEWER);

    assertThat(service.findByEmail(GROUP_ID, PETRA, "  ")).isEmpty();
    verifyNoInteractions(contacts);
  }

  @Test
  void br02NonMemberGetsNotFoundForEmailMatches() {
    UUID other = UUID.randomUUID();
    when(guard.require(other, PETRA, Permission.READ)).thenThrow(new NotFoundException());

    assertThatThrownBy(() -> service.findByEmail(other, PETRA, "janez.novak@example.si"))
        .isInstanceOf(NotFoundException.class);
    verifyNoInteractions(contacts);
  }

  @Test
  void r01406AnyMemberReadsContactOfTheGroup() {
    when(guard.require(GROUP_ID, PETRA, Permission.READ)).thenReturn(Role.VIEWER);
    Contact janez = contact(Map.of("firstName", "Janez", "lastName", "Novak"));
    when(contacts.findByGroupIdAndId(GROUP_ID, janez.getId())).thenReturn(Optional.of(janez));

    UUID tag = UUID.randomUUID();
    when(contactQueries.findAttributes(GROUP_ID, janez.getId()))
        .thenReturn(List.of(new ContactAttributeRow("kompetenca", "spletnoProgramiranje")));
    when(contactQueries.findTags(GROUP_ID, List.of(janez.getId())))
        .thenReturn(List.of(new ContactTagRow(janez.getId(), tag, "udelezenec2025")));

    ContactView view = service.get(GROUP_ID, PETRA, janez.getId());

    assertThat(view.attributes())
        .containsExactly(new AttributeView("kompetenca", "spletnoProgramiranje"));
    assertThat(view.tags()).containsExactly(new TagView(tag, "udelezenec2025"));
    assertThat(view.id()).isEqualTo(janez.getId());
    assertThat(view.fields()).containsEntry("lastName", "Novak");
  }

  @Test
  void br02AnUnknownContactOrOneOfAnotherGroupIsNotFound() {
    when(guard.require(GROUP_ID, PETRA, Permission.READ)).thenReturn(Role.VIEWER);
    UUID unknown = UUID.randomUUID();
    when(contacts.findByGroupIdAndId(GROUP_ID, unknown)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.get(GROUP_ID, PETRA, unknown))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void r00701EditorSavesChangedFieldsAsOneVersionEdited() {
    Contact janez =
        stored(
            Map.of("firstName", "Janez", "lastName", "Novak", "email", "janez.novak@example.si"));
    ContactFields before = janez.getFields();

    ContactView saved =
        service.update(
            GROUP_ID,
            MARKO,
            janez.getId(),
            1,
            Map.of("firstName", "Janez", "lastName", "Novak", "email", " janez@novak.si "),
            null,
            null);

    verify(versionWriter)
        .record(
            eq(janez),
            eq(ContactVersion.EDITED),
            eq(new ContactContent(before, List.of(), List.of())),
            any(ContactContent.class));
    assertThat(saved.versionNumber()).isEqualTo(2);
    assertThat(saved.fields()).containsEntry("email", "janez@novak.si");
    assertThat(janez.getUpdatedBy()).isEqualTo(MARKO);
    assertThat(janez.getSearchText()).isEqualTo("janez novak janez@novak.si");
    assertThat(janez.getSortName()).isEqualTo("Novak");
  }

  @Test
  void r00701TheSavedContactKeepsItsAttributesAndTags() {
    Contact janez = stored(Map.of("firstName", "Janez"));
    when(contactQueries.findAttributes(GROUP_ID, janez.getId()))
        .thenReturn(List.of(new ContactAttributeRow("kompetenca", "spletnoProgramiranje")));

    ContactView saved =
        service.update(
            GROUP_ID,
            MARKO,
            janez.getId(),
            1,
            Map.of("firstName", "Janez", "city", "Maribor"),
            null,
            null);

    assertThat(saved.attributes())
        .containsExactly(new AttributeView("kompetenca", "spletnoProgramiranje"));
  }

  @Test
  void r00702InvalidEditedFieldsChangeNothing() {
    Contact janez = stored(Map.of("firstName", "Janez", "lastName", "Novak"));

    assertThatThrownBy(
            () ->
                service.update(
                    GROUP_ID,
                    MARKO,
                    janez.getId(),
                    1,
                    Map.of("firstName", " ", "lastName", ""),
                    null,
                    null))
        .isInstanceOf(ValidationException.class);
    assertThat(janez.getVersionNumber()).isEqualTo(1);
    assertThat(janez.getFields().lastName()).isEqualTo("Novak");
    verifyNoInteractions(versionWriter);
  }

  @Test
  void r00702UnknownFieldNamesOrNoVersionNumberAreRejected() {
    Contact janez = stored(Map.of("firstName", "Janez"));

    assertThatThrownBy(
            () ->
                service.update(
                    GROUP_ID, MARKO, janez.getId(), 1, Map.of("nickname", "J"), null, null))
        .isInstanceOf(ValidationException.class);
    assertThatThrownBy(
            () ->
                service.update(
                    GROUP_ID, MARKO, janez.getId(), null, Map.of("firstName", "J"), null, null))
        .isInstanceOf(ValidationException.class);
    verifyNoInteractions(versionWriter);
  }

  @Test
  void ac00705ViewerCannotEditAndNothingIsRead() {
    when(guard.require(GROUP_ID, PETRA, Permission.CHANGE_CONTACT))
        .thenThrow(new ForbiddenException());
    UUID contactId = UUID.randomUUID();

    assertThatThrownBy(
            () ->
                service.update(
                    GROUP_ID, PETRA, contactId, 1, Map.of("city", "Maribor"), null, null))
        .isInstanceOf(ForbiddenException.class);
    verifyNoInteractions(contacts, versionWriter);
  }

  @Test
  void ex00705ContactOfAnotherGroupIsNotFound() {
    UUID other = UUID.randomUUID();
    when(contacts.findForUpdate(GROUP_ID, other)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service.update(GROUP_ID, MARKO, other, 1, Map.of("firstName", "Maja"), null, null))
        .isInstanceOf(NotFoundException.class);
    verifyNoInteractions(versionWriter);
  }

  @Test
  void r00703SaveWithoutChangedItemIsRejectedAndCreatesNoVersion() {
    Contact janez = stored(Map.of("firstName", "Janez", "lastName", "Novak"));

    assertThatThrownBy(
            () ->
                service.update(
                    GROUP_ID,
                    MARKO,
                    janez.getId(),
                    1,
                    Map.of("firstName", "Janez ", "lastName", "Novak"),
                    null,
                    null))
        .isInstanceOf(ConflictException.class)
        .hasFieldOrPropertyWithValue("code", "contact.noChanges");
    assertThat(janez.getVersionNumber()).isEqualTo(1);
    assertThat(janez.getUpdatedBy()).isEqualTo(ANA);
    verifyNoInteractions(versionWriter);
  }

  @Test
  void br06StaleVersionIsRejectedWithTheNameOfWhoChangedIt() {
    Contact janez = stored(Map.of("firstName", "Janez", "lastName", "Novak"));
    janez.edit(
        ContactFields.ofNames(Map.of("firstName", "Janez", "lastName", "Novak", "city", "Maribor")),
        "",
        "",
        MARKO,
        NOW);
    User marko = User.create("uid-marko", "marko.kranjc@gmail.com", "Marko Kranjc", NOW);
    when(users.findById(MARKO)).thenReturn(Optional.of(marko));
    Map<String, String> anasForm =
        Map.of("firstName", "Janez", "lastName", "Novak", "jobTitle", "CEO");

    assertThatThrownBy(() -> service.update(GROUP_ID, ANA, janez.getId(), 1, anasForm, null, null))
        .isInstanceOf(ConflictException.class)
        .hasFieldOrPropertyWithValue("code", "version.conflict")
        .hasFieldOrPropertyWithValue("params", Map.of("changedBy", "Marko Kranjc"));
    assertThat(janez.getVersionNumber()).isEqualTo(2);
    assertThat(janez.getFields().jobTitle()).isNull();
    verifyNoInteractions(versionWriter);
  }

  @Test
  void ex00704VersionNumberHigherThanCurrentIsRejectedAsBr06() {
    Contact janez = stored(Map.of("firstName", "Janez"));
    User ana = User.create("uid-ana", "ana.kovac@example.si", "Ana Kovač", NOW);
    when(users.findById(ANA)).thenReturn(Optional.of(ana));

    assertThatThrownBy(
            () ->
                service.update(
                    GROUP_ID, MARKO, janez.getId(), 5, Map.of("firstName", "Jan"), null, null))
        .isInstanceOf(ConflictException.class)
        .hasFieldOrPropertyWithValue("code", "version.conflict");
    assertThat(janez.getVersionNumber()).isEqualTo(1);
    verifyNoInteractions(versionWriter);
  }

  @ParameterizedTest
  @CsvSource({"inRecycleBin, contact.inRecycleBin", "merged, contact.merged"})
  void ex00703ContactThatIsNotActiveCannotBeEdited(String state, String code) {
    Contact janez = stored(Map.of("firstName", "Janez"));
    ReflectionTestUtils.setField(janez, "state", state);

    assertThatThrownBy(
            () ->
                service.update(
                    GROUP_ID,
                    MARKO,
                    janez.getId(),
                    1,
                    Map.of("firstName", "Janez", "city", "Maribor"),
                    null,
                    null))
        .isInstanceOf(ConflictException.class)
        .hasFieldOrPropertyWithValue("code", code);
    assertThat(janez.getFields().city()).isNull();
    assertThat(janez.getVersionNumber()).isEqualTo(1);
    verifyNoInteractions(versionWriter);
  }

  private static List<AttributeView> attributesOf(String... keyValues) {
    List<AttributeView> list = new ArrayList<>();
    for (int i = 0; i < keyValues.length; i += 2) {
      list.add(new AttributeView(keyValues[i], keyValues[i + 1]));
    }
    return list;
  }

  private List<Attribute> storedAttributes() {
    ArgumentCaptor<Attribute> stored = ArgumentCaptor.forClass(Attribute.class);
    verify(attributes, atLeast(0)).add(eq(GROUP_ID), stored.capture());
    return stored.getAllValues();
  }

  private void hasAttributes(Contact contact, String... keyValues) {
    List<ContactAttributeRow> rows = new ArrayList<>();
    for (int i = 0; i < keyValues.length; i += 2) {
      rows.add(new ContactAttributeRow(keyValues[i], keyValues[i + 1]));
    }
    when(contactQueries.findAttributes(GROUP_ID, contact.getId())).thenReturn(rows);
  }

  @Test
  void r00801CreateStoresTheTrimmedAttributesInVersionOne() {
    service.create(
        GROUP_ID, MARKO, Map.of("firstName", "Janez"), attributesOf(" kompetenca ", "java "), null);

    assertThat(storedAttributes()).hasSize(1);
    ArgumentCaptor<ContactContent> after = ArgumentCaptor.forClass(ContactContent.class);
    verify(versionWriter)
        .record(
            any(Contact.class),
            eq(ContactVersion.CREATED),
            eq(ContactContent.EMPTY),
            after.capture());
    assertThat(after.getValue().attributes())
        .containsExactly(new AttributeView("kompetenca", "java"));
  }

  @Test
  void r00802InvalidAttributesStoreNoContact() {
    assertThatThrownBy(
            () ->
                service.create(
                    GROUP_ID, MARKO, Map.of("firstName", "Janez"), attributesOf("   ", "v"), null))
        .isInstanceOf(ValidationException.class);
    verifyNoInteractions(contacts, attributes, versionWriter);
  }

  @Test
  void r00801EditStoresTheAttributesAnewAndRecordsBeforeAndAfter() {
    Contact janez = stored(Map.of("firstName", "Janez"));
    hasAttributes(janez, "kompetenca", "spletnoProgramiranje");

    service.update(
        GROUP_ID,
        MARKO,
        janez.getId(),
        1,
        Map.of("firstName", "Janez"),
        attributesOf("kompetenca", "mobilnoProgramiranje"),
        null);

    verify(attributes).discardOfContact(GROUP_ID, janez.getId());
    assertThat(storedAttributes()).hasSize(1);
    verify(versionWriter)
        .record(
            janez,
            ContactVersion.EDITED,
            new ContactContent(
                janez.getFields(), attributesOf("kompetenca", "spletnoProgramiranje"), List.of()),
            new ContactContent(
                janez.getFields(), attributesOf("kompetenca", "mobilnoProgramiranje"), List.of()));
    assertThat(janez.getVersionNumber()).isEqualTo(2);
    assertThat(janez.getUpdatedBy()).isEqualTo(MARKO);
  }

  @Test
  void r00703SameAttributesInAnotherOrderAreNoChange() {
    Contact janez = stored(Map.of("firstName", "Janez"));
    hasAttributes(janez, "kompetenca", "java", "opis", "x");

    assertThatThrownBy(
            () ->
                service.update(
                    GROUP_ID,
                    MARKO,
                    janez.getId(),
                    1,
                    Map.of("firstName", "Janez"),
                    attributesOf("opis", "x", " kompetenca", "java"),
                    null))
        .isInstanceOf(ConflictException.class)
        .hasFieldOrPropertyWithValue("code", "contact.noChanges");
    verifyNoInteractions(attributes, versionWriter);
  }

  @Test
  void r00801WithoutAttributesInTheRequestTheStoredOnesStay() {
    Contact janez = stored(Map.of("firstName", "Janez"));
    hasAttributes(janez, "kompetenca", "java");

    service.update(
        GROUP_ID,
        MARKO,
        janez.getId(),
        1,
        Map.of("firstName", "Janez", "city", "Maribor"),
        null,
        null);

    verifyNoInteractions(attributes);
    ArgumentCaptor<ContactContent> after = ArgumentCaptor.forClass(ContactContent.class);
    verify(versionWriter).record(eq(janez), eq(ContactVersion.EDITED), any(), after.capture());
    assertThat(after.getValue().attributes())
        .containsExactly(new AttributeView("kompetenca", "java"));
  }

  @Test
  void r00802InvalidEditedAttributesChangeNothing() {
    Contact janez = stored(Map.of("firstName", "Janez"));

    assertThatThrownBy(
            () ->
                service.update(
                    GROUP_ID,
                    MARKO,
                    janez.getId(),
                    1,
                    Map.of("firstName", "Janez"),
                    attributesOf("kompetenca", "a", "Kompetenca", "b"),
                    null))
        .isInstanceOf(ValidationException.class);
    assertThat(janez.getVersionNumber()).isEqualTo(1);
    verifyNoInteractions(attributes, versionWriter);
  }

  @Test
  void ex00807StaleVersionStoresNoAttributes() {
    Contact janez = stored(Map.of("firstName", "Janez"));
    User ana = User.create("uid-ana", "ana.kovac@example.si", "Ana Kovač", NOW);
    when(users.findById(ANA)).thenReturn(Optional.of(ana));

    assertThatThrownBy(
            () ->
                service.update(
                    GROUP_ID,
                    MARKO,
                    janez.getId(),
                    2,
                    Map.of("firstName", "Janez"),
                    attributesOf("kompetenca", "java"),
                    null))
        .isInstanceOf(ConflictException.class)
        .hasFieldOrPropertyWithValue("code", "version.conflict");
    verifyNoInteractions(attributes, versionWriter);
  }

  private Tag hasTags(Contact contact, String... names) {
    List<ContactTagRow> rows = new ArrayList<>();
    Tag last = null;
    for (String name : names) {
      last = Tag.create(GROUP_ID, name);
      when(tags.findInGroup(GROUP_ID, name)).thenReturn(Optional.of(last));
      rows.add(new ContactTagRow(contact.getId(), last.getId(), name));
    }
    when(contactQueries.findTags(GROUP_ID, List.of(contact.getId()))).thenReturn(rows);
    return last;
  }

  private ContactContent recorded(String kind) {
    ArgumentCaptor<ContactContent> after = ArgumentCaptor.forClass(ContactContent.class);
    verify(versionWriter).record(any(Contact.class), eq(kind), any(), after.capture());
    return after.getValue();
  }

  @Test
  void r00901CreatePutsTheTagsOnInVersionOne() {
    service.create(GROUP_ID, MARKO, Map.of("firstName", "Janez"), null, List.of(" vip "));

    verify(tags).add(eq(GROUP_ID), any(Tag.class));
    verify(contactTags).add(eq(GROUP_ID), any(ContactTag.class));
    assertThat(recorded(ContactVersion.CREATED).tags()).containsExactly("vip");
  }

  @Test
  void r00905InvalidTagsStoreNoContact() {
    assertThatThrownBy(
            () ->
                service.create(
                    GROUP_ID, MARKO, Map.of("firstName", "Janez"), null, List.of("vip, 2025")))
        .isInstanceOf(ValidationException.class);
    verifyNoInteractions(contacts, tags, contactTags, versionWriter);
  }

  @Test
  void r00901EditListsAddedAndRemovedTagsInOneVersion() {
    Contact janez = stored(Map.of("firstName", "Janez"));
    hasTags(janez, "udelezenec2025", "neObvescaj");

    service.update(
        GROUP_ID,
        MARKO,
        janez.getId(),
        1,
        Map.of("firstName", "Janez"),
        null,
        List.of("udelezenec2025", "vip"));

    verify(contactTags).discardOfContact(eq(GROUP_ID), eq(janez.getId()), any());
    verify(contactTags).add(eq(GROUP_ID), any(ContactTag.class));
    verify(versionWriter)
        .record(
            janez,
            ContactVersion.EDITED,
            new ContactContent(
                janez.getFields(), List.of(), List.of("neObvescaj", "udelezenec2025")),
            new ContactContent(janez.getFields(), List.of(), List.of("udelezenec2025", "vip")));
    assertThat(janez.getVersionNumber()).isEqualTo(2);
  }

  @Test
  void r00903StoredTagInOtherCaseIsNoChange() {
    Contact janez = stored(Map.of("firstName", "Janez"));
    Tag vip = hasTags(janez, "vip");
    when(tags.findInGroup(GROUP_ID, "VIP")).thenReturn(Optional.of(vip));

    assertThatThrownBy(
            () ->
                service.update(
                    GROUP_ID,
                    MARKO,
                    janez.getId(),
                    1,
                    Map.of("firstName", "Janez"),
                    null,
                    List.of("VIP")))
        .isInstanceOf(ConflictException.class)
        .hasFieldOrPropertyWithValue("code", "contact.noChanges");
    verifyNoInteractions(contactTags, versionWriter);
  }

  @Test
  void r00901WithoutTagsInTheRequestTheStoredOnesStay() {
    Contact janez = stored(Map.of("firstName", "Janez"));
    hasTags(janez, "vip");

    service.update(
        GROUP_ID,
        MARKO,
        janez.getId(),
        1,
        Map.of("firstName", "Janez", "city", "Maribor"),
        null,
        null);

    verifyNoInteractions(tags, contactTags);
    assertThat(recorded(ContactVersion.EDITED).tags()).containsExactly("vip");
  }

  @Test
  void r00905InvalidEditedTagsChangeNothing() {
    Contact janez = stored(Map.of("firstName", "Janez"));

    assertThatThrownBy(
            () ->
                service.update(
                    GROUP_ID,
                    MARKO,
                    janez.getId(),
                    1,
                    Map.of("firstName", "Janez"),
                    null,
                    List.of("t".repeat(51))))
        .isInstanceOf(ValidationException.class);
    assertThat(janez.getVersionNumber()).isEqualTo(1);
    verifyNoInteractions(tags, contactTags, versionWriter);
  }

  @Test
  void ex00907StaleVersionStoresNoTags() {
    Contact janez = stored(Map.of("firstName", "Janez"));
    User ana = User.create("uid-ana", "ana.kovac@example.si", "Ana Kovač", NOW);
    when(users.findById(ANA)).thenReturn(Optional.of(ana));

    assertThatThrownBy(
            () ->
                service.update(
                    GROUP_ID,
                    MARKO,
                    janez.getId(),
                    2,
                    Map.of("firstName", "Janez"),
                    null,
                    List.of("vip")))
        .isInstanceOf(ConflictException.class)
        .hasFieldOrPropertyWithValue("code", "version.conflict");
    verifyNoInteractions(tags, contactTags, versionWriter);
  }

  private Contact stored(Map<String, String> fields) {
    Contact contact = Contact.create(GROUP_ID, ContactFields.ofNames(fields), "", "", ANA, NOW);
    when(contacts.findForUpdate(GROUP_ID, contact.getId())).thenReturn(Optional.of(contact));
    return contact;
  }

  private static Contact contact(Map<String, String> fields) {
    return Contact.create(GROUP_ID, ContactFields.ofNames(fields), "", "", MARKO, NOW);
  }
}
