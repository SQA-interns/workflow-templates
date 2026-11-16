package app.contacts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** A record about a person or a company kept in one Group (glossary "Contact", table contact). */
@Entity
@Table(name = "contact")
public class Contact implements GroupOwned {

  /** State of a Contact in use (Data model). */
  public static final String ACTIVE = "active";

  /** State of a Contact in the recycle bin (Data model, US-012). */
  public static final String IN_RECYCLE_BIN = "inRecycleBin";

  @Id private UUID id;

  @Column(name = "group_id", nullable = false, updatable = false)
  private UUID groupId;

  @Embedded private ContactFields fields;

  @Column(nullable = false)
  private String state;

  @Column(name = "version_number", nullable = false)
  private int versionNumber;

  @Column(name = "search_text", nullable = false)
  private String searchText;

  @Column(name = "sort_name", nullable = false)
  private String sortName;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "created_by", nullable = false, updatable = false)
  private UUID createdBy;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "updated_by", nullable = false)
  private UUID updatedBy;

  /** For JPA. */
  protected Contact() {}

  /**
   * A new active Contact at Version 1 (R-006-01).
   *
   * @param groupId the Group
   * @param fields the checked Fields
   * @param searchText the search text of the Fields
   * @param sortName the sort name of the Fields
   * @param userId the User who adds it
   * @param now the time
   * @return the new, unsaved Contact
   */
  public static Contact create(
      UUID groupId,
      ContactFields fields,
      String searchText,
      String sortName,
      UUID userId,
      Instant now) {
    Contact contact = new Contact();
    contact.id = UUID.randomUUID();
    contact.groupId = groupId;
    contact.fields = fields;
    contact.state = ACTIVE;
    contact.versionNumber = 1;
    contact.searchText = searchText;
    contact.sortName = sortName;
    contact.createdAt = now;
    contact.createdBy = userId;
    contact.updatedAt = now;
    contact.updatedBy = userId;
    return contact;
  }

  /**
   * Stores the checked Fields as the next Version (R-007-01).
   *
   * @param fields the checked Fields
   * @param searchText the search text of the Fields
   * @param sortName the sort name of the Fields
   * @param userId the User who saves them
   * @param now the time
   */
  public void edit(
      ContactFields fields, String searchText, String sortName, UUID userId, Instant now) {
    this.fields = fields;
    this.searchText = searchText;
    this.sortName = sortName;
    this.versionNumber++;
    this.updatedAt = now;
    this.updatedBy = userId;
  }

  public UUID getId() {
    return id;
  }

  @Override
  public UUID getGroupId() {
    return groupId;
  }

  public ContactFields getFields() {
    return fields;
  }

  public String getState() {
    return state;
  }

  public int getVersionNumber() {
    return versionNumber;
  }

  public String getSearchText() {
    return searchText;
  }

  public String getSortName() {
    return sortName;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public UUID getUpdatedBy() {
    return updatedBy;
  }
}
