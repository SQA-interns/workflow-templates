package app.contacts.service;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The contract's {@code ContactFields} (D-23) for the generated OpenAPI document (AR-02): the code
 * carries the Fields as a map from name to value, which springdoc would describe as a free map.
 * Only named in {@code @Schema(implementation = …)}; never built.
 *
 * @param firstName first name
 * @param lastName last name
 * @param email e-mail
 * @param phone phone
 * @param company company
 * @param jobTitle job title
 * @param street street
 * @param postalCode postal code
 * @param city city
 * @param country ISO 3166-1 alpha-2 code
 * @param note note
 */
@Schema(name = "ContactFields")
public record ContactFieldsSchema(
    String firstName,
    String lastName,
    String email,
    String phone,
    String company,
    String jobTitle,
    String street,
    String postalCode,
    String city,
    String country,
    String note) {}
