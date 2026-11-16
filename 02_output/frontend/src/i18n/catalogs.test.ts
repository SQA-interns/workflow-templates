import { describe, expect, it } from "vitest";
import en from "./en.json";
import sl from "./sl.json";

function entries(value: unknown, prefix = ""): [string, unknown][] {
  if (typeof value !== "object" || value === null) return [[prefix, value]];
  return Object.entries(value).flatMap(([k, v]) => entries(v, prefix ? `${prefix}.${k}` : k));
}

/** i18next plural suffixes; each language has its own set (Slovenian also _two and _few). */
const PLURAL = /_(zero|one|two|few|many|other)$/;

describe("message catalogs (NFR-08, UI-30)", () => {
  it("NFR-08 both catalogs have exactly the same messages", () => {
    const keysOf = (catalog: unknown) =>
      [...new Set(entries(catalog).map(([key]) => key.replace(PLURAL, "")))].sort();
    expect(keysOf(sl)).toEqual(keysOf(en));
  });

  it("UI-18 every plural message has the plural forms of its language", () => {
    for (const [catalog, language] of [
      [sl, "sl"],
      [en, "en"],
    ] as const) {
      const plural = entries(catalog)
        .map(([key]) => key)
        .filter((key) => PLURAL.test(key));
      const forms = new Intl.PluralRules(language).resolvedOptions().pluralCategories;
      for (const base of new Set(plural.map((key) => key.replace(PLURAL, "")))) {
        for (const form of forms) expect(plural, language).toContain(`${base}_${form}`);
      }
    }
  });

  it("NFR-08 no text is empty in either catalog", () => {
    for (const [key, text] of [...entries(sl), ...entries(en)]) {
      expect(text, key).toEqual(expect.stringMatching(/\S/));
    }
  });
});

describe("sign-in wording (D-37)", () => {
  it("D-37 the Google button names the brand Google in every language", () => {
    expect(sl.account.google).toContain("Google");
    expect(en.account.google).toContain("Google");
  });
});
