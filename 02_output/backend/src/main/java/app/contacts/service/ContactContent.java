package app.contacts.service;

import app.contacts.domain.ContactFields;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * What a Version records of a Contact: its Fields and its Attributes (Data model "snapshot").
 *
 * @param fields the Fields
 * @param attributes the Attributes by key A–Z in Slovenian collation, spelling kept (R-008-06)
 */
public record ContactContent(ContactFields fields, List<AttributeView> attributes) {

  /** A Contact before it is added: no Fields, no Attributes. */
  public static final ContactContent EMPTY = new ContactContent(ContactFields.EMPTY, List.of());

  /**
   * The content with its Attributes sorted by key, so that equal content is equal.
   *
   * @param fields the Fields
   * @param attributes the Attributes in any order
   */
  public ContactContent {
    Comparator<Object> slovenian = Collator.getInstance(Locale.forLanguageTag("sl"));
    List<AttributeView> sorted = new ArrayList<>(attributes);
    sorted.sort(Comparator.comparing(AttributeView::key, slovenian));
    attributes = List.copyOf(sorted);
  }
}
