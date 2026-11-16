import { createContext, type ReactNode, useCallback, useContext, useEffect, useState } from "react";
import { type Account, onAccountChange, refreshAccount, startAuth } from "./firebase";
import { loadRuntimeConfig } from "./runtimeConfig";

/** Where the browser stands with Firebase Authentication (ADR-03). */
export type AuthState =
  | { status: "loading" }
  | { status: "failed" }
  | { status: "signedOut" }
  | { status: "signedIn"; account: Account };

interface AuthContextValue {
  state: AuthState;
  /** Reads the account again, e.g. after the e-mail was confirmed (R-001-05). */
  refresh: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

let started: Promise<void> | undefined;

/** Starts Firebase once per page, from the settings of this environment (ADR-05). */
function startOnce(): Promise<void> {
  started ??= loadRuntimeConfig().then(startAuth);
  return started;
}

function stateOf(account: Account | null): AuthState {
  return account ? { status: "signedIn", account } : { status: "signedOut" };
}

/** Keeps the signed-in account for the screens (R-001-01, R-001-06). */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>({ status: "loading" });

  useEffect(() => {
    let unsubscribe: (() => void) | undefined;
    let active = true;
    startOnce().then(
      () => {
        if (active) unsubscribe = onAccountChange((account) => setState(stateOf(account)));
      },
      () => setState({ status: "failed" }),
    );
    return () => {
      active = false;
      unsubscribe?.();
    };
  }, []);

  const refresh = useCallback(async () => setState(stateOf(await refreshAccount())), []);

  return <AuthContext.Provider value={{ state, refresh }}>{children}</AuthContext.Provider>;
}

/** The account state and its refresh; only inside AuthProvider. */
export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext);
  if (!value) throw new Error("useAuth outside AuthProvider");
  return value;
}
