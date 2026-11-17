package app.contacts.web;

import app.contacts.service.MergeCommand;
import java.util.Map;
import java.util.UUID;

/**
 * The body of a Merge (contract {@code MergeRequest}).
 *
 * @param primaryId the Primary contact
 * @param otherId the Other contact
 * @param primaryVersionNumber the Primary contact's Version number the client read
 * @param otherVersionNumber the Other contact's Version number the client read
 * @param fieldChoices Field name to {@code primary} or {@code other}
 * @param attributeChoices Attribute key to {@code primary} or {@code other}
 */
public record MergeRequest(
    UUID primaryId,
    UUID otherId,
    Integer primaryVersionNumber,
    Integer otherVersionNumber,
    Map<String, String> fieldChoices,
    Map<String, String> attributeChoices) {

  /**
   * A request with its own copies of the choices.
   *
   * @param primaryId the Primary contact
   * @param otherId the Other contact
   * @param primaryVersionNumber the Primary contact's Version number
   * @param otherVersionNumber the Other contact's Version number
   * @param fieldChoices the Field choices
   * @param attributeChoices the Attribute choices
   */
  public MergeRequest {
    fieldChoices = fieldChoices == null ? null : Map.copyOf(fieldChoices);
    attributeChoices = attributeChoices == null ? null : Map.copyOf(attributeChoices);
  }

  MergeCommand toCommand() {
    return new MergeCommand(
        primaryId,
        otherId,
        primaryVersionNumber,
        otherVersionNumber,
        fieldChoices,
        attributeChoices);
  }
}
