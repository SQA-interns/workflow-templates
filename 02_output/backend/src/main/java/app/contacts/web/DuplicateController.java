package app.contacts.web;

import app.contacts.auth.CurrentUser;
import app.contacts.service.DuplicateCandidateView;
import app.contacts.service.DuplicateService;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Possible duplicates of a Group's Contacts (US-017). */
@RestController
@RequestMapping("/api/v1/groups/{groupId}")
public class DuplicateController {

  private final DuplicateService duplicates;

  /**
   * The controller.
   *
   * @param duplicates the duplicate service
   */
  public DuplicateController(DuplicateService duplicates) {
    this.duplicates = duplicates;
  }

  /**
   * Every pair of Possible duplicates with every reason (R-017-01 … R-017-05); the service views
   * are the contract's {@code DuplicateCandidate}.
   *
   * @param current the signed-in User
   * @param groupId the Group
   * @return the pairs
   */
  @GetMapping("/duplicates")
  public List<DuplicateCandidateView> list(
      @AuthenticationPrincipal CurrentUser current, @PathVariable UUID groupId) {
    return duplicates.find(groupId, current.id());
  }
}
