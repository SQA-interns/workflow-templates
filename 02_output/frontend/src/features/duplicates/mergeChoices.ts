import type { Contact } from "../../api/contacts";
import type { Choice } from "../../api/duplicates";
import type { AttributeRow } from "../contacts/attributes";
import { FIELD_NAMES, type FieldName } from "../contacts/contactFields";

/** The value of one Field of the Contact; null when empty. */
export function fieldValue(contact: Contact, name: FieldName): string | null {
  return new Map(Object.entries(contact.fields)).get(name) ?? null;
}

/** The Fields of the Merge screen: those with a value in either Contact. */
export function shownFields(primary: Contact, other: Contact): FieldName[] {
  return FIELD_NAMES.filter((name) => fieldValue(primary, name) || fieldValue(other, name));
}

/**
 * The preselected choice per shown Field: the Primary contact's value if non-blank, else the Other
 * contact's (R-018-01).
 */
export function preselected(primary: Contact, other: Contact): Record<string, Choice> {
  return Object.fromEntries(
    shownFields(primary, other).map((name) => [
      name,
      fieldValue(primary, name) ? "primary" : "other",
    ]),
  );
}

/** One Attribute key of the merged Contact; {@code other} is set when the values differ. */
export interface MergedAttribute {
  key: string;
  value: string;
  other?: string;
}

/**
 * The Attributes of both, one per key ignoring case, the Primary contact's spelling first; a key
 * with two different values needs a choice (R-018-03), the same value is kept once (EX-018-01).
 */
export function mergedAttributes(primary: AttributeRow[], other: AttributeRow[]) {
  const byKey = new Map<string, MergedAttribute>();
  for (const { key, value } of primary) byKey.set(key.toLowerCase(), { key, value });
  for (const { key, value } of other) {
    const kept = byKey.get(key.toLowerCase());
    if (!kept) byKey.set(key.toLowerCase(), { key, value });
    else if (kept.value !== value) kept.other = value;
  }
  return [...byKey.values()];
}

/** The Tag names of both, each once ignoring case (EX-018-07). */
export function mergedTags(primary: Contact, other: Contact): string[] {
  const byName = new Map<string, string>();
  for (const { name } of [...primary.tags, ...other.tags]) {
    if (!byName.has(name.toLowerCase())) byName.set(name.toLowerCase(), name);
  }
  return [...byName.values()];
}
