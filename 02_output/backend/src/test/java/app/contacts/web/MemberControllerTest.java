package app.contacts.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.contacts.auth.CurrentUser;
import app.contacts.service.MemberView;
import app.contacts.service.MembershipService;
import app.contacts.service.UserView;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class MemberControllerTest {

  private static final UUID ANA_ID = UUID.randomUUID();
  private static final CurrentUser ANA =
      new CurrentUser(new UserView(ANA_ID, "ana.kovac@example.si", "Ana Kovač", "sl"), true);
  private static final UUID GROUP_ID = UUID.randomUUID();
  private static final UUID MARKO_ID = UUID.randomUUID();
  private static final MemberView MARKO =
      new MemberView(MARKO_ID, "Marko Kranjc", "marko.kranjc@gmail.com", "EDITOR");

  private final MembershipService memberships = mock(MembershipService.class);
  private final MemberController controller = new MemberController(memberships);

  @Test
  void r00501ListAnswersDisplayNameEmailAndRole() {
    when(memberships.list(GROUP_ID, ANA_ID)).thenReturn(List.of(MARKO));

    assertThat(controller.list(ANA, GROUP_ID))
        .containsExactly(
            new MemberResponse(MARKO_ID, "Marko Kranjc", "marko.kranjc@gmail.com", "EDITOR"));
  }

  @Test
  void r00502ChangeRoleAnswersTheMemberWithTheNewRole() {
    when(memberships.changeRole(GROUP_ID, ANA_ID, MARKO_ID, "VIEWER"))
        .thenReturn(new MemberView(MARKO_ID, "Marko Kranjc", "marko.kranjc@gmail.com", "VIEWER"));

    assertThat(controller.changeRole(ANA, GROUP_ID, MARKO_ID, new RoleChangeRequest("VIEWER")))
        .isEqualTo(
            new MemberResponse(MARKO_ID, "Marko Kranjc", "marko.kranjc@gmail.com", "VIEWER"));
  }

  @Test
  void r00503RemoveAnswers204() {
    ResponseEntity<Void> res = controller.remove(ANA, GROUP_ID, MARKO_ID);

    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    verify(memberships).endMembership(GROUP_ID, ANA_ID, MARKO_ID);
  }
}
