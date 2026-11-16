package app.contacts.service;

import app.contacts.domain.ContactFields;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * What a Version records of a Contact: its Fields, Attributes and Tag names (Data model
 * "snapshot").
 *
 * @param fields the Fields
 * @param attributes the Attributes by key A–Z in Slovenian collation, spelling kept (R-008-06)
 * @param tags the Tag names A–Z in Slovenian collation, in the Tags' spelling (R-009-03)
 */
public record ContactContent(
    ContactFields fields, List<AttributeView> attributes, List<String> tags) {

  /** A Contact before it is added: no Fields, no Attributes, no Tags. */
  public static final ContactContent EMPTY =
      new ContactContent(ContactFields.EMPTY, List.of(), List.of());

  /**
   * The content with its Attributes sorted by key and its Tags by name, so that equal content is
   * equal.
   *
   * @param fields the Fields
   * @param attributes the Attributes in any order
   * @param tags the Tag names in any order
   */
  public ContactContent {
    Comparator<Object> slovenian = Collator.getInstance(Locale.forLanguageTag("sl"));
    List<AttributeView> sortedAttributes = new ArrayList<>(attributes);
    sortedAttributes.sort(Comparator.comparing(AttributeView::key, slovenian));
    attributes = List.copyOf(sortedAttributes);
    List<String> sortedTags = new ArrayList<>(tags);
    sortedTags.sort(slovenian);
    tags = List.copyOf(sortedTags);
  }
}
