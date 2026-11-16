import { describe, expect, it } from "vitest";
import { authErrorKey, toAccount } from "./firebase";

describe("Firebase errors → catalog keys (EX-001-01, EX-001-05)", () => {
  it("EX-001-01 wrong password and unknown e-mail give the same message", () => {
    const keys = [
      "auth/invalid-credential",
      "auth/wrong-password",
      "auth/user-not-found",
      "auth/invalid-login-credentials",
    ].map((code) => authErrorKey({ code }));

    expect(new Set(keys)).toEqual(new Set(["account.error.wrongCredentials"]));
  });

  it("EX-001-05 an e-mail that already has an account", () => {
    expect(authErrorKey({ code: "auth/email-already-in-use" })).toBe("account.error.emailInUse");
  });

  it("R-001-01 an invalid e-mail and a password below the Firebase minimum", () => {
    expect(authErrorKey({ code: "auth/invalid-email" })).toBe("account.error.emailInvalid");
    expect(authErrorKey({ code: "auth/weak-password" })).toBe("account.error.weakPassword");
  });

  it("SR-03 Firebase's own limit shows the rate limit text", () => {
    expect(authErrorKey({ code: "auth/too-many-requests" })).toBe("errors.rateLimited");
  });

  it("a closed Google window is no error", () => {
    expect(authErrorKey({ code: "auth/popup-closed-by-user" })).toBeNull();
    expect(authErrorKey({ code: "auth/cancelled-popup-request" })).toBeNull();
  });

  it("anything else shows the generic error text", () => {
    expect(authErrorKey({ code: "auth/network-request-failed" })).toBe("state.error");
    expect(authErrorKey(new Error("boom"))).toBe("state.error");
  });
});

describe("account of the Firebase user (R-001-05)", () => {
  it("R-001-05 an e-mail/password account must verify its e-mail", () => {
    const account = toAccount({
      email: "ana.kovac@example.si",
      emailVerified: false,
      providerData: [{ providerId: "password" }],
    });

    expect(account).toEqual({ email: "ana.kovac@example.si", mustVerifyEmail: true });
  });

  it("R-001-05 a verified account and a Google account need no verification", () => {
    expect(
      toAccount({
        email: "a@b.si",
        emailVerified: true,
        providerData: [{ providerId: "password" }],
      }).mustVerifyEmail,
    ).toBe(false);
    expect(
      toAccount({
        email: "marko.kranjc@gmail.com",
        emailVerified: false,
        providerData: [{ providerId: "google.com" }],
      }).mustVerifyEmail,
    ).toBe(false);
  });
});
