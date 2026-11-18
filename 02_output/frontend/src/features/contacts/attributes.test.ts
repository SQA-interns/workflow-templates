import { describe, expect, it } from "vitest";
import { type AttributeRow, checkAttributes } from "./attributes";

function rows(...keyValues: string[]): AttributeRow[] {
  return keyValues.flatMap((key, i) =>
    i % 2 === 0 ? [{ key, value: keyValues.at(i + 1) ?? "" }] : [],
  );
}

function numbered(count: number): AttributeRow[] {
  return Array.from({ length: count }, (_, i) => ({ key: `kljuc${i + 1}`, value: "v" }));
}

describe("Attributes of a Contact (specification §3, R-008-02 … R-008-05)", () => {
  it("R-008-01 keys and values are trimmed and keep their spelling", () => {
    expect(checkAttributes(rows("  Kompetenca ", " spletnoProgramiranje ")).trimmed).toEqual([
      { key: "Kompetenca", value: "spletnoProgramiranje" },
    ]);
  });

  it("EX-008-01 a key of 50 characters is accepted, of 51 rejected", () => {
    expect(checkAttributes(rows("k".repeat(50), "v")).problems).toEqual([]);
    expect(checkAttributes(rows("k".repeat(51), "v")).problems).toEqual([
      { field: "attributes[0].key", code: "attribute.keyLength" },
    ]);
  });

  it("EX-008-02 a key of only spaces is rejected", () => {
    expect(checkAttributes(rows("   ", "v")).problems).toEqual([
      { field: "attributes[0].key", code: "attribute.keyLength" },
    ]);
  });

  it("EX-008-03 a value of 500 characters is accepted, of 501 rejected", () => {
    expect(checkAttributes(rows("opis", "v".repeat(500))).problems).toEqual([]);
    expect(checkAttributes(rows("opis", "v".repeat(501))).problems).toEqual([
      { field: "attributes[0].value", code: "attribute.valueTooLong" },
    ]);
  });

  it("EX-008-04 an empty value is accepted", () => {
    expect(checkAttributes(rows("kompetenca", "")).problems).toEqual([]);
  });

  it("AC-008-03 the same key ignoring case and spaces names the second key", () => {
    expect(checkAttributes(rows("kompetenca", "mobilno", " Kompetenca", "java")).problems).toEqual([
      { field: "attributes[1].key", code: "attribute.keyTwice", params: { key: "Kompetenca" } },
    ]);
  });

  it("EX-008-05 50 Attributes are accepted, 51 rejected", () => {
    expect(checkAttributes(numbered(50)).problems).toEqual([]);
    expect(checkAttributes(numbered(51)).problems).toEqual([
      { field: "attributes", code: "attribute.tooMany" },
    ]);
  });

  it("EX-008-01 a key of one character is accepted", () => {
    expect(checkAttributes(rows("k", "v")).problems).toEqual([]);
  });
});
