import { describe, expect, it } from "vitest";
import { ApiError } from "../../api/problem";
import { NAME_LENGTH_KEY, nameErrorKeyOf, nameIsValid } from "./groupName";

describe("Group name rule (R-002-02)", () => {
  it("R-002-02 accepts 1 to 100 characters, counted as characters", () => {
    expect(nameIsValid("")).toBe(false);
    expect(nameIsValid("K")).toBe(true);
    expect(nameIsValid("K".repeat(100))).toBe(true);
    expect(nameIsValid("K".repeat(101))).toBe(false);
    expect(nameIsValid("😀".repeat(100))).toBe(true);
  });

  it("R-002-02 the server's name rule maps to the name text", () => {
    const error = new ApiError(400, {
      code: "validation",
      errors: [{ field: "name", code: "group.nameLength" }],
    });

    expect(nameErrorKeyOf(error)).toBe(NAME_LENGTH_KEY);
  });

  it("UI-13 another field problem shows the problem's own text", () => {
    const error = new ApiError(400, {
      code: "field.tooLong",
      errors: [{ field: "other", code: "field.tooLong" }],
    });

    expect(nameErrorKeyOf(error)).toBe("errors.field.tooLong");
  });

  it("UI-13 a failure without a problem is the general error", () => {
    expect(nameErrorKeyOf(new Error("offline"))).toBe("state.error");
  });
});
