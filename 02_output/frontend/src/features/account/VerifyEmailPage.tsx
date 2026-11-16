import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Navigate } from "react-router";
import { Button } from "@/components/ui/button";
import { useAuth } from "../../auth/AuthProvider";
import { signOutAccount } from "../../auth/firebase";
import { AccountCard } from "./AccountCard";

/** WF-03 Confirm your e-mail: shown until the link in the e-mail was used (R-001-05, D-16). */
export function VerifyEmailPage() {
  const { t } = useTranslation();
  const { state, refresh } = useAuth();
  const [busy, setBusy] = useState(false);

  if (state.status === "signedOut") return <Navigate to="/sign-in" replace />;
  if (state.status === "signedIn" && !state.account.mustVerifyEmail) {
    return <Navigate to="/" replace />;
  }
  if (state.status !== "signedIn") return <p role="status">{t("state.loading")}</p>;

  async function confirmed() {
    setBusy(true);
    await refresh().finally(() => setBusy(false));
  }

  return (
    <AccountCard>
      <h1 className="text-xl font-semibold">{t("account.verify.title")}</h1>
      <p className="rounded-md border bg-surface p-3">{t("errors.auth.emailNotVerified")}</p>
      <p className="text-sm text-text-muted">
        {t("account.verify.sentTo", { email: state.account.email })}
      </p>
      <div className="flex flex-wrap gap-2">
        <Button disabled={busy} onClick={() => void confirmed()}>
          {t("account.verify.confirmed")}
        </Button>
        <Button variant="ghost" onClick={() => void signOutAccount()}>
          {t("account.signOut")}
        </Button>
      </div>
    </AccountCard>
  );
}
