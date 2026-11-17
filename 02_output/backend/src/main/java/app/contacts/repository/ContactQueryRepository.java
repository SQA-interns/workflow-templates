package app.contacts.repository;

import app.contacts.domain.ContactAttributeRow;
import app.contacts.domain.ContactExportAttributeRow;
import app.contacts.domain.ContactExportRow;
import app.contacts.domain.ContactFields;
import app.contacts.domain.ContactFilters;
import app.contacts.domain.ContactListRow;
import app.contacts.domain.ContactSort;
import app.contacts.domain.ContactTagRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * The Contact list of one Group: active Contacts only (BR-09), searched in {@code search_text} with
 * a LIKE pattern, filtered, sorted in Slovenian collation and paged (ADR-02, R-014-01 … R-014-04,
 * R-015-01 … R-015-05).
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
  private static final String EXPORT_COLUMNS =
      "SELECT id, first_name, last_name, email, phone, company, job_title, street, postal_code,"
          + " city, country, note FROM contact ";

  /** The ORDER BY of each sort key A–Z; ties stay in name order (R-014-02, R-014-03). */
  private static final Map<ContactSort, String> ASCENDING =
      Map.of(
          ContactSort.NAME, NAME_ASC,
          ContactSort.COMPANY, "company COLLATE \"sl-x-icu\" NULLS LAST, " + NAME_ASC,
          ContactSort.EMAIL, "lower(email) NULLS LAST, " + NAME_ASC,
          ContactSort.UPDATED_AT, "updated_at, " + NAME_ASC);

  /** The ORDER BY of each sort key Z–A; ties stay in name order (R-014-03). */
  private static final Map<ContactSort, String> DESCENDING =
      Map.of(
          ContactSort.NAME, NAME_DESC,
          ContactSort.COMPANY, "company COLLATE \"sl-x-icu\" DESC NULLS LAST, " + NAME_ASC,
          ContactSort.EMAIL, "lower(email) DESC NULLS LAST, " + NAME_ASC,
          ContactSort.UPDATED_AT, "updated_at DESC, " + NAME_ASC);

  private static final String PAGE = " LIMIT :limit OFFSET :offset";

  /** Only the Checked contacts among the list (R-016-02). */
  private static final String CHECKED = "AND id IN (:contactIds) ";

  /** Every one of the Tags (R-015-01); the ids are distinct, so the count says "all". */
  private static final String HAS_TAGS =
      "AND id IN (SELECT contact_id FROM contact_tag WHERE group_id = :groupId"
          + " AND tag_id IN (:hasTagIds) GROUP BY contact_id HAVING count(*) = :hasTagCount) ";

  /** None of the Tags (R-015-02). */
  private static final String WITHOUT_TAGS =
      "AND NOT EXISTS (SELECT 1 FROM contact_tag ct WHERE ct.contact_id = contact.id"
          + " AND ct.tag_id IN (:withoutTagIds)) ";

  /** An Attribute with the key, compared case-insensitively (R-015-03, BR-11). */
  private static final String ATTRIBUTE_KEY =
      "AND EXISTS (SELECT 1 FROM attribute a WHERE a.group_id = :groupId"
          + " AND a.contact_id = contact.id AND lower(a.key) = lower(:attributeKey)) ";

  /** An Attribute with the key and the value, both compared case-insensitively (R-015-03). */
  private static final String ATTRIBUTE_VALUE =
      "AND EXISTS (SELECT 1 FROM attribute a WHERE a.group_id = :groupId"
          + " AND a.contact_id = contact.id AND lower(a.key) = lower(:attributeKey)"
          + " AND lower(a.value) = lower(:attributeValue)) ";

  /** The company, compared case-insensitively (R-015-04). */
  private static final String COMPANY = "AND lower(company) = lower(:company) ";

  /** The country code (R-015-04). */
  private static final String COUNTRY = "AND country = :country ";

  @PersistenceContext private EntityManager entityManager;

  /**
   * One Page of the active Contacts of the Group whose search text matches the pattern and that
   * pass every filter (R-015-05).
   *
   * @param groupId the Group
   * @param pattern a LIKE pattern over the Normalised search text, {@code \} escaping
   * @param filters the Active filters, with Tag ids of this Group only
   * @param sort the sort key
   * @param descending whether the sort key runs Z–A; ties stay in name order
   * @param offset rows to skip
   * @param limit rows to return
   * @return the rows
   */
  public List<ContactListRow> findPage(
      UUID groupId,
      String pattern,
      ContactFilters filters,
      ContactSort sort,
      boolean descending,
      int offset,
      int limit) {
    Query query =
        filtered(
                groupId,
                COLUMNS,
                pattern,
                filters,
                null,
                "ORDER BY " + (descending ? DESCENDING : ASCENDING).get(sort) + PAGE)
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
   * The number of active Contacts of the Group of {@link #findPage}: the count of the filtered list
   * (R-015-07).
   *
   * @param groupId the Group
   * @param pattern the LIKE pattern of {@link #findPage}
   * @param filters the Active filters of {@link #findPage}
   * @return the total
   */
  public long count(UUID groupId, String pattern, ContactFilters filters) {
    return countExport(groupId, pattern, filters, null);
  }

  /**
   * How many of the Tags belong to the Group (EX-015-04).
   *
   * @param groupId the Group
   * @param tagIds the Tags, each once
   * @return the number of them in the Group
   */
  public long countTags(UUID groupId, List<UUID> tagIds) {
    if (tagIds.isEmpty()) {
      return 0;
    }
    Object found =
        entityManager
            .createNativeQuery(
                "SELECT count(*) FROM tag WHERE group_id = :groupId AND id IN (:ids)")
            .setParameter("groupId", groupId)
            .setParameter("ids", tagIds)
            .getSingleResult();
    return ((Number) found).longValue();
  }

  /**
   * The query of the Group's active Contacts that match the search and every set filter, with its
   * parameters bound.
   */
  private Query filtered(
      UUID groupId,
      String select,
      String pattern,
      ContactFilters filters,
      List<UUID> contactIds,
      String tail) {
    StringBuilder sql = new StringBuilder(select).append(WHERE);
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("groupId", groupId);
    parameters.put("pattern", pattern);
    if (contactIds != null) {
      sql.append(CHECKED);
      parameters.put("contactIds", contactIds);
    }
    if (!filters.hasTagIds().isEmpty()) {
      sql.append(HAS_TAGS);
      parameters.put("hasTagIds", filters.hasTagIds());
      parameters.put("hasTagCount", filters.hasTagIds().size());
    }
    if (!filters.withoutTagIds().isEmpty()) {
      sql.append(WITHOUT_TAGS);
      parameters.put("withoutTagIds", filters.withoutTagIds());
    }
    if (filters.attributeKey() != null) {
      sql.append(filters.attributeValue() == null ? ATTRIBUTE_KEY : ATTRIBUTE_VALUE);
      parameters.put("attributeKey", filters.attributeKey());
      if (filters.attributeValue() != null) {
        parameters.put("attributeValue", filters.attributeValue());
      }
    }
    if (filters.company() != null) {
      sql.append(COMPANY);
      parameters.put("company", filters.company());
    }
    if (filters.country() != null) {
      sql.append(COUNTRY);
      parameters.put("country", filters.country());
    }
    Query query = entityManager.createNativeQuery(sql.append(tail).toString());
    parameters.forEach(query::setParameter);
    return query;
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

  /**
   * The number of Contacts of {@link #findExport}: the Export scope (R-016-05, R-016-07).
   *
   * @param groupId the Group
   * @param pattern the LIKE pattern of the list
   * @param filters the Active filters of the list
   * @param contactIds the Checked contacts, at least one; {@code null} for the whole list
   * @return the size of the Export scope
   */
  public long countExport(
      UUID groupId, String pattern, ContactFilters filters, List<UUID> contactIds) {
    Object total =
        filtered(groupId, "SELECT count(*) FROM contact ", pattern, filters, contactIds, "")
            .getSingleResult();
    return ((Number) total).longValue();
  }

  /**
   * The Export scope with every Field: the active Contacts of the list, or only the Checked
   * contacts among them, in the list's sort order (R-016-01, R-016-02, BR-09, EX-016-07).
   *
   * @param groupId the Group
   * @param pattern the LIKE pattern of the list
   * @param filters the Active filters of the list
   * @param contactIds the Checked contacts, at least one; {@code null} for the whole list
   * @param sort the sort key of the list
   * @param descending whether the sort key runs Z–A
   * @return the rows
   */
  public List<ContactExportRow> findExport(
      UUID groupId,
      String pattern,
      ContactFilters filters,
      List<UUID> contactIds,
      ContactSort sort,
      boolean descending) {
    Query query =
        filtered(
            groupId,
            EXPORT_COLUMNS,
            pattern,
            filters,
            contactIds,
            "ORDER BY " + (descending ? DESCENDING : ASCENDING).get(sort));
    List<ContactExportRow> rows = new ArrayList<>();
    for (Object result : query.getResultList()) {
      Object[] row = (Object[]) result;
      rows.add(
          new ContactExportRow(
              (UUID) row[0],
              new ContactFields(
                  (String) row[1],
                  (String) row[2],
                  (String) row[3],
                  (String) row[4],
                  (String) row[5],
                  (String) row[6],
                  (String) row[7],
                  (String) row[8],
                  (String) row[9],
                  (String) row[10],
                  (String) row[11])));
    }
    return rows;
  }

  /**
   * The Attributes of the given Contacts of the Group, by key A–Z in Slovenian collation
   * (R-016-03).
   *
   * @param groupId the Group
   * @param contactIds the Contacts
   * @return one row per Attribute; none for Contacts of another Group
   */
  public List<ContactExportAttributeRow> findExportAttributes(UUID groupId, List<UUID> contactIds) {
    if (contactIds.isEmpty()) {
      return List.of();
    }
    Query query =
        entityManager
            .createNativeQuery(
                """
                SELECT contact_id, key, value FROM attribute
                WHERE group_id = :groupId AND contact_id IN (:contactIds)
                ORDER BY key COLLATE "sl-x-icu", id
                """)
            .setParameter("groupId", groupId)
            .setParameter("contactIds", contactIds);
    List<ContactExportAttributeRow> attributes = new ArrayList<>();
    for (Object result : query.getResultList()) {
      Object[] row = (Object[]) result;
      attributes.add(
          new ContactExportAttributeRow((UUID) row[0], (String) row[1], (String) row[2]));
    }
    return attributes;
  }
}
