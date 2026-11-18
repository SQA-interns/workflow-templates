import { afterEach, describe, expect, it, vi } from "vitest";
import { forgetReturnTo, rememberReturnTo, returnTo } from "./returnTo";

afterEach(() => {
  vi.restoreAllMocks();
  sessionStorage.clear();
});

describe("return to the Invitation after signing in (R-004-07)", () => {
  it("R-004-07 the opened Invitation is kept for this browser tab", () => {
    rememberReturnTo("/invitations/abc_DEF-123");

    expect(sessionStorage.getItem("contacts.returnTo")).toBe("/invitations/abc_DEF-123");
    expect(returnTo()).toBe("/invitations/abc_DEF-123");
  });

  it("R-004-07 a path that is no Invitation link is never kept", () => {
    rememberReturnTo("https://evil.example/invitations/abc");
    rememberReturnTo("/evil/invitations/abc");
    rememberReturnTo("/invitations/abc/../../groups");
    rememberReturnTo("/invitations/abc?next=https://evil.example");

    expect(sessionStorage.getItem("contacts.returnTo")).toBeNull();
    expect(returnTo()).toBe("/");
  });

  it("R-004-07 a stored value that is no Invitation link leads to the start screen", () => {
    sessionStorage.setItem("contacts.returnTo", "/evil/invitations/abc");
    expect(returnTo()).toBe("/");

    sessionStorage.setItem("contacts.returnTo", "/invitations/abc/evil");
    expect(returnTo()).toBe("/");
  });

  it("without a remembered Invitation the User starts at the start screen", () => {
    expect(returnTo()).toBe("/");
  });

  it("blocked storage leads to the start screen", () => {
    vi.spyOn(Storage.prototype, "getItem").mockImplementation(() => {
      throw new Error("blocked");
    });

    expect(returnTo()).toBe("/");
  });

  it("blocked storage does not stop remembering or forgetting", () => {
    vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
      throw new Error("blocked");
    });
    vi.spyOn(Storage.prototype, "removeItem").mockImplementation(() => {
      throw new Error("blocked");
    });

    expect(() => rememberReturnTo("/invitations/abc")).not.toThrow();
    expect(() => forgetReturnTo()).not.toThrow();
  });

  it("R-004-07 the Invitation is forgotten once it is shown", () => {
    rememberReturnTo("/invitations/abc");

    forgetReturnTo();

    expect(sessionStorage.getItem("contacts.returnTo")).toBeNull();
    expect(returnTo()).toBe("/");
  });
});
