package app.contacts.web;

import app.contacts.service.GroupView;
import java.util.UUID;

/**
 * A Group with the caller's Role (contract {@code Group}).
 *
 * @param id the Group id
 * @param name the name
 * @param role the caller's Role
 */
public record GroupResponse(UUID id, String name, String role) {

  static GroupResponse of(GroupView group) {
    return new GroupResponse(group.id(), group.name(), group.role());
  }
}
