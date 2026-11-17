package app.contacts.service;

import java.util.Map;
import java.util.UUID;

/**
 * A Merge as the User confirmed it (contract {@code MergeRequest}).
 *
 * @param primaryId the Primary contact
 * @param otherId the Other contact
 * @param primaryVersionNumber the Primary contact's Version number the client read (BR-06)
 * @param otherVersionNumber the Other contact's Version number the client read (BR-06)
 * @param fieldChoices Field name to {@code primary} or {@code other}; {@code null} for none
 * @param attributeChoices Attribute key to {@code primary} or {@code other}; {@code null} for none
 */
public record MergeCommand(
    UUID primaryId,
    UUID otherId,
    Integer primaryVersionNumber,
    Integer otherVersionNumber,
    Map<String, String> fieldChoices,
    Map<String, String> attributeChoices) {

  /**
   * A command with its own copies of the choices.
   *
   * @param primaryId the Primary contact
   * @param otherId the Other contact
   * @param primaryVersionNumber the Primary contact's Version number
   * @param otherVersionNumber the Other contact's Version number
   * @param fieldChoices the Field choices
   * @param attributeChoices the Attribute choices
   */
  public MergeCommand {
    fieldChoices = fieldChoices == null ? null : Map.copyOf(fieldChoices);
    attributeChoices = attributeChoices == null ? null : Map.copyOf(attributeChoices);
  }
}
