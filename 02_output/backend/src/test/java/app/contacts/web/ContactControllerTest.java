package app.contacts.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.contacts.auth.CurrentUser;
import app.contacts.service.AttributeView;
import app.contacts.service.ContactPageView;
import app.contacts.service.ContactQuery;
import app.contacts.service.ContactQueryService;
import app.contacts.service.ContactRefView;
import app.contacts.service.ContactService;
import app.contacts.service.ContactView;
import app.contacts.service.MergedFromView;
import app.contacts.service.TagView;
import app.contacts.service.UserView;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ContactControllerTest {

  private static final Instant NOW = Instant.parse("2026-03-05T10:00:00Z");
  private static final UUID MARKO_ID = UUID.randomUUID();
  private static final UUID GROUP_ID = UUID.randomUUID();
  private static final UUID CONTACT_ID = UUID.randomUUID();
  private static final UUID TAG_ID = UUID.randomUUID();
  private static final List<AttributeView> KOMPETENCA =
      List.of(new AttributeView("kompetenca", "java"));
  private static final List<String> VIP = List.of("vip");
  private static final CurrentUser MARKO =
      new CurrentUser(new UserView(MARKO_ID, "marko.kranjc@gmail.com", "Marko Kranjc", "sl"), true);

  private final ContactService contacts = mock(ContactService.class);
  private final ContactQueryService contactQueries = mock(ContactQueryService.class);
  private final ContactController controller = new ContactController(contacts, contactQueries);

  @Test
  void r00901PostContactsAnswers201WithTheNewContactItsAttributesAndTags() {
    SequencedMap<String, String> fields = new LinkedHashMap<>();
    fields.put("firstName", "Janez");
    fields.put("lastName", "Novak");
    fields.put("email", null);
    Map<String, String> sent = Map.of("firstName", "Janez", "lastName", "Novak");
    when(contacts.create(GROUP_ID, MARKO_ID, sent, KOMPETENCA, VIP))
        .thenReturn(
            new ContactView(CONTACT_ID, 1, "active", fields, List.of(), List.of(), NOW, NOW));

    ContactResponse body =
        controller.create(MARKO, GROUP_ID, new ContactWriteRequest(sent, KOMPETENCA, VIP));

    assertThat(ResponseStatuses.of(ContactController.class, "create"))
        .isEqualTo(HttpStatus.CREATED);
    assertThat(body.id()).isEqualTo(CONTACT_ID);
    assertThat(body.versionNumber()).isEqualTo(1);
    assertThat(body.state()).isEqualTo("active");
    assertThat(body.fields()).containsExactlyEntriesOf(fields);
    assertThat(body.attributes()).isEmpty();
    assertThat(body.tags()).isEmpty();
    assertThat(body.mergedFrom()).isEmpty();
    assertThat(body.mergedInto()).isNull();
    assertThat(body.createdAt()).isEqualTo(NOW);
    assertThat(body.updatedAt()).isEqualTo(NOW);
  }

  @Test
  void r00901PutContactSavesFieldsAttributesAndTagsWithTheVersionNumberRead() {
    SequencedMap<String, String> fields = new LinkedHashMap<>();
    fields.put("firstName", "Janez");
    fields.put("city", "Maribor");
    Map<String, String> sent = Map.of("firstName", "Janez", "city", "Maribor");
    when(contacts.update(GROUP_ID, MARKO_ID, CONTACT_ID, 1, sent, KOMPETENCA, VIP))
        .thenReturn(
            new ContactView(CONTACT_ID, 2, "active", fields, List.of(), List.of(), NOW, NOW));

    ContactResponse saved =
        controller.update(
            MARKO, GROUP_ID, CONTACT_ID, new ContactUpdateRequest(sent, KOMPETENCA, VIP, 1));

    assertThat(saved.versionNumber()).isEqualTo(2);
    assertThat(saved.fields()).containsExactlyEntriesOf(fields);
  }

  @Test
  void r00606GetEmailMatchesListsTheMatchingContacts() {
    when(contacts.findByEmail(GROUP_ID, MARKO_ID, "Janez.Novak@example.si", null))
        .thenReturn(List.of(new ContactRefView(CONTACT_ID, "Janez Novak")));

    assertThat(controller.emailMatches(MARKO, GROUP_ID, "Janez.Novak@example.si", null))
        .containsExactly(new ContactRefResponse(CONTACT_ID, "Janez Novak"));
  }

  @Test
  void r00606GetEmailMatchesPassesTheContactBeingEdited() {
    UUID edited = UUID.randomUUID();
    when(contacts.findByEmail(GROUP_ID, MARKO_ID, "Janez.Novak@example.si", edited))
        .thenReturn(List.of());

    assertThat(controller.emailMatches(MARKO, GROUP_ID, "Janez.Novak@example.si", edited))
        .isEmpty();
  }

  @Test
  void r01401R01505GetContactsAnswersOnePageOfTheQueryWithItsFilters() {
    ContactPageView page = new ContactPageView(List.of(), 0, 2, 50);
    UUID ne = UUID.randomUUID();
    ContactQuery.Filters filters =
        new ContactQuery.Filters(List.of(TAG_ID), List.of(ne), "kompetenca", "splet", "Šola", "SI");
    when(contactQueries.page(
            GROUP_ID, MARKO_ID, new ContactQuery("novak", "company", "desc", 2, filters)))
        .thenReturn(page);

    assertThat(
            controller.list(
                MARKO,
                GROUP_ID,
                "novak",
                "company",
                "desc",
                2,
                List.of(TAG_ID),
                List.of(ne),
                "kompetenca",
                "splet",
                "Šola",
                "SI"))
        .isSameAs(page);
  }

  @Test
  void r01805GetContactAnswersItsMergeLinks() {
    UUID mergeId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    MergedFromView from =
        new MergedFromView(mergeId, new ContactRefView(otherId, "J. Novak"), false);
    when(contacts.get(GROUP_ID, MARKO_ID, CONTACT_ID))
        .thenReturn(
            new ContactView(
                CONTACT_ID,
                4,
                "active",
                new LinkedHashMap<>(),
                List.of(),
                List.of(),
                NOW,
                NOW,
                List.of(from),
                null));
    when(contacts.get(GROUP_ID, MARKO_ID, otherId))
        .thenReturn(
            new ContactView(
                otherId,
                2,
                "merged",
                new LinkedHashMap<>(),
                List.of(),
                List.of(),
                NOW,
                NOW,
                List.of(),
                new ContactRefView(CONTACT_ID, "Janez Novak")));

    assertThat(controller.get(MARKO, GROUP_ID, CONTACT_ID).mergedFrom()).containsExactly(from);
    assertThat(controller.get(MARKO, GROUP_ID, otherId).mergedInto())
        .isEqualTo(new ContactRefResponse(CONTACT_ID, "Janez Novak"));
  }

  @Test
  void r01406GetContactAnswersTheContact() {
    SequencedMap<String, String> fields = new LinkedHashMap<>();
    fields.put("firstName", "Janez");
    when(contacts.get(GROUP_ID, MARKO_ID, CONTACT_ID))
        .thenReturn(
            new ContactView(
                CONTACT_ID,
                2,
                "active",
                fields,
                List.of(new AttributeView("kompetenca", "spletnoProgramiranje")),
                List.of(new TagView(TAG_ID, "udelezenec2025")),
                NOW,
                NOW));

    ContactResponse contact = controller.get(MARKO, GROUP_ID, CONTACT_ID);

    assertThat(contact.attributes())
        .containsExactly(new AttributeView("kompetenca", "spletnoProgramiranje"));
    assertThat(contact.tags()).containsExactly(new TagView(TAG_ID, "udelezenec2025"));
    assertThat(contact.id()).isEqualTo(CONTACT_ID);
    assertThat(contact.versionNumber()).isEqualTo(2);
    assertThat(contact.fields()).containsEntry("firstName", "Janez");
  }
}
