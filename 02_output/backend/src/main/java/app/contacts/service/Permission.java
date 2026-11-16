package app.contacts.service;

import app.contacts.domain.Role;
import java.util.EnumSet;
import java.util.Set;

/** What a Role may do in its Group: the permission table of ADR-04 (D-15). */
public enum Permission {
  /** List, search, filters, details, History, Tags, duplicates. */
  READ(Role.OWNER, Role.EDITOR, Role.VIEWER),
  /** Export to .xlsx. */
  EXPORT(Role.OWNER, Role.EDITOR, Role.VIEWER),
  /** Add, edit, Attributes, Tags, restore Version, recycle bin, merge, undo merge. */
  CHANGE_CONTACT(Role.OWNER, Role.EDITOR),
  /** See the recycle bin. */
  VIEW_RECYCLE_BIN(Role.OWNER, Role.EDITOR),
  /** Rename, move the Group to the recycle bin, Members, Invitations. */
  MANAGE_GROUP(Role.OWNER),
  /** Permanently delete a Contact. */
  PERMANENTLY_DELETE(Role.OWNER);

  private final Set<Role> roles;

  Permission(Role first, Role... others) {
    this.roles = EnumSet.of(first, others);
  }

  /**
   * Whether the Role has this permission.
   *
   * @param role the Role
   * @return true when allowed
   */
  public boolean allows(Role role) {
    return roles.contains(role);
  }
}
