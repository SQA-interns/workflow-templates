package app.contacts.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.contacts.auth.CurrentUser;
import app.contacts.service.ContactView;
import app.contacts.service.RecycleBinService;
import app.contacts.service.UserView;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecycleBinControllerTest {

  private static final Instant MOVED_AT = Instant.parse("2026-03-05T10:00:00Z");
  private static final UUID MARKO_ID = UUID.randomUUID();
  private static final UUID GROUP_ID = UUID.randomUUID();
  private static final UUID CONTACT_ID = UUID.randomUUID();
  private static final CurrentUser MARKO =
      new CurrentUser(new UserView(MARKO_ID, "marko.kranjc@gmail.com", "Marko Kranjc", "sl"), true);

  private final RecycleBinService recycleBin = mock(RecycleBinService.class);
  private final RecycleBinController controller = new RecycleBinController(recycleBin);

  @Test
  void r01201PostMoveAnswersTheContactInTheRecycleBin() {
    when(recycleBin.move(GROUP_ID, MARKO_ID, CONTACT_ID, 3))
        .thenReturn(
            new ContactView(
                CONTACT_ID,
                4,
                "inRecycleBin",
                new LinkedHashMap<>(),
                List.of(),
                List.of(),
                MOVED_AT,
                MOVED_AT));

    ContactResponse moved =
        controller.move(MARKO, GROUP_ID, CONTACT_ID, new VersionNumberRequest(3));

    assertThat(moved.state()).isEqualTo("inRecycleBin");
    assertThat(moved.versionNumber()).isEqualTo(4);
  }
}
