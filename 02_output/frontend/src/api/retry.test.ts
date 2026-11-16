import { describe, expect, it } from "vitest";
import { ApiError } from "./problem";
import { retryUnlessClientError } from "./retry";

describe("asking again after a failure (UI-14)", () => {
  it("BR-02 a 404 or another client error is final: not asked again", () => {
    expect(retryUnlessClientError(0, new ApiError(404, { code: "notFound" }))).toBe(false);
    expect(retryUnlessClientError(0, new ApiError(400, { code: "validation" }))).toBe(false);
  });

  it("UI-14 a server or network failure is asked again up to three times", () => {
    expect(retryUnlessClientError(0, new ApiError(500, { code: "internal" }))).toBe(true);
    expect(retryUnlessClientError(2, new TypeError("Failed to fetch"))).toBe(true);
    expect(retryUnlessClientError(3, new TypeError("Failed to fetch"))).toBe(false);
  });
});
