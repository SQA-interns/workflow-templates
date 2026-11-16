import { describe, expect, it } from "vitest";
import { addTag, checkTagName } from "./tags";

describe("Tags of a Contact (specification §3, R-009-03 … R-009-06)", () => {
  it("R-009-05 a name is trimmed and keeps its spelling", () => {
    expect(checkTagName("  udeležba ")).toEqual({ name: "udeležba" });
  });

  it("EX-009-01 a name of 50 characters is accepted, of 51 rejected", () => {
    expect(checkTagName("t".repeat(50))).toEqual({ name: "t".repeat(50) });
    expect(checkTagName("t".repeat(51))).toEqual({ problem: "tag.invalid" });
  });

  it("EX-009-01 characters are counted, not UTF-16 units", () => {
    expect(checkTagName("😀".repeat(50))).toEqual({ name: "😀".repeat(50) });
  });

  it("EX-009-02 a name of only spaces is rejected", () => {
    expect(checkTagName("   ")).toEqual({ problem: "tag.invalid" });
  });

  it("AC-009-05 a name with a comma is rejected", () => {
    expect(checkTagName("vip, 2025")).toEqual({ problem: "tag.invalid" });
  });

  it("R-009-03 a name equal to a suggested Tag case-insensitively takes its spelling", () => {
    expect(addTag([], "Udelezenec2025", ["udelezenec2025"])).toEqual(["udelezenec2025"]);
  });

  it("R-009-04 a new name is added as entered", () => {
    expect(addTag(["vip"], "udeležba", [])).toEqual(["vip", "udeležba"]);
  });

  it("EX-009-03 a name the Contact already has is not added again", () => {
    expect(addTag(["vip"], "VIP", [])).toEqual(["vip"]);
  });
});
