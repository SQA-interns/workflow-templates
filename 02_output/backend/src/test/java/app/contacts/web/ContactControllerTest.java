package app.contacts.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.contacts.auth.CurrentUser;
import app.contacts.service.ContactPageView;
import app.contacts.service.ContactQuery;
import app.contacts.service.ContactQueryService;
import app.contacts.service.ContactRefView;
import app.contacts.service.ContactService;
import app.contacts.service.ContactView;
import app.contacts.service.UserView;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ContactControllerTest {

  private static final Instant NOW = Instant.parse("2026-03-05T10:00:00Z");
  private static final UUID MARKO_ID = UUID.randomUUID();
  private static final UUID GROUP_ID = UUID.randomUUID();
  private static final UUID CONTACT_ID = UUID.randomUUID();
  private static final CurrentUser MARKO =
      new CurrentUser(new UserView(MARKO_ID, "marko.kranjc@gmail.com", "Marko Kranjc", "sl"), true);

  private final ContactService contacts = mock(ContactService.class);
  private final ContactQueryService contactQueries = mock(ContactQueryService.class);
  private final ContactController controller = new ContactController(contacts, contactQueries);

  @Test
  void r00601PostContactsAnswers201WithTheNewContact() {
    SequencedMap<String, String> fields = new LinkedHashMap<>();
    fields.put("firstName", "Janez");
    fields.put("lastName", "Novak");
    fields.put("email", null);
    Map<String, String> sent = Map.of("firstName", "Janez", "lastName", "Novak");
    when(contacts.create(GROUP_ID, MARKO_ID, sent))
        .thenReturn(new ContactView(CONTACT_ID, 1, "active", fields, NOW, NOW));

    ResponseEntity<ContactResponse> created =
        controller.create(MARKO, GROUP_ID, new ContactWriteRequest(sent));

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    ContactResponse body = created.getBody();
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
  void r00606GetEmailMatchesListsTheMatchingContacts() {
    when(contacts.findByEmail(GROUP_ID, MARKO_ID, "Janez.Novak@example.si"))
        .thenReturn(List.of(new ContactRefView(CONTACT_ID, "Janez Novak")));

    assertThat(controller.emailMatches(MARKO, GROUP_ID, "Janez.Novak@example.si"))
        .containsExactly(new ContactRefResponse(CONTACT_ID, "Janez Novak"));
  }

  @Test
  void r01401GetContactsAnswersOnePageOfTheQuery() {
    ContactPageView page = new ContactPageView(List.of(), 0, 2, 50);
    when(contactQueries.page(GROUP_ID, MARKO_ID, new ContactQuery("novak", "company", "desc", 2)))
        .thenReturn(page);

    assertThat(controller.list(MARKO, GROUP_ID, "novak", "company", "desc", 2)).isSameAs(page);
  }
}
