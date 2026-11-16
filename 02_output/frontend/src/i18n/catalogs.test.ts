import { describe, expect, it } from "vitest";
import en from "./en.json";
import sl from "./sl.json";

function entries(value: unknown, prefix = ""): [string, unknown][] {
  if (typeof value !== "object" || value === null) return [[prefix, value]];
  return Object.entries(value).flatMap(([k, v]) => entries(v, prefix ? `${prefix}.${k}` : k));
}

describe("message catalogs (NFR-08, UI-30)", () => {
  it("NFR-08 both catalogs have exactly the same keys", () => {
    const keysOf = (catalog: unknown) =>
      entries(catalog)
        .map(([key]) => key)
        .sort();
    expect(keysOf(sl)).toEqual(keysOf(en));
  });

  it("NFR-08 no text is empty in either catalog", () => {
    for (const [key, text] of [...entries(sl), ...entries(en)]) {
      expect(text, key).toEqual(expect.stringMatching(/\S/));
    }
  });
});
