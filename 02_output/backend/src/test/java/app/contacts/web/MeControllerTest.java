package app.contacts.web;

import static org.assertj.core.api.Assertions.assertThat;

import app.contacts.auth.CurrentUser;
import app.contacts.service.UserView;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MeControllerTest {

  @Test
  void r00104MeShowsTheUserAndWhetherTheEmailIsVerified() {
    UUID id = UUID.randomUUID();
    CurrentUser ana =
        new CurrentUser(new UserView(id, "ana.kovac@example.si", "Ana Kovač", "sl"), false);

    MeResponse me = new MeController().me(ana);

    assertThat(me).isEqualTo(new MeResponse(id, "ana.kovac@example.si", "Ana Kovač", "sl", false));
  }
}
