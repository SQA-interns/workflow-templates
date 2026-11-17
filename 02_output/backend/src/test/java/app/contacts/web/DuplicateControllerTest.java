package app.contacts.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.contacts.auth.CurrentUser;
import app.contacts.service.ContactRefView;
import app.contacts.service.DuplicateCandidateView;
import app.contacts.service.DuplicateService;
import app.contacts.service.UserView;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

class DuplicateControllerTest {

  private static final UUID MARKO_ID = UUID.randomUUID();
  private static final UUID GROUP_ID = UUID.randomUUID();
  private static final CurrentUser MARKO =
      new CurrentUser(new UserView(MARKO_ID, "marko.kranjc@gmail.com", "Marko Kranjc", "sl"), true);

  private final DuplicateService duplicates = mock(DuplicateService.class);
  private final DuplicateController controller = new DuplicateController(duplicates);

  @Test
  void r01701GetDuplicatesAnswersThePairsWithTheirReasons() {
    DuplicateCandidateView pair =
        new DuplicateCandidateView(
            new ContactRefView(UUID.randomUUID(), "Janez Novak"),
            new ContactRefView(UUID.randomUUID(), "J. Novak"),
            List.of("sameEmail"));
    when(duplicates.find(GROUP_ID, MARKO_ID)).thenReturn(List.of(pair));

    assertThat(controller.list(MARKO, GROUP_ID)).containsExactly(pair);
  }

  @Test
  void r01701DuplicatesAreAtTheContractPath() throws NoSuchMethodException {
    String base = DuplicateController.class.getAnnotation(RequestMapping.class).value()[0];
    String path =
        DuplicateController.class
            .getMethod("list", CurrentUser.class, UUID.class)
            .getAnnotation(GetMapping.class)
            .value()[0];

    assertThat(base + path).isEqualTo("/api/v1/groups/{groupId}/duplicates");
  }
}
