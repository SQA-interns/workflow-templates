import { describe, expect, it } from "vitest";
import i18n from "../i18n";
import { ApiError, messageKey, toApiError } from "./problem";

describe("problem codes → catalog (ADR-06, UI-30)", () => {
  it("R-001-05 the code auth.emailNotVerified has the story's text", () => {
    const error = new ApiError(403, { code: "auth.emailNotVerified" });

    expect(i18n.getFixedT("en")(messageKey(error.code))).toBe(
      "Confirm your e-mail address: we sent you a link.",
    );
  });

  it("BR-02 notFound and SR-03 rateLimited have their §5 texts", () => {
    const en = i18n.getFixedT("en");
    expect(en(messageKey("notFound"))).toBe("This page cannot be found.");
    expect(en(messageKey("rateLimited"))).toBe("Too many requests. Try again in a minute.");
    expect(en(messageKey("auth.forbidden"))).toBe("You are not allowed to do this.");
  });

  it("an unknown code falls back to the generic error text", () => {
    expect(i18n.getFixedT("en")(messageKey("no.such.code"))).toBe("Something went wrong.");
  });

  it("keeps params and field errors of the problem", () => {
    const error = new ApiError(400, {
      code: "validation",
      errors: [{ field: "firstName", code: "field.tooLong", params: { max: 100 } }],
    });

    expect(error.errors[0]).toEqual({
      field: "firstName",
      code: "field.tooLong",
      params: { max: 100 },
    });
    expect(error.params).toEqual({});
  });

  it("R-021-03 a problem naming a deleted User names Deleted user", async () => {
    await i18n.changeLanguage("en");
    const error = new ApiError(409, {
      code: "version.conflict",
      params: { changedBy: "user.deleted" },
    });

    expect(i18n.t(messageKey(error.code), error.params)).toBe(
      "Contact was changed by Deleted user, reload",
    );
    await i18n.changeLanguage("sl");
  });

  it("is an Error named ApiError with the code as its message", () => {
    const error = new ApiError(404, { code: "notFound" });

    expect(error).toBeInstanceOf(Error);
    expect(error.name).toBe("ApiError");
    expect(error.message).toBe("notFound");
    expect(new ApiError(500, {}).message).toBe("internal");
  });

  it("SB-07 an answer that is not JSON is not read as a problem", async () => {
    const response = new Response("<html>Bad Gateway</html>", {
      status: 502,
      headers: { "Content-Type": "text/html" },
    });

    const error = await toApiError(response);

    expect(error.status).toBe(502);
    expect(error.code).toBe("internal");
  });
});
