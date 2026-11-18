import { act, renderHook } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { useSelection } from "./useSelection";

describe("useSelection (R-016-02, D-24 (b))", () => {
  it("R-016-02 checks and unchecks Contacts one by one", () => {
    const { result } = renderHook(() => useSelection("q="));

    act(() => result.current.check("c1", true));
    act(() => result.current.check("c2", true));
    act(() => result.current.check("c1", false));

    expect(result.current.checked).toEqual(["c2"]);
    expect(result.current.isChecked("c2")).toBe(true);
    expect(result.current.isChecked("c1")).toBe(false);
  });

  it("R-016-02 checks or unchecks every Contact of the Page, keeping other Pages", () => {
    const { result } = renderHook(() => useSelection("q="));
    act(() => result.current.check("other-page", true));

    act(() => result.current.checkAll(["c1", "c2"], true));
    expect(result.current.checked).toEqual(["other-page", "c1", "c2"]);

    act(() => result.current.checkAll(["c1", "c2"], false));
    expect(result.current.checked).toEqual(["other-page"]);
  });

  it("R-016-02 checking a Page again does not count a checked Contact twice", () => {
    const { result } = renderHook(() => useSelection("q="));
    act(() => result.current.check("c1", true));

    act(() => result.current.checkAll(["c1", "c2"], true));

    expect(result.current.checked).toEqual(["c1", "c2"]);
  });

  it("D-24 (b) kept while the scope stays (Pages, sort) and cleared when it changes", () => {
    const { result, rerender } = renderHook(({ scope }) => useSelection(scope), {
      initialProps: { scope: "q=novak" },
    });
    act(() => result.current.check("c1", true));

    rerender({ scope: "q=novak" });
    expect(result.current.checked).toEqual(["c1"]);

    rerender({ scope: "q=zupan" });
    expect(result.current.checked).toEqual([]);
  });
});
