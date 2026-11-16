package app.contacts.service;

import java.util.List;

/**
 * One Page of the Contact list with the total (contract {@code ContactPage}, R-014-01).
 *
 * @param items the rows of the Page
 * @param total the number of matching Contacts
 * @param page the Page number
 * @param pageSize Contacts per Page
 */
public record ContactPageView(List<ContactListItemView> items, long total, int page, int pageSize) {

  /**
   * A Page with its own copy of the rows.
   *
   * @param items the rows
   * @param total the total
   * @param page the Page number
   * @param pageSize Contacts per Page
   */
  public ContactPageView {
    items = List.copyOf(items);
  }
}
