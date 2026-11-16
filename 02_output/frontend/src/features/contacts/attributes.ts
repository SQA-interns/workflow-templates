// The Attributes of a Contact and the checks of specification §3, repeated in the browser for
// inline errors (UI-13); the API checks them again (SB-01).

/** One Attribute as typed or sent (`Attribute` of the contract). */
export interface AttributeRow {
  key: string;
  value: string;
}

/** One problem of the Attributes: `attributes[i].key`, `attributes[i].value` or `attributes`. */
export interface AttributeProblem {
  field: string;
  code: string;
  params?: { key: string };
}

/** Longest key and value in characters, most Attributes per Contact (business-rules.md, Data model). */
const MAX_KEY_LENGTH = 50;
const MAX_VALUE_LENGTH = 500;
const MAX_ATTRIBUTES = 50;

function length(text: string): number {
  return [...text].length;
}

/** The trimmed Attributes and every problem with them (R-008-02 … R-008-05, R-006-07). */
export function checkAttributes(rows: AttributeRow[]): {
  trimmed: AttributeRow[];
  problems: AttributeProblem[];
} {
  const trimmed = rows.map((row) => ({ key: row.key.trim(), value: row.value.trim() }));
  const problems: AttributeProblem[] =
    trimmed.length > MAX_ATTRIBUTES ? [{ field: "attributes", code: "attribute.tooMany" }] : [];
  const keys = new Set<string>();
  trimmed.forEach(({ key, value }, index) => {
    const at = `attributes[${index}]`;
    if (length(key) < 1 || length(key) > MAX_KEY_LENGTH) {
      problems.push({ field: `${at}.key`, code: "attribute.keyLength" });
    } else if (keys.has(key.toLowerCase())) {
      problems.push({ field: `${at}.key`, code: "attribute.keyTwice", params: { key } });
    } else {
      keys.add(key.toLowerCase());
    }
    if (length(value) > MAX_VALUE_LENGTH) {
      problems.push({ field: `${at}.value`, code: "attribute.valueTooLong" });
    }
  });
  return { trimmed, problems };
}
