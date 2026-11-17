package app.contacts.web;

import app.contacts.service.DeletedGroupView;
import java.time.Instant;
import java.util.UUID;

/**
 * A Group in the recycle bin with its removal date (contract {@code DeletedGroup}).
 *
 * @param id the Group id
 * @param name the name
 * @param deletedAt when it was moved to the recycle bin
 * @param removalAt when it is removed
 */
public record DeletedGroupResponse(UUID id, String name, Instant deletedAt, Instant removalAt) {

  static DeletedGroupResponse of(DeletedGroupView group) {
    return new DeletedGroupResponse(group.id(), group.name(), group.deletedAt(), group.removalAt());
  }
}
