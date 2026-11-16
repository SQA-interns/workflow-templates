package app.contacts.domain;

/** What the Contact list is sorted by (R-014-02, R-014-03; contract parameter {@code sort}). */
public enum ContactSort {
  /** Last name, else first name, else company; then first name, then company (D-24 (a)). */
  NAME,
  /** Company, then name. */
  COMPANY,
  /** E-mail, then name. */
  EMAIL,
  /** Time of the last change, then name. */
  UPDATED_AT
}
