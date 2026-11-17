package app.contacts.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.contacts.auth.CurrentUser;
import app.contacts.config.AppProperties;
import app.contacts.service.CreatedInvitation;
import app.contacts.service.GroupView;
import app.contacts.service.InvitationService;
import app.contacts.service.InvitationView;
import app.contacts.service.InviteeView;
import app.contacts.service.UserView;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class InvitationControllerTest {

  private static final UUID ANA_ID = UUID.randomUUID();
  private static final UserView ANA_VIEW =
      new UserView(ANA_ID, "ana.kovac@example.si", "Ana Kovač", "sl");
  private static final CurrentUser ANA = new CurrentUser(ANA_VIEW, true);
  private static final UUID GROUP_ID = UUID.randomUUID();
  private static final UUID INVITATION_ID = UUID.randomUUID();
  private static final String MARKO = "marko.kranjc@gmail.com";
  private static final Instant EXPIRES = Instant.parse("2026-03-12T09:00:00Z");
  private static final InvitationView OPEN =
      new InvitationView(INVITATION_ID, MARKO, "EDITOR", EXPIRES);
  private static final String TOKEN = "C2o8HooKTFWdfioRPE1ebwAAAAAAAAAAAAAAAAAAAAAA";

  private final InvitationService invitations = mock(InvitationService.class);
  private final InvitationController controller =
      new InvitationController(
          invitations, new AppProperties("https://kontakti.example.si/", 100, null));

  @Test
  void r00401CreateAnswers201WithTheLinkShownOnce() {
    when(invitations.create(GROUP_ID, ANA_ID, MARKO, "EDITOR"))
        .thenReturn(new CreatedInvitation(OPEN, TOKEN));

    ResponseEntity<InvitationCreatedResponse> res =
        controller.create(ANA, GROUP_ID, new InvitationRequest(MARKO, "EDITOR"));

    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(res.getBody())
        .isEqualTo(
            new InvitationCreatedResponse(
                new InvitationResponse(INVITATION_ID, MARKO, "EDITOR", EXPIRES),
                "https://kontakti.example.si/invitations/" + TOKEN));
  }

  @Test
  void r00408ListShowsEmailRoleAndExpiryWithoutLink() {
    when(invitations.listOpen(GROUP_ID, ANA_ID)).thenReturn(List.of(OPEN));

    assertThat(controller.list(ANA, GROUP_ID))
        .containsExactly(new InvitationResponse(INVITATION_ID, MARKO, "EDITOR", EXPIRES));
  }

  @Test
  void r00409RevokeAnswers204() {
    ResponseEntity<Void> res = controller.revoke(ANA, GROUP_ID, INVITATION_ID);

    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    verify(invitations).revoke(GROUP_ID, ANA_ID, INVITATION_ID);
  }

  @Test
  void r00404ViewShowsGroupNameRoleAndExpiry() {
    when(invitations.view(TOKEN, ANA_VIEW))
        .thenReturn(new InviteeView("Konferenca 2025", "EDITOR", EXPIRES));

    assertThat(controller.view(ANA, TOKEN))
        .isEqualTo(new InvitationViewResponse("Konferenca 2025", "EDITOR", EXPIRES));
  }

  @Test
  void r00404AcceptAnswersTheGroupWithTheNewRole() {
    when(invitations.accept(TOKEN, ANA_VIEW))
        .thenReturn(new GroupView(GROUP_ID, "Konferenca 2025", "EDITOR"));

    assertThat(controller.accept(ANA, TOKEN))
        .isEqualTo(new GroupResponse(GROUP_ID, "Konferenca 2025", "EDITOR"));
  }
}
