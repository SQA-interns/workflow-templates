import { beforeEach, describe, expect, it, vi } from "vitest";
import { initializeApp } from "firebase/app";
import {
  connectAuthEmulator,
  createUserWithEmailAndPassword,
  getAuth,
  GoogleAuthProvider,
  onAuthStateChanged,
  sendEmailVerification,
  signInWithEmailAndPassword,
  signInWithPopup,
  signOut,
} from "firebase/auth";
import { setTokenSource } from "../api/apiClient";
import { authErrorKey, toAccount } from "./firebase";

/** The Firebase SDK is replaced; the tests check what this module asks of it. */
const fakeApp = vi.hoisted(() => ({ name: "fake-app" }));
const fakeAuth = vi.hoisted(() => ({ currentUser: null as unknown }));

vi.mock("firebase/app", () => ({ initializeApp: vi.fn(() => fakeApp) }));
vi.mock("firebase/auth", () => ({
  connectAuthEmulator: vi.fn(),
  createUserWithEmailAndPassword: vi.fn(async () => ({ user: { uid: "new-user" } })),
  getAuth: vi.fn(() => fakeAuth),
  GoogleAuthProvider: vi.fn(),
  onAuthStateChanged: vi.fn(() => () => undefined),
  sendEmailVerification: vi.fn(async () => undefined),
  signInWithEmailAndPassword: vi.fn(async () => undefined),
  signInWithPopup: vi.fn(async () => undefined),
  signOut: vi.fn(async () => undefined),
}));
vi.mock("../api/apiClient", () => ({ setTokenSource: vi.fn() }));

const config = {
  firebaseApiKey: "fake-api-key",
  firebaseAuthDomain: "localhost",
  firebaseProjectId: "demo-contacts",
};

/** A fresh copy of the module, so each test starts with Firebase not started. */
async function freshModule() {
  vi.resetModules();
  return import("./firebase");
}

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

  it("an error that is no object, or has no text code, shows the generic error text", () => {
    expect(authErrorKey(null)).toBe("state.error");
    expect(authErrorKey(undefined)).toBe("state.error");
    expect(authErrorKey("auth/invalid-email")).toBe("state.error");
    expect(authErrorKey({ code: 42 })).toBe("state.error");
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

  it("R-001-05 a Google account that also has a password must verify its e-mail", () => {
    expect(
      toAccount({
        email: "ana.kovac@example.si",
        emailVerified: false,
        providerData: [{ providerId: "google.com" }, { providerId: "password" }],
      }).mustVerifyEmail,
    ).toBe(true);
  });
});

describe("Firebase Authentication start (ADR-03, ADR-05)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    fakeAuth.currentUser = null;
  });

  it("ADR-05 starts Firebase with the settings of this environment", async () => {
    const firebase = await freshModule();

    firebase.startAuth(config);

    expect(initializeApp).toHaveBeenCalledWith({
      apiKey: "fake-api-key",
      authDomain: "localhost",
      projectId: "demo-contacts",
    });
    expect(getAuth).toHaveBeenCalledWith(fakeApp);
    expect(connectAuthEmulator).not.toHaveBeenCalled();
  });

  it("ADR-05 connects to the Auth emulator when /config.json names one", async () => {
    const firebase = await freshModule();

    firebase.startAuth({ ...config, authEmulatorUrl: "http://localhost:9199" });

    expect(connectAuthEmulator).toHaveBeenCalledWith(fakeAuth, "http://localhost:9199", {
      disableWarnings: true,
    });
  });

  it("ADR-06 requests carry the ID token of the signed-in account, none when signed out", async () => {
    const firebase = await freshModule();
    firebase.startAuth(config);
    const source = vi.mocked(setTokenSource).mock.calls[0]?.[0];
    if (!source) throw new Error("no token source");

    expect(await source()).toBeNull();
    fakeAuth.currentUser = { getIdToken: vi.fn(async () => "id-token-1") };
    expect(await source()).toBe("id-token-1");
  });

  it("every call before the start fails with a clear message", async () => {
    const firebase = await freshModule();

    await expect(firebase.signInWithEmail("a@b.si", "geslo123")).rejects.toThrow(
      "Firebase Authentication is not started",
    );
    expect(() => firebase.onAccountChange(() => undefined)).toThrow(
      "Firebase Authentication is not started",
    );
    expect(signInWithEmailAndPassword).not.toHaveBeenCalled();
  });

  it("R-001-01 signs in with e-mail and password", async () => {
    const firebase = await freshModule();
    firebase.startAuth(config);

    await firebase.signInWithEmail("ana.kovac@example.si", "geslo123");

    expect(signInWithEmailAndPassword).toHaveBeenCalledWith(
      fakeAuth,
      "ana.kovac@example.si",
      "geslo123",
    );
  });

  it("R-001-05 signing up creates the account and sends the confirmation link", async () => {
    const firebase = await freshModule();
    firebase.startAuth(config);

    await firebase.signUpWithEmail("ana.kovac@example.si", "geslo123");

    expect(createUserWithEmailAndPassword).toHaveBeenCalledWith(
      fakeAuth,
      "ana.kovac@example.si",
      "geslo123",
    );
    expect(sendEmailVerification).toHaveBeenCalledWith({ uid: "new-user" });
  });

  it("R-001-01 signs in with Google in a popup", async () => {
    const firebase = await freshModule();
    firebase.startAuth(config);

    await firebase.signInWithGoogle();

    expect(signInWithPopup).toHaveBeenCalledWith(fakeAuth, expect.any(GoogleAuthProvider));
  });

  it("R-001-06 signs out of Firebase", async () => {
    const firebase = await freshModule();
    firebase.startAuth(config);

    await firebase.signOutAccount();

    expect(signOut).toHaveBeenCalledWith(fakeAuth);
  });

  it("R-001-01 calls back with the account after sign-in and with null after sign-out", async () => {
    const firebase = await freshModule();
    firebase.startAuth(config);
    const callback = vi.fn();

    firebase.onAccountChange(callback);
    const listener = vi.mocked(onAuthStateChanged).mock.calls[0]?.[1] as (user: unknown) => void;
    listener({ email: "a@b.si", emailVerified: true, providerData: [{ providerId: "password" }] });
    listener(null);

    expect(vi.mocked(onAuthStateChanged).mock.calls[0]?.[0]).toBe(fakeAuth);
    expect(callback.mock.calls).toEqual([[{ email: "a@b.si", mustVerifyEmail: false }], [null]]);
  });

  it("R-001-05 refreshing reloads the account with a fresh ID token", async () => {
    const firebase = await freshModule();
    firebase.startAuth(config);
    const user = {
      email: "ana.kovac@example.si",
      emailVerified: true,
      providerData: [{ providerId: "password" }],
      reload: vi.fn(async () => undefined),
      getIdToken: vi.fn(async () => "fresh"),
    };
    fakeAuth.currentUser = user;

    const account = await firebase.refreshAccount();

    expect(user.reload).toHaveBeenCalled();
    expect(user.getIdToken).toHaveBeenCalledWith(true);
    expect(account).toEqual({ email: "ana.kovac@example.si", mustVerifyEmail: false });
  });

  it("refreshing without a signed-in account gives no account", async () => {
    const firebase = await freshModule();
    firebase.startAuth(config);

    expect(await firebase.refreshAccount()).toBeNull();
  });
});
