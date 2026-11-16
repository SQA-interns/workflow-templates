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

  it("SB-01 the Field problems of a 400 answer go to their Fields", () => {
    const error = new ApiError(400, {
      code: "validation",
      errors: [
        { field: "note", code: "field.tooLong", params: { field: "note", max: 2000 } },
        { field: "attributes", code: "attribute.tooMany" },
      ],
    });

    expect(problemsOfError(error)).toEqual({ fields: [tooLong], general: null });
  });

  it("BR-03 a problem without Fields is one general text", () => {
    expect(problemsOfError(new ApiError(403, { code: "auth.forbidden" }))).toEqual({
      fields: [],
      general: "errors.auth.forbidden",
    });
  });

  it("UI-14 a failure without a problem shows the generic error text", () => {
    expect(problemsOfError(new TypeError("Failed to fetch"))).toEqual({
      fields: [],
      general: "state.error",
    });
  });
});
