package app.contacts.domain;

import java.util.UUID;

/**
 * One Contact of the Export scope with every Field (R-016-01, R-016-03).
 *
 * @param id the Contact id
 * @param fields the Fields in Data-model order
 */
public record ContactExportRow(UUID id, ContactFields fields) {}
