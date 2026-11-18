package app.contacts.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.contacts.auth.CurrentUser;
import app.contacts.service.MergeCommand;
import app.contacts.service.MergeService;
import app.contacts.service.MergeView;
import app.contacts.service.UserView;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class MergeControllerTest {

  private static final UUID MARKO_ID = UUID.randomUUID();
  private static final UUID GROUP_ID = UUID.randomUUID();
  private static final UUID JANEZ_ID = UUID.randomUUID();
  private static final UUID OTHER_ID = UUID.randomUUID();
  private static final CurrentUser MARKO =
      new CurrentUser(new UserView(MARKO_ID, "marko.kranjc@gmail.com", "Marko Kranjc", "sl"), true);

  private final MergeService merges = mock(MergeService.class);
  private final MergeController controller = new MergeController(merges);

  @Test
  void r01804PostMergeAnswers201WithTheMerge() {
    MergeView merge =
        new MergeView(
            UUID.randomUUID(), JANEZ_ID, OTHER_ID, Instant.parse("2026-03-05T09:00:00Z"), false);
    Map<String, String> keepOtherPhone = Map.of("phone", "other");
    when(merges.merge(
            GROUP_ID,
            MARKO_ID,
            new MergeCommand(JANEZ_ID, OTHER_ID, 3, 1, keepOtherPhone, Map.of())))
        .thenReturn(merge);

    MergeView response =
        controller.merge(
            MARKO, GROUP_ID, new MergeRequest(JANEZ_ID, OTHER_ID, 3, 1, keepOtherPhone, Map.of()));

    assertThat(ResponseStatuses.of(MergeController.class, "merge")).isEqualTo(HttpStatus.CREATED);
    assertThat(response).isEqualTo(merge);
  }

  @Test
  void r01901PostUndoAnswers200WithTheUndoneMerge() {
    UUID mergeId = UUID.randomUUID();
    MergeView undone =
        new MergeView(mergeId, JANEZ_ID, OTHER_ID, Instant.parse("2026-03-05T09:00:00Z"), true);
    when(merges.undoMerge(GROUP_ID, MARKO_ID, mergeId, 4)).thenReturn(undone);

    MergeView response = controller.undo(MARKO, GROUP_ID, mergeId, new VersionNumberRequest(4));

    assertThat(ResponseStatuses.of(MergeController.class, "undo")).isEqualTo(HttpStatus.OK);
    assertThat(response).isEqualTo(undone);
  }
}
