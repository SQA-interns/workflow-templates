package app.contacts.service;

import static app.contacts.service.Permission.CHANGE_CONTACT;
import static app.contacts.service.Permission.EXPORT;
import static app.contacts.service.Permission.MANAGE_GROUP;
import static app.contacts.service.Permission.READ;
import static app.contacts.service.Permission.VIEW_RECYCLE_BIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.contacts.domain.Role;
import app.contacts.repository.MembershipRepository;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class AccessGuardTest {

  private static final UUID GROUP = UUID.randomUUID();
  private static final UUID USER = UUID.randomUUID();

  private final MembershipRepository memberships = mock(MembershipRepository.class);
  private final AccessGuard guard = new AccessGuard(memberships);

  @Test
  void br02NonMemberOrGroupInRecycleBinGetsNotFound() {
    when(memberships.findActiveRole(GROUP, USER)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> guard.require(GROUP, USER, READ))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void adr04MemberWithThePermissionGetsTheRole() {
    when(memberships.findActiveRole(GROUP, USER)).thenReturn(Optional.of(Role.OWNER));

    assertThat(guard.require(GROUP, USER, MANAGE_GROUP)).isEqualTo(Role.OWNER);
  }

  @Test
  void br03MemberWithoutThePermissionIsForbidden() {
    when(memberships.findActiveRole(GROUP, USER)).thenReturn(Optional.of(Role.VIEWER));

    assertThatThrownBy(() -> guard.require(GROUP, USER, CHANGE_CONTACT))
        .isInstanceOf(ForbiddenException.class);
  }

  @ParameterizedTest
  @EnumSource(Role.class)
  void adr04PermissionTable(Role role) {
    Set<Permission> expected =
        switch (role) {
          case OWNER -> EnumSet.allOf(Permission.class);
          case EDITOR -> EnumSet.of(READ, EXPORT, CHANGE_CONTACT, VIEW_RECYCLE_BIN);
          case VIEWER -> EnumSet.of(READ, EXPORT);
        };

    for (Permission permission : Permission.values()) {
      assertThat(permission.allows(role))
          .as("%s %s", role, permission)
          .isEqualTo(expected.contains(permission));
    }
  }
}
