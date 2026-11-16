import { afterEach, describe, expect, it, vi } from "vitest";
import { lastGroupId, rememberGroup, startGroupId } from "./currentGroup";

const konferenca = { id: "g1", name: "Konferenca 2025", role: "OWNER" as const };
const alfa = { id: "g2", name: "Podjetje Alfa", role: "OWNER" as const };

afterEach(() => {
  localStorage.clear();
  vi.restoreAllMocks();
});

describe("current Group (R-002-05, D-22)", () => {
  it("R-002-05 the Group selected last is kept per User", () => {
    rememberGroup("ana", "g2");
    rememberGroup("marko", "g9");

    expect(lastGroupId("ana")).toBe("g2");
    expect(lastGroupId("marko")).toBe("g9");
    expect(lastGroupId("petra")).toBeNull();
  });

  it("AC-002-05 signing in opens the Group selected last", () => {
    expect(startGroupId([konferenca, alfa], "g2")).toBe("g2");
  });

  it("EX-002-06 a Group no longer available opens the first Group of the switcher", () => {
    expect(startGroupId([konferenca, alfa], "gone")).toBe("g1");
    expect(startGroupId([konferenca, alfa], null)).toBe("g1");
  });

  it("EX-002-06 without Groups there is no Group to open", () => {
    expect(startGroupId([], "g2")).toBeNull();
  });

  it("R-002-05 blocked browser storage forgets the choice instead of failing", () => {
    vi.spyOn(Storage.prototype, "getItem").mockImplementation(() => {
      throw new Error("blocked");
    });
    vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
      throw new Error("blocked");
    });

    expect(() => rememberGroup("ana", "g2")).not.toThrow();
    expect(lastGroupId("ana")).toBeNull();
  });
});
