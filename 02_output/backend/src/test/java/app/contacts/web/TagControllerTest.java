package app.contacts.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.contacts.auth.CurrentUser;
import app.contacts.service.TagService;
import app.contacts.service.TagView;
import app.contacts.service.UserView;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TagControllerTest {

  private static final UUID MARKO_ID = UUID.randomUUID();
  private static final UUID GROUP_ID = UUID.randomUUID();
  private static final CurrentUser MARKO =
      new CurrentUser(new UserView(MARKO_ID, "marko.kranjc@gmail.com", "Marko Kranjc", "sl"), true);

  private final TagService tags = mock(TagService.class);
  private final TagController controller = new TagController(tags);

  @Test
  void r00902GetTagsAnswersTheSuggestedTags() {
    TagView udelezenec = new TagView(UUID.randomUUID(), "udelezenec2025");
    when(tags.suggest(GROUP_ID, MARKO_ID, "udel")).thenReturn(List.of(udelezenec));

    assertThat(controller.suggest(MARKO, GROUP_ID, "udel")).containsExactly(udelezenec);
  }
}
