import { describe, expect, it } from "vitest";
import type { Contact } from "../../api/contacts";
import { emptyFields } from "../contacts/contactFields";
import { mergedAttributes, mergedTags, primaryFirst } from "./mergeChoices";

function contact(id: string, createdAt: string, tags: string[] = []): Contact {
  return {
    id,
    versionNumber: 1,
    state: "active",
    fields: { ...emptyFields(), firstName: id },
    attributes: [],
    tags: tags.map((name, index) => ({ id: `${id}-${index}`, name })),
    createdAt,
    updatedAt: createdAt,
    mergedFrom: [],
    mergedInto: null,
  } as unknown as Contact;
}

describe("mergeChoices (US-018)", () => {
  it("D-74 on the same creation time the pair keeps the order given", () => {
    const a = contact("a", "2026-03-01T09:00:00Z");
    const b = contact("b", "2026-03-01T09:00:00Z");

    expect(primaryFirst(a, b)).toEqual([a, b]);
    expect(primaryFirst(b, a)).toEqual([b, a]);
  });

  it("R-018-03 keeps an Attribute only the Other contact has", () => {
    expect(
      mergedAttributes([{ key: "kompetenca", value: "java" }], [{ key: "jezik", value: "sl" }]),
    ).toEqual([
      { key: "kompetenca", value: "java" },
      { key: "jezik", value: "sl" },
    ]);
  });

  it("EX-018-01 the same value under a key in another case is kept once without a choice", () => {
    expect(
      mergedAttributes([{ key: "Jezik", value: "sl" }], [{ key: "jezik", value: "sl" }]),
    ).toEqual([{ key: "Jezik", value: "sl" }]);
  });

  it("R-018-03 keys of the Other contact are compared ignoring case among themselves", () => {
    expect(
      mergedAttributes(
        [],
        [
          { key: "Jezik", value: "sl" },
          { key: "jezik", value: "en" },
        ],
      ),
    ).toEqual([{ key: "Jezik", value: "sl", other: "en" }]);
  });

  it("EX-018-07 a Tag of both in another case is kept once, the Primary contact's spelling", () => {
    const primary = contact("a", "2026-03-01T09:00:00Z", ["VIP", "java"]);
    const other = contact("b", "2026-03-02T09:00:00Z", ["vip", "Python"]);

    expect(mergedTags(primary, other)).toEqual(["VIP", "java", "Python"]);
  });
});
