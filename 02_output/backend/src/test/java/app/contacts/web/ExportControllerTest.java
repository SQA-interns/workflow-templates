package app.contacts.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.contacts.auth.CurrentUser;
import app.contacts.service.ContactQuery;
import app.contacts.service.ExportFile;
import app.contacts.service.ExportService;
import app.contacts.service.UserView;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ExportControllerTest {

  private static final UUID PETRA_ID = UUID.randomUUID();
  private static final UUID GROUP_ID = UUID.randomUUID();
  private static final CurrentUser PETRA =
      new CurrentUser(new UserView(PETRA_ID, "petra.horvat@gmail.com", "Petra Horvat", "en"), true);
  private static final byte[] XLSX = {80, 75, 3, 4};

  private final ExportService exports = mock(ExportService.class);
  private final ExportController controller = new ExportController(exports);

  @Test
  void r01601R01606PostAnswersTheFileWithItsUtf8FileName() {
    UUID udel = UUID.randomUUID();
    UUID maja = UUID.randomUUID();
    ContactQuery query =
        new ContactQuery(
            "novak",
            "company",
            "desc",
            null,
            new ContactQuery.Filters(List.of(udel), null, "jezik", "nemščina", "Alfa", "SI"));
    when(exports.export(GROUP_ID, PETRA_ID, "en", query, List.of(maja)))
        .thenReturn(new ExportFile("kontakti-Žiga 2025-2026-03-05.xlsx", XLSX));
    ExportRequest request =
        new ExportRequest(
            "novak",
            "company",
            "desc",
            List.of(udel),
            null,
            "jezik",
            "nemščina",
            "Alfa",
            "SI",
            List.of(maja));

    ResponseEntity<byte[]> response = controller.export(PETRA, GROUP_ID, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(XLSX);
    assertThat(response.getHeaders().getContentType())
        .hasToString("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    assertThat(response.getHeaders().getContentDisposition().getFilename())
        .isEqualTo("kontakti-Žiga 2025-2026-03-05.xlsx");
    assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
        .startsWith("attachment;")
        .contains("filename*=UTF-8''kontakti-%C5%BDiga%202025-2026-03-05.xlsx");
  }

  @Test
  void r01601AnEmptyRequestExportsTheWholeList() {
    ContactQuery all = new ContactQuery(null, null, null, null, ContactQuery.Filters.NONE);
    when(exports.export(GROUP_ID, PETRA_ID, "en", all, null))
        .thenReturn(new ExportFile("kontakti-K-2026-03-05.xlsx", XLSX));

    ResponseEntity<byte[]> response =
        controller.export(
            PETRA,
            GROUP_ID,
            new ExportRequest(null, null, null, null, null, null, null, null, null, null));

    assertThat(response.getBody()).isEqualTo(XLSX);
  }
}
