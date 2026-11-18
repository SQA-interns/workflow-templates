import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { ApiError } from "../../api/problem";
import i18n from "../../i18n";
import { problemsOfError, problemText } from "./fieldMessages";

const tooLong = { field: "note", code: "field.tooLong", params: { max: 2000 } } as const;

beforeEach(async () => {
  await i18n.changeLanguage("en");
});
afterEach(async () => {
  await i18n.changeLanguage("sl");
});

describe("texts of Field problems (specification §5)", () => {
  it("R-006-03 field.tooLong names the Field and its maximum in the language (UI-18)", async () => {
    expect(problemText(i18n.t, "en", tooLong)).toBe("Note may have at most 2,000 characters.");
    await i18n.changeLanguage("sl");
    expect(problemText(i18n.t, "sl", tooLong)).toBe("Opomba ima lahko največ 2000 znakov.");
  });

  it("R-006-02 R-006-04 D-23 the other codes show their catalog text", () => {
    expect(problemText(i18n.t, "en", { field: "name", code: "contact.nameRequired" })).toBe(
      "Enter a first name, last name or company.",
    );
    expect(problemText(i18n.t, "en", { field: "email", code: "field.emailInvalid" })).toBe(
      "Enter a valid e-mail address.",
    );
    expect(problemText(i18n.t, "en", { field: "phone", code: "field.phoneInvalid" })).toBe(
      "Enter a valid phone number.",
    );
  });

  it("SB-01 the Field, Attribute and Tag problems of a 400 answer go to their places", () => {
    const error = new ApiError(400, {
      code: "validation",
      errors: [
        { field: "note", code: "field.tooLong", params: { field: "note", max: 2000 } },
        { field: "attributes", code: "attribute.tooMany" },
        { field: "tags[1]", code: "tag.invalid" },
      ],
    });

    expect(problemsOfError(error)).toEqual({
      fields: [tooLong],
      attributes: [{ field: "attributes", code: "attribute.tooMany" }],
      tags: ["tag.invalid"],
      general: null,
      params: {},
    });
  });

  it("BR-03 a problem without Fields is one general text", () => {
    expect(problemsOfError(new ApiError(403, { code: "auth.forbidden" }))).toEqual({
      fields: [],
      attributes: [],
      tags: [],
      general: "errors.auth.forbidden",
      params: {},
    });
  });

  it("BR-06 a conflict keeps the values its text names", () => {
    const conflict = new ApiError(409, {
      code: "version.conflict",
      params: { changedBy: "Marko Kranjc" },
    });

    expect(problemsOfError(conflict)).toEqual({
      fields: [],
      attributes: [],
      tags: [],
      general: "errors.version.conflict",
      params: { changedBy: "Marko Kranjc" },
    });
  });

  it("UI-14 a failure without a problem shows the generic error text", () => {
    expect(problemsOfError(new TypeError("Failed to fetch"))).toEqual({
      fields: [],
      attributes: [],
      tags: [],
      general: "state.error",
      params: {},
    });
  });

  it("R-006-03 field.tooLong without a maximum shows the catalog text", () => {
    expect(problemText(i18n.t, "en", { field: "note", code: "field.tooLong" })).toBe(
      i18n.t("errors.field.tooLong"),
    );
  });

  it("D-23 another code with a maximum still shows its own text", () => {
    expect(
      problemText(i18n.t, "en", {
        field: "phone",
        code: "field.phoneInvalid",
        params: { max: 30 },
      }),
    ).toBe("Enter a valid phone number.");
  });

  it("SB-01 a name problem and a Tag problem of a 400 answer go to their places", () => {
    const error = new ApiError(400, {
      code: "validation",
      errors: [
        { field: "name", code: "contact.nameRequired" },
        { field: "tags[0]", code: "tag.invalid" },
      ],
    });

    expect(problemsOfError(error)).toStrictEqual({
      fields: [{ field: "name", code: "contact.nameRequired", params: undefined }],
      attributes: [],
      tags: ["tag.invalid"],
      general: null,
      params: {},
    });
  });

  it("R-008-04 an Attribute problem keeps the key its text names, and only a text key", () => {
    const error = new ApiError(400, {
      code: "validation",
      errors: [
        { field: "attributes[1].key", code: "attribute.keyTwice", params: { key: "Kompetenca" } },
        { field: "attributes[2].key", code: "attribute.keyTwice", params: { key: 7 } },
      ],
    });

    expect(problemsOfError(error).attributes).toStrictEqual([
      { field: "attributes[1].key", code: "attribute.keyTwice", params: { key: "Kompetenca" } },
      { field: "attributes[2].key", code: "attribute.keyTwice", params: undefined },
    ]);
  });

  it("R-006-03 a Field problem keeps only a numeric maximum", () => {
    const error = new ApiError(400, {
      code: "validation",
      errors: [{ field: "note", code: "field.tooLong", params: { max: "2000" } }],
    });

    expect(problemsOfError(error).fields).toStrictEqual([
      { field: "note", code: "field.tooLong", params: undefined },
    ]);
  });
});
