package app.contacts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import app.contacts.domain.Contact;
import app.contacts.domain.ContactFields;
import app.contacts.domain.ContactVersion;
import app.contacts.repository.ContactVersionRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class VersionWriterTest {

  private static final Instant NOW = Instant.parse("2026-03-05T10:00:00Z");
  private static final UUID GROUP_ID = UUID.randomUUID();
  private static final UUID MARKO = UUID.randomUUID();
  private static final UUID ANA = UUID.randomUUID();
  private static final Instant LATER = Instant.parse("2026-03-06T09:00:00Z");

  private final JsonMapper json = JsonMapper.builder().build();
  private final ContactVersionRepository versions = mock(ContactVersionRepository.class);
  private final VersionWriter writer = new VersionWriter(versions);

  private static final ContactFields JANEZ =
      ContactFields.ofNames(
          Map.of("firstName", "Janez", "lastName", "Novak", "email", "janez.novak@example.si"));

  private ContactVersion written(Contact contact, ContactFields before) {
    writer.record(contact, ContactVersion.CREATED, before);
    ArgumentCaptor<ContactVersion> version = ArgumentCaptor.forClass(ContactVersion.class);
    verify(versions).add(eq(GROUP_ID), version.capture());
    return version.getValue();
  }

  @Test
  void r00601VersionOneCreatedListsEveryFilledFieldAsChanged() {
    Contact contact = Contact.create(GROUP_ID, JANEZ, "janez", "Novak", MARKO, NOW);

    ContactVersion version = written(contact, ContactFields.EMPTY);

    assertThat(version.getNumber()).isEqualTo(1);
    assertThat(version.getKind()).isEqualTo("created");
    JsonNode changes = json.readTree(version.getChanges());
    assertThat(changes.size()).isEqualTo(3);
    assertThat(changes.get(0).path("item").asString()).isEqualTo("firstName");
    assertThat(changes.get(0).path("oldValue").isNull()).isTrue();
    assertThat(changes.get(0).path("newValue").asString()).isEqualTo("Janez");
    assertThat(changes.get(2).path("item").asString()).isEqualTo("email");
  }

  @Test
  void adr07SnapshotHoldsTheFullContentAfterTheChange() {
    Contact contact = Contact.create(GROUP_ID, JANEZ, "janez", "Novak", MARKO, NOW);

    JsonNode snapshot = json.readTree(written(contact, ContactFields.EMPTY).getSnapshot());

    assertThat(snapshot.path("fields").propertyNames())
        .containsExactlyElementsOf(ContactFields.NAMES);
    assertThat(snapshot.path("fields").path("lastName").asString()).isEqualTo("Novak");
    assertThat(snapshot.path("fields").path("city").isNull()).isTrue();
    assertThat(snapshot.path("attributes").isEmpty()).isTrue();
    assertThat(snapshot.path("tags").isEmpty()).isTrue();
  }

  @Test
  void br05UnchangedFieldsAreNotListed() {
    Contact contact = Contact.create(GROUP_ID, JANEZ, "janez", "Novak", MARKO, NOW);
    ContactFields before = ContactFields.ofNames(Map.of("firstName", "Janez", "lastName", "Novak"));

    JsonNode changes = json.readTree(written(contact, before).getChanges());

    assertThat(changes.size()).isEqualTo(1);
    assertThat(changes.get(0).path("item").asString()).isEqualTo("email");
  }

  @Test
  void ex00701TwoFieldsChangedInOneSaveGiveOneVersionEditedWithBothItems() {
    Contact contact = Contact.create(GROUP_ID, JANEZ, "janez", "Novak", ANA, NOW);
    ContactFields before = contact.getFields();
    ContactFields after =
        ContactFields.ofNames(
            Map.of(
                "firstName", "Janez",
                "lastName", "Novak",
                "email", "janez.novak@example.si",
                "city", "Maribor",
                "jobTitle", "CEO"));
    contact.edit(after, "janez novak", "Novak", MARKO, LATER);

    writer.record(contact, ContactVersion.EDITED, before);

    ArgumentCaptor<ContactVersion> version = ArgumentCaptor.forClass(ContactVersion.class);
    verify(versions).add(eq(GROUP_ID), version.capture());
    assertThat(version.getValue().getNumber()).isEqualTo(2);
    assertThat(version.getValue().getKind()).isEqualTo("edited");
    JsonNode changes = json.readTree(version.getValue().getChanges());
    assertThat(changes.size()).isEqualTo(2);
    assertThat(changes.findValuesAsString("item")).containsExactly("jobTitle", "city");
    assertThat(changes.get(1).path("oldValue").isNull()).isTrue();
    assertThat(changes.get(1).path("newValue").asString()).isEqualTo("Maribor");
  }
}
