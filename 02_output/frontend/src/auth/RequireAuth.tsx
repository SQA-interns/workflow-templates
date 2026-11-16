import type { ReactNode } from "react";
import { useTranslation } from "react-i18next";
import { Navigate } from "react-router";
import { type AuthState, useAuth } from "./AuthProvider";

/** Loading and error states while Firebase starts (UI-14); null once the account is known. */
function Pending({ state }: { state: AuthState }) {
  const { t } = useTranslation();
  if (state.status === "loading") return <p role="status">{t("state.loading")}</p>;
  if (state.status === "failed") return <p role="alert">{t("state.error")}</p>;
  return null;
}

/** Only for a signed-in User with a confirmed e-mail (R-001-03, R-001-05, R-001-06). */
export function RequireAuth({ children }: { children: ReactNode }) {
  const { state } = useAuth();
  if (state.status === "signedOut") return <Navigate to="/sign-in" replace />;
  if (state.status !== "signedIn") return <Pending state={state} />;
  if (state.account.mustVerifyEmail) return <Navigate to="/verify-email" replace />;
  return children;
}

/** Sign-in and sign-up: a signed-in User goes on to the product (R-001-01). */
export function GuestOnly({ children }: { children: ReactNode }) {
  const { state } = useAuth();
  if (state.status === "signedIn") {
    return <Navigate to={state.account.mustVerifyEmail ? "/verify-email" : "/"} replace />;
  }
  if (state.status !== "signedOut") return <Pending state={state} />;
  return children;
}
