package app.contacts.domain;

import jakarta.persistence.Embeddable;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * The Fields of a Contact in the order of the Contact form (glossary "Field", D-23); an empty Field
 * is {@code null}.
 *
 * @param firstName first name
 * @param lastName last name
 * @param email e-mail address
 * @param phone phone number
 * @param company company
 * @param jobTitle job title
 * @param street street and house number
 * @param postalCode postal code
 * @param city city
 * @param country ISO 3166-1 alpha-2 code
 * @param note note
 */
@Embeddable
public record ContactFields(
    String firstName,
    String lastName,
    String email,
    String phone,
    String company,
    String jobTitle,
    String street,
    String postalCode,
    String city,
    @JdbcTypeCode(SqlTypes.CHAR) String country,
    String note) {

  /** The Field names in the order of the Contact form (D-23). */
  public static final List<String> NAMES =
      List.of(
          "firstName",
          "lastName",
          "email",
          "phone",
          "company",
          "jobTitle",
          "street",
          "postalCode",
          "city",
          "country",
          "note");

  /**
   * The Fields from their names; a missing name is an empty Field.
   *
   * @param values Field name to value
   * @return the Fields
   */
  public static ContactFields ofNames(Map<String, String> values) {
    return new ContactFields(
        values.get("firstName"),
        values.get("lastName"),
        values.get("email"),
        values.get("phone"),
        values.get("company"),
        values.get("jobTitle"),
        values.get("street"),
        values.get("postalCode"),
        values.get("city"),
        values.get("country"),
        values.get("note"));
  }

  /**
   * Field name to value in the order of {@link #NAMES}, empty Fields included as {@code null}.
   *
   * @return the values
   */
  public SequencedMap<String, String> byName() {
    List<String> values =
        Arrays.asList(
            firstName,
            lastName,
            email,
            phone,
            company,
            jobTitle,
            street,
            postalCode,
            city,
            country,
            note);
    SequencedMap<String, String> byName = new LinkedHashMap<>();
    for (int i = 0; i < NAMES.size(); i++) {
      byName.put(NAMES.get(i), values.get(i));
    }
    return byName;
  }
}
