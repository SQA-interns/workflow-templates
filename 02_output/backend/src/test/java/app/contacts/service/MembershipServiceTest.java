package app.contacts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.contacts.domain.MemberRow;
import app.contacts.repository.MembershipRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MembershipServiceTest {

  private static final UUID GROUP = UUID.randomUUID();
  private static final UUID ANA = UUID.randomUUID();
  private static final UUID MARKO = UUID.randomUUID();

  private final MembershipRepository memberships = mock(MembershipRepository.class);
  private final AccessGuard guard = mock(AccessGuard.class);
  private final MembershipService service = new MembershipService(memberships, guard);

  @Test
  void r00501OwnerSeesEveryMemberWithDisplayNameEmailAndRole() {
    when(memberships.findMembers(GROUP))
        .thenReturn(
            List.of(
                new MemberRow(ANA, "Ana Kovač", "ana.kovac@example.si", "OWNER"),
                new MemberRow(MARKO, "Marko Kranjc", "marko.kranjc@gmail.com", "EDITOR")));

    assertThat(service.list(GROUP, ANA))
        .containsExactly(
            new MemberView(ANA, "Ana Kovač", "ana.kovac@example.si", "OWNER"),
            new MemberView(MARKO, "Marko Kranjc", "marko.kranjc@gmail.com", "EDITOR"));
    verify(guard).require(GROUP, ANA, Permission.MANAGE_GROUP);
  }

  @Test
  void r00501OnlyTheOwnerSeesTheMembers() {
    when(guard.require(GROUP, MARKO, Permission.MANAGE_GROUP)).thenThrow(new ForbiddenException());

    assertThatThrownBy(() -> service.list(GROUP, MARKO)).isInstanceOf(ForbiddenException.class);
    verifyNoInteractions(memberships);
  }
}
