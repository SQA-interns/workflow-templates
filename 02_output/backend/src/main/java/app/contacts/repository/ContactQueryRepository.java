package app.contacts.repository;

import app.contacts.domain.ContactAttributeRow;
import app.contacts.domain.ContactListRow;
import app.contacts.domain.ContactSort;
import app.contacts.domain.ContactTagRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * The Contact list of one Group: active Contacts only (BR-09), searched in {@code search_text} with
 * a LIKE pattern, sorted in Slovenian collation and paged (ADR-02, R-014-01 … R-014-04).
 */
@Repository
public class ContactQueryRepository {

  private static final String COLUMNS =
      "SELECT id, first_name, last_name, company, email, phone, updated_at FROM contact ";
  private static final String WHERE =
      "WHERE group_id = :groupId AND state = 'active' AND search_text LIKE :pattern ESCAPE '\\' ";
  private static final String NAME_ASC =
      "sort_name COLLATE \"sl-x-icu\", first_name COLLATE \"sl-x-icu\" NULLS LAST,"
          + " company COLLATE \"sl-x-icu\" NULLS LAST, id";
  private static final String NAME_DESC =
      "sort_name COLLATE \"sl-x-icu\" DESC, first_name COLLATE \"sl-x-icu\" DESC NULLS LAST,"
          + " company COLLATE \"sl-x-icu\" DESC NULLS LAST, id DESC";
  private static final String PAGE = " LIMIT :limit OFFSET :offset";

  @PersistenceContext private EntityManager entityManager;

  /**
   * One Page of the active Contacts of the Group whose search text matches the pattern.
   *
   * @param groupId the Group
   * @param pattern a LIKE pattern over the Normalised search text, {@code \} escaping
   * @param sort the sort key
   * @param descending whether the sort key runs Z–A; ties stay in name order
   * @param offset rows to skip
   * @param limit rows to return
   * @return the rows
   */
  public List<ContactListRow> findPage(
      UUID groupId, String pattern, ContactSort sort, boolean descending, int offset, int limit) {
    String sql =
        switch (sort) {
          case NAME ->
              descending
                  ? COLUMNS + WHERE + "ORDER BY " + NAME_DESC + PAGE
                  : COLUMNS + WHERE + "ORDER BY " + NAME_ASC + PAGE;
          case COMPANY ->
              descending
                  ? COLUMNS
                      + WHERE
                      + "ORDER BY company COLLATE \"sl-x-icu\" DESC NULLS LAST, "
                      + NAME_ASC
                      + PAGE
                  : COLUMNS
                      + WHERE
                      + "ORDER BY company COLLATE \"sl-x-icu\" NULLS LAST, "
                      + NAME_ASC
                      + PAGE;
          case EMAIL ->
              descending
                  ? COLUMNS + WHERE + "ORDER BY lower(email) DESC NULLS LAST, " + NAME_ASC + PAGE
                  : COLUMNS + WHERE + "ORDER BY lower(email) NULLS LAST, " + NAME_ASC + PAGE;
          case UPDATED_AT ->
              descending
                  ? COLUMNS + WHERE + "ORDER BY updated_at DESC, " + NAME_ASC + PAGE
                  : COLUMNS + WHERE + "ORDER BY updated_at, " + NAME_ASC + PAGE;
        };
    Query query =
        entityManager
            .createNativeQuery(sql)
            .setParameter("groupId", groupId)
            .setParameter("pattern", pattern)
            .setParameter("limit", limit)
            .setParameter("offset", offset);
    List<ContactListRow> rows = new ArrayList<>();
    for (Object result : query.getResultList()) {
      Object[] row = (Object[]) result;
      rows.add(
          new ContactListRow(
              (UUID) row[0],
              (String) row[1],
              (String) row[2],
              (String) row[3],
              (String) row[4],
              (String) row[5],
              (Instant) row[6]));
    }
    return rows;
  }

  /**
   * The number of active Contacts of the Group whose search text matches the pattern.
   *
   * @param groupId the Group
   * @param pattern the LIKE pattern of {@link #findPage}
   * @return the total
   */
  public long count(UUID groupId, String pattern) {
    Object total =
        entityManager
            .createNativeQuery("SELECT count(*) FROM contact " + WHERE)
            .setParameter("groupId", groupId)
            .setParameter("pattern", pattern)
            .getSingleResult();
    return ((Number) total).longValue();
  }

  /**
   * The Tags of the given Contacts of the Group, by name A–Z.
   *
   * @param groupId the Group
   * @param contactIds the Contacts
   * @return one row per Tag of a Contact
   */
  public List<ContactTagRow> findTags(UUID groupId, List<UUID> contactIds) {
    if (contactIds.isEmpty()) {
      return List.of();
    }
    Query query =
        entityManager
            .createNativeQuery(
                """
                SELECT ct.contact_id, t.id, t.name
                FROM contact_tag ct JOIN tag t ON t.id = ct.tag_id
                WHERE ct.group_id = :groupId AND ct.contact_id IN (:contactIds)
                ORDER BY t.name COLLATE "sl-x-icu", t.id
                """)
            .setParameter("groupId", groupId)
            .setParameter("contactIds", contactIds);
    List<ContactTagRow> tags = new ArrayList<>();
    for (Object result : query.getResultList()) {
      Object[] row = (Object[]) result;
      tags.add(new ContactTagRow((UUID) row[0], (UUID) row[1], (String) row[2]));
    }
    return tags;
  }

  /**
   * The Attributes of one Contact of the Group, by key A–Z in Slovenian collation (R-014-06,
   * R-008-06).
   *
   * @param groupId the Group
   * @param contactId the Contact
   * @return the Attributes; none for a Contact of another Group
   */
  public List<ContactAttributeRow> findAttributes(UUID groupId, UUID contactId) {
    Query query =
        entityManager
            .createNativeQuery(
                """
                SELECT key, value FROM attribute
                WHERE group_id = :groupId AND contact_id = :contactId
                ORDER BY key COLLATE "sl-x-icu", id
                """)
            .setParameter("groupId", groupId)
            .setParameter("contactId", contactId);
    List<ContactAttributeRow> attributes = new ArrayList<>();
    for (Object result : query.getResultList()) {
      Object[] row = (Object[]) result;
      attributes.add(new ContactAttributeRow((String) row[0], (String) row[1]));
    }
    return attributes;
  }
}
