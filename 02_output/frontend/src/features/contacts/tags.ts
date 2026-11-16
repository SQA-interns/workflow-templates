// The Tag names of a Contact and the check of specification §3, repeated in the browser for an
// inline error (UI-13); the API checks them again and resolves the Group's spelling (SB-01).

/** Longest Tag name in characters (business-rules.md, Data model; R-009-05). */
const MAX_TAG_LENGTH = 50;

/** The trimmed name, or `tag.invalid` when it is blank, too long or has a comma (R-009-05). */
export function checkTagName(text: string): { name: string } | { problem: string } {
  const name = text.trim();
  const length = [...name].length;
  if (length < 1 || length > MAX_TAG_LENGTH || name.includes(",")) {
    return { problem: "tag.invalid" };
  }
  return { name };
}

function sameName(a: string, b: string): boolean {
  return a.toLowerCase() === b.toLowerCase();
}

/**
 * The Tags with the name added once (R-009-06), in the spelling of a suggested Tag of the Group
 * that equals it case-insensitively (R-009-03, BR-11), else as entered (R-009-04).
 */
export function addTag(tags: string[], name: string, suggested: string[]): string[] {
  if (tags.some((tag) => sameName(tag, name))) return tags;
  return [...tags, suggested.find((tag) => sameName(tag, name)) ?? name];
}
