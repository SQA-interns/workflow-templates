package app.contacts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.contacts.domain.Contact;
import app.contacts.domain.ContactFields;
import app.contacts.domain.ContactTag;
import app.contacts.domain.Role;
import app.contacts.domain.Tag;
import app.contacts.repository.ContactTagRepository;
import app.contacts.repository.TagRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TagServiceTest {

  private static final Instant NOW = Instant.parse("2026-03-05T10:00:00Z");
  private static final UUID GROUP_ID = UUID.randomUUID();
  private static final UUID PETRA = UUID.randomUUID();

  private final TagRepository tags = mock(TagRepository.class);
  private final ContactTagRepository contactTags = mock(ContactTagRepository.class);
  private final AccessGuard guard = mock(AccessGuard.class);
  private final TagService service = new TagService(tags, contactTags, guard);

  private final Contact janez =
      Contact.create(
          GROUP_ID,
          ContactFields.ofNames(Map.of("firstName", "Janez")),
          "",
          "",
          UUID.randomUUID(),
          NOW);

  @BeforeEach
  void noTagsYet() {
    when(tags.findInGroup(eq(GROUP_ID), anyString())).thenReturn(Optional.empty());
    when(tags.add(eq(GROUP_ID), any(Tag.class))).thenAnswer(call -> call.getArgument(1));
  }

  private Tag existing(String name) {
    Tag tag = Tag.create(GROUP_ID, name);
    when(tags.findInGroup(GROUP_ID, name)).thenReturn(Optional.of(tag));
    return tag;
  }

  @Test
  void r00903NameOfAnExistingTagInOtherCaseUsesTheExistingTag() {
    Tag udelezenec = existing("udelezenec2025");
    when(tags.findInGroup(GROUP_ID, "Udelezenec2025")).thenReturn(Optional.of(udelezenec));

    List<Tag> resolved = service.resolve(GROUP_ID, List.of("Udelezenec2025"));

    assertThat(resolved).containsExactly(udelezenec);
    verify(tags, never()).add(any(), any());
  }

  @Test
  void r00904NewNameCreatesATagOfTheGroupAsEntered() {
    List<Tag> resolved = service.resolve(GROUP_ID, List.of("udeležba"));

    ArgumentCaptor<Tag> created = ArgumentCaptor.forClass(Tag.class);
    verify(tags).add(eq(GROUP_ID), created.capture());
    assertThat(created.getValue().getName()).isEqualTo("udeležba");
    assertThat(created.getValue().getGroupId()).isEqualTo(GROUP_ID);
    assertThat(resolved).containsExactly(created.getValue());
  }

  @Test
  void r00906NamesOfTheSameTagGiveItOnce() {
    Tag vip = existing("vip");
    when(tags.findInGroup(GROUP_ID, "VIP")).thenReturn(Optional.of(vip));

    assertThat(service.resolve(GROUP_ID, List.of("vip", "VIP"))).containsExactly(vip);
  }

  @Test
  void r00901PutsOnlyNewTagsOnAndTakesOnlyRemovedTagsOff() {
    Tag udelezenec = existing("udelezenec2025");
    Tag neObvescaj = existing("neObvescaj");
    Tag vip = existing("vip");

    service.retag(janez, Set.of(udelezenec.getId(), neObvescaj.getId()), List.of(udelezenec, vip));

    verify(contactTags).discardOfContact(GROUP_ID, janez.getId(), Set.of(neObvescaj.getId()));
    ArgumentCaptor<ContactTag> put = ArgumentCaptor.forClass(ContactTag.class);
    verify(contactTags).add(eq(GROUP_ID), put.capture());
    assertThat(put.getAllValues()).hasSize(1);
  }

  @Test
  void r00901NothingRemovedTakesNothingOff() {
    Tag vip = existing("vip");

    service.retag(janez, Set.of(), List.of(vip));

    verify(contactTags, never()).discardOfContact(any(), any(), any());
  }

  @Test
  void r00902AnyMemberGetsTheTagsContainingTheTrimmedText() {
    Tag udelezenec = existing("udelezenec2025");
    when(guard.require(GROUP_ID, PETRA, Permission.READ)).thenReturn(Role.VIEWER);
    when(tags.suggest(GROUP_ID, "udel")).thenReturn(List.of(udelezenec));

    assertThat(service.suggest(GROUP_ID, PETRA, " udel "))
        .containsExactly(new TagView(udelezenec.getId(), "udelezenec2025"));
  }

  @Test
  void r00902WithoutTextEveryTagOnAnActiveContactIsSuggested() {
    service.suggest(GROUP_ID, PETRA, null);

    verify(tags).suggest(GROUP_ID, "");
  }

  @Test
  void r00902TextOverFiftyCharactersIsInvalid() {
    assertThatThrownBy(() -> service.suggest(GROUP_ID, PETRA, "t".repeat(51)))
        .isInstanceOf(ValidationException.class)
        .extracting("problems")
        .asInstanceOf(InstanceOfAssertFactories.LIST)
        .containsExactly(new FieldProblem("q", "field.tooLong", Map.of("field", "q", "max", 50)));
    verify(tags, never()).suggest(any(), any());
  }

  @Test
  void br02NonMemberGetsNotFoundAndNoTags() {
    when(guard.require(GROUP_ID, PETRA, Permission.READ)).thenThrow(new NotFoundException());

    assertThatThrownBy(() -> service.suggest(GROUP_ID, PETRA, "udel"))
        .isInstanceOf(NotFoundException.class);
    verify(tags, never()).suggest(any(), any());
  }
}
