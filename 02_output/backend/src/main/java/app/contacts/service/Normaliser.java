package app.contacts.service;

import app.contacts.domain.ContactFields;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/** Derived values of a Contact, kept with it on every save (specification §3, ADR-02). */
@Component
public class Normaliser {

  /**
   * Normalised text: lower case, {@code č→c š→s ž→z ć→c đ→d} (US-014).
   *
   * @param text the text
   * @return the Normalised text
   */
  public String text(String text) {
    return text.toLowerCase(Locale.ROOT)
        .replace('č', 'c')
        .replace('š', 's')
        .replace('ž', 'z')
        .replace('ć', 'c')
        .replace('đ', 'd');
  }

  /**
   * The search text: the Normalised first name, last name, e-mail, phone, company and city, one per
   * line so that a search never matches across two Fields (R-014-04).
   *
   * @param fields the Fields
   * @return the search text
   */
  public String searchText(ContactFields fields) {
    return Stream.of(
            fields.firstName(),
            fields.lastName(),
            fields.email(),
            fields.phone(),
            fields.company(),
            fields.city())
        .filter(Objects::nonNull)
        .map(this::text)
        .collect(Collectors.joining(" "));
  }

  /**
   * The sort name: the last name, else the first name, else the company (R-014-02, EX-014-04).
   *
   * @param fields the Fields, with at least one name part (R-006-02)
   * @return the sort name
   */
  public String sortName(ContactFields fields) {
    return Stream.of(fields.lastName(), fields.firstName(), fields.company())
        .filter(Objects::nonNull)
        .findFirst()
        .orElseThrow();
  }
}
