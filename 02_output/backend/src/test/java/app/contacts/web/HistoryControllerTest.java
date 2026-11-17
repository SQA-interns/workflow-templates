package app.contacts.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.contacts.auth.CurrentUser;
import app.contacts.service.ChangeView;
import app.contacts.service.HistoryService;
import app.contacts.service.SnapshotView;
import app.contacts.service.UserView;
import app.contacts.service.VersionSummaryView;
import app.contacts.service.VersionView;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class HistoryControllerTest {

  private static final Instant EDITED_AT = Instant.parse("2026-07-01T08:00:00Z");
  private static final UUID PETRA_ID = UUID.randomUUID();
  private static final UUID GROUP_ID = UUID.randomUUID();
  private static final UUID CONTACT_ID = UUID.randomUUID();
  private static final CurrentUser PETRA =
      new CurrentUser(new UserView(PETRA_ID, "petra.zupan@example.si", "Petra Zupan", "sl"), true);
  private static final String VERSION_1 =
      "/api/v1/groups/" + GROUP_ID + "/contacts/" + CONTACT_ID + "/versions/1";

  private final HistoryService history = mock(HistoryService.class);
  private final HistoryController controller = new HistoryController(history);

  @Test
  void r01001GetVersionsAnswersTheHistory() {
    VersionSummaryView edited =
        new VersionSummaryView(
            2,
            "edited",
            "Marko Kranjc",
            EDITED_AT,
            null,
            List.of(new ChangeView("email", "janez.novak@example.si", "janez@novak.si")));
    when(history.history(GROUP_ID, PETRA_ID, CONTACT_ID)).thenReturn(List.of(edited));

    assertThat(controller.history(PETRA, GROUP_ID, CONTACT_ID)).containsExactly(edited);
  }

  @Test
  void r01002GetVersionAnswersTheVersionWithItsContent() {
    VersionView created =
        new VersionView(
            1,
            "created",
            "Ana Kovač",
            EDITED_AT,
            null,
            List.of(),
            new SnapshotView(new LinkedHashMap<>(), List.of(), List.of()));
    when(history.version(GROUP_ID, PETRA_ID, CONTACT_ID, 1)).thenReturn(created);

    assertThat(controller.version(PETRA, GROUP_ID, CONTACT_ID, 1)).isEqualTo(created);
  }

  @Test
  void ex01003ChangingOrDeletingVersionIs405AndReachesNoService() throws Exception {
    MockMvc mvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new ProblemHandler())
            .build();
    for (MockHttpServletRequestBuilder request :
        List.of(put(VERSION_1), patch(VERSION_1), delete(VERSION_1))) {
      mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content("{\"kind\":\"edited\"}"))
          .andExpect(status().isMethodNotAllowed())
          .andExpect(jsonPath("$.status").value(405));
    }
    verifyNoInteractions(history);
  }
}
