package app.contacts.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.contacts.auth.CurrentUser;
import app.contacts.service.UserService;
import app.contacts.service.UserView;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MeControllerTest {

  private static final UUID ID = UUID.randomUUID();

  private final UserService users = mock(UserService.class);
  private final MeController controller = new MeController(users);

  @Test
  void r00104MeShowsTheUserAndWhetherTheEmailIsVerified() {
    CurrentUser ana =
        new CurrentUser(new UserView(ID, "ana.kovac@example.si", "Ana Kovač", "sl"), false);

    MeResponse me = controller.me(ana);

    assertThat(me).isEqualTo(new MeResponse(ID, "ana.kovac@example.si", "Ana Kovač", "sl", false));
  }

  @Test
  void r02002PatchMeChangesTheLanguageAndShowsTheChangedUser() {
    CurrentUser ana =
        new CurrentUser(new UserView(ID, "ana.kovac@example.si", "Ana Kovač", "sl"), true);
    when(users.changeLanguage(ID, "en"))
        .thenReturn(new UserView(ID, "ana.kovac@example.si", "Ana Kovač", "en"));

    MeResponse me = controller.update(ana, new MeUpdate("en"));

    assertThat(me).isEqualTo(new MeResponse(ID, "ana.kovac@example.si", "Ana Kovač", "en", true));
  }

  @Test
  void r02101DeleteMeDeletesTheAccountWithTheTypedEmail() {
    CurrentUser marko =
        new CurrentUser(new UserView(ID, "marko.kranjc@gmail.com", "Marko Kranjc", "sl"), true);

    controller.delete(marko, new AccountDeletion("marko.kranjc@gmail.com"));

    verify(users).closeAccount(ID, "marko.kranjc@gmail.com");
  }
}
