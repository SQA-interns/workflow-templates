package app.contacts.web;

import app.contacts.auth.CurrentUser;
import app.contacts.service.ContactQuery;
import app.contacts.service.ExportFile;
import app.contacts.service.ExportService;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Export to {@code .xlsx} ({@code /groups/{groupId}/exports}, US-016, ADR-09). */
@RestController
public class ExportController {

  /** The media type of an {@code .xlsx} file (contract {@code exportContacts}). */
  static final MediaType XLSX =
      MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

  private final ExportService exports;

  /**
   * The controller.
   *
   * @param exports the Export service
   */
  public ExportController(ExportService exports) {
    this.exports = exports;
  }

  /**
   * The Export scope as an {@code .xlsx} file, named in {@code Content-Disposition} with RFC 5987
   * UTF-8 encoding (R-016-01, R-016-06, ADR-09).
   *
   * @param current the signed-in User; the header row is in the User's language (R-016-04)
   * @param groupId the Group
   * @param body the list's search, sort and filters, and the Checked contacts
   * @return the file
   */
  @PostMapping("/api/v1/groups/{groupId}/exports")
  public ResponseEntity<byte[]> export(
      @AuthenticationPrincipal CurrentUser current,
      @PathVariable UUID groupId,
      @RequestBody ExportRequest body) {
    ContactQuery.Filters filters =
        new ContactQuery.Filters(
            body.hasTagIds(),
            body.withoutTagIds(),
            body.attributeKey(),
            body.attributeValue(),
            body.company(),
            body.country());
    ExportFile file =
        exports.export(
            groupId,
            current.id(),
            current.user().language(),
            new ContactQuery(body.q(), body.sort(), body.dir(), null, filters),
            body.contactIds());
    ContentDisposition disposition =
        ContentDisposition.attachment().filename(file.fileName(), StandardCharsets.UTF_8).build();
    return ResponseEntity.ok()
        .contentType(XLSX)
        .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
        .body(file.content());
  }
}
