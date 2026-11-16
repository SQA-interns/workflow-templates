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
import java.util.ArrayList;
import java.util.List;
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

  private static ContactContent content(ContactFields fields, String... keyValues) {
    List<AttributeView> attributes = new ArrayList<>();
    for (int i = 0; i < keyValues.length; i += 2) {
      attributes.add(new AttributeView(keyValues[i], keyValues[i + 1]));
    }
    return new ContactContent(fields, attributes);
  }

  private ContactVersion written(Contact contact, ContactFields before) {
    return written(contact, content(before), content(contact.getFields()));
  }

  private ContactVersion written(Contact contact, ContactContent before, ContactContent after) {
    writer.record(contact, ContactVersion.CREATED, before, after);
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

    writer.record(contact, ContactVersion.EDITED, content(before), content(after));

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

  @Test
  void r00801AddedAttributeIsListedAsAttributeKeyWithoutOldValue() {
    Contact contact = Contact.create(GROUP_ID, JANEZ, "janez", "Novak", MARKO, NOW);

    JsonNode changes =
        json.readTree(
            written(contact, content(JANEZ), content(JANEZ, "kompetenca", "spletnoProgramiranje"))
                .getChanges());

    assertThat(changes.size()).isEqualTo(1);
    assertThat(changes.get(0).path("item").asString()).isEqualTo("attribute:kompetenca");
    assertThat(changes.get(0).path("oldValue").isNull()).isTrue();
    assertThat(changes.get(0).path("newValue").asString()).isEqualTo("spletnoProgramiranje");
  }

  @Test
  void r00801ChangedValueIsListedWithOldAndNewValue() {
    Contact contact = Contact.create(GROUP_ID, JANEZ, "janez", "Novak", MARKO, NOW);

    JsonNode changes =
        json.readTree(
            written(
                    contact,
                    content(JANEZ, "kompetenca", "spletnoProgramiranje", "opis", "x"),
                    content(JANEZ, "opis", "x", "kompetenca", "mobilnoProgramiranje"))
                .getChanges());

    assertThat(changes.size()).isEqualTo(1);
    assertThat(changes.get(0).path("item").asString()).isEqualTo("attribute:kompetenca");
    assertThat(changes.get(0).path("oldValue").asString()).isEqualTo("spletnoProgramiranje");
    assertThat(changes.get(0).path("newValue").asString()).isEqualTo("mobilnoProgramiranje");
  }

  @Test
  void r00801RemovedAttributeIsListedWithoutNewValue() {
    Contact contact = Contact.create(GROUP_ID, JANEZ, "janez", "Novak", MARKO, NOW);

    JsonNode changes =
        json.readTree(
            written(contact, content(JANEZ, "kompetenca", "mobilnoProgramiranje"), content(JANEZ))
                .getChanges());

    assertThat(changes.get(0).path("item").asString()).isEqualTo("attribute:kompetenca");
    assertThat(changes.get(0).path("oldValue").asString()).isEqualTo("mobilnoProgramiranje");
    assertThat(changes.get(0).path("newValue").isNull()).isTrue();
  }

  @Test
  void ex00806FieldAndAttributeChangedInOneSaveAreItemsOfOneVersion() {
    Contact contact = Contact.create(GROUP_ID, JANEZ, "janez", "Novak", MARKO, NOW);
    ContactFields before = ContactFields.ofNames(Map.of("firstName", "Janez", "lastName", "Novak"));

    JsonNode changes =
        json.readTree(
            written(contact, content(before), content(JANEZ, "kompetenca", "java")).getChanges());

    assertThat(changes.findValuesAsString("item")).containsExactly("email", "attribute:kompetenca");
  }

  @Test
  void r00806SnapshotHoldsTheAttributesByKeyAtoZ() {
    Contact contact = Contact.create(GROUP_ID, JANEZ, "janez", "Novak", MARKO, NOW);

    JsonNode snapshot =
        json.readTree(
            written(contact, content(JANEZ), content(JANEZ, "drža", "3", "Čas", "2", "cena", "1"))
                .getSnapshot());

    assertThat(snapshot.path("attributes").findValuesAsString("key"))
        .containsExactly("cena", "Čas", "drža");
    assertThat(snapshot.path("attributes").get(0).path("value").asString()).isEqualTo("1");
  }
}
