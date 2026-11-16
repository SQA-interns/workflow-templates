package app.contacts.web;

import app.contacts.auth.CurrentUser;
import app.contacts.service.TagService;
import app.contacts.service.TagView;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Tags of a Group ({@code /groups/{groupId}/tags}, US-009). */
@RestController
@RequestMapping("/api/v1/groups/{groupId}/tags")
public class TagController {

  private final TagService tags;

  /**
   * The controller.
   *
   * @param tags the Tag service
   */
  public TagController(TagService tags) {
    this.tags = tags;
  }

  /**
   * The Tags to suggest for the typed text (R-009-02).
   *
   * @param current the signed-in User
   * @param groupId the Group
   * @param q the typed text; missing for every Tag on an active Contact
   * @return the Tags by name A–Z
   */
  @GetMapping
  public List<TagView> suggest(
      @AuthenticationPrincipal CurrentUser current,
      @PathVariable UUID groupId,
      @RequestParam(required = false) String q) {
    return tags.suggest(groupId, current.id(), q);
  }
}
