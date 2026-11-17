package app.contacts.web;

import app.contacts.auth.CurrentUser;
import app.contacts.service.HistoryService;
import app.contacts.service.VersionSummaryView;
import app.contacts.service.VersionView;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The History of a Contact ({@code /groups/{groupId}/contacts/{contactId}/versions}, US-010);
 * Versions are only read, so changing or deleting one is 405 (R-010-03, EX-010-03).
 */
@RestController
@RequestMapping("/api/v1/groups/{groupId}/contacts/{contactId}/versions")
public class HistoryController {

  private final HistoryService history;

  /**
   * The controller.
   *
   * @param history the History service
   */
  public HistoryController(HistoryService history) {
    this.history = history;
  }

  /**
   * All Versions newest first (R-010-01); the service views are the contract's {@code
   * VersionSummary}.
   *
   * @param current the signed-in User
   * @param groupId the Group
   * @param contactId the Contact
   * @return the History
   */
  @GetMapping
  public List<VersionSummaryView> history(
      @AuthenticationPrincipal CurrentUser current,
      @PathVariable UUID groupId,
      @PathVariable UUID contactId) {
    return history.history(groupId, current.id(), contactId);
  }

  /**
   * The full content after Version n (R-010-02); the service view is the contract's {@code
   * Version}.
   *
   * @param current the signed-in User
   * @param groupId the Group
   * @param contactId the Contact
   * @param number the Version number
   * @return the Version
   */
  @GetMapping("/{number}")
  public VersionView version(
      @AuthenticationPrincipal CurrentUser current,
      @PathVariable UUID groupId,
      @PathVariable UUID contactId,
      @PathVariable int number) {
    return history.version(groupId, current.id(), contactId, number);
  }
}
