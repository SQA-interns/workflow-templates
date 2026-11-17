package app.contacts.service;

/**
 * One changed item of a Version (contract {@code Change}).
 *
 * @param item the Field name, {@code attribute:<key>} or {@code tag:<name>}
 * @param oldValue the value before; {@code null} for none
 * @param newValue the value after; {@code null} for none
 */
public record ChangeView(String item, String oldValue, String newValue) {}
