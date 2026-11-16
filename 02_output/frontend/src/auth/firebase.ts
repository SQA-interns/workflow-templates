import { initializeApp } from "firebase/app";
import {
  type Auth,
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
import type { RuntimeConfig } from "./runtimeConfig";

/** The signed-in Firebase account as the screens need it (R-001-01, R-001-05). */
export interface Account {
  email: string | null;
  /** An e-mail/password account whose e-mail is not verified yet (R-001-05, D-16). */
  mustVerifyEmail: boolean;
}

interface FirebaseUserLike {
  email: string | null;
  emailVerified: boolean;
  providerData: { providerId: string }[];
}

let auth: Auth | undefined;

/** Starts Firebase Authentication for this environment; requests then carry its ID token. */
export function startAuth(config: RuntimeConfig): void {
  const app = initializeApp({
    apiKey: config.firebaseApiKey,
    authDomain: config.firebaseAuthDomain,
    projectId: config.firebaseProjectId,
  });
  auth = getAuth(app);
  if (config.authEmulatorUrl) {
    connectAuthEmulator(auth, config.authEmulatorUrl, { disableWarnings: true });
  }
  setTokenSource(async () => (await requireAuth().currentUser?.getIdToken()) ?? null);
}

function requireAuth(): Auth {
  if (!auth) throw new Error("Firebase Authentication is not started");
  return auth;
}

export function toAccount(user: FirebaseUserLike): Account {
  const usesPassword = user.providerData.some((p) => p.providerId === "password");
  return { email: user.email, mustVerifyEmail: usesPassword && !user.emailVerified };
}

/** Calls back with the account after every sign-in and sign-out. */
export function onAccountChange(callback: (account: Account | null) => void): () => void {
  return onAuthStateChanged(requireAuth(), (user) => callback(user ? toAccount(user) : null));
}

export async function signInWithEmail(email: string, password: string): Promise<void> {
  await signInWithEmailAndPassword(requireAuth(), email, password);
}

/** Creates the account and sends the link that confirms the e-mail (R-001-01, R-001-05). */
export async function signUpWithEmail(email: string, password: string): Promise<void> {
  const credential = await createUserWithEmailAndPassword(requireAuth(), email, password);
  await sendEmailVerification(credential.user);
}

export async function signInWithGoogle(): Promise<void> {
  await signInWithPopup(requireAuth(), new GoogleAuthProvider());
}

/** Signs out; Firebase discards the stored ID token (R-001-06). */
export async function signOutAccount(): Promise<void> {
  await signOut(requireAuth());
}

/** Reads the account again, e.g. after the e-mail was confirmed, with a fresh ID token. */
export async function refreshAccount(): Promise<Account | null> {
  const user = requireAuth().currentUser;
  if (!user) return null;
  await user.reload();
  await user.getIdToken(true);
  return toAccount(user);
}

const errorKeys = new Map<string, string | null>(
  Object.entries({
    "auth/invalid-credential": "account.error.wrongCredentials",
    "auth/invalid-login-credentials": "account.error.wrongCredentials",
    "auth/wrong-password": "account.error.wrongCredentials",
    "auth/user-not-found": "account.error.wrongCredentials",
    "auth/email-already-in-use": "account.error.emailInUse",
    "auth/invalid-email": "account.error.emailInvalid",
    "auth/weak-password": "account.error.weakPassword",
    "auth/too-many-requests": "errors.rateLimited",
    "auth/popup-closed-by-user": null,
    "auth/cancelled-popup-request": null,
  }),
);

/** The catalog key of a Firebase error; null when there is nothing to show (EX-001-01, EX-001-05). */
export function authErrorKey(error: unknown): string | null {
  const code = typeof error === "object" && error !== null && "code" in error ? error.code : "";
  const key = typeof code === "string" ? errorKeys.get(code) : undefined;
  return key === undefined ? "state.error" : key;
}
