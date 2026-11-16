package app.contacts.domain;

import java.util.UUID;

/** One Group of the Group switcher with the caller's Role in it (R-002-03). */
public interface MemberGroup {

  /**
   * The Group id.
   *
   * @return the id
   */
  UUID getId();

  /**
   * The Group name.
   *
   * @return the name
   */
  String getName();

  /**
   * The caller's Role, e.g. {@code OWNER}.
   *
   * @return the Role name
   */
  String getRole();
}
