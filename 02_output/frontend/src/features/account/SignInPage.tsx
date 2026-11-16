import { useTranslation } from "react-i18next";
import { Link } from "react-router";
import { signInWithEmail } from "../../auth/firebase";
import { AccountCard } from "./AccountCard";
import { CredentialsForm } from "./CredentialsForm";

/** WF-01 Sign in: e-mail and password, or Google (R-001-01, EX-001-01). */
export function SignInPage() {
  const { t } = useTranslation();
  return (
    <AccountCard>
      <h1 className="text-xl font-semibold">{t("account.signIn.title")}</h1>
      <h2 className="text-base font-semibold">{t("account.signIn.withEmail")}</h2>
      <CredentialsForm
        passwordAutoComplete="current-password"
        submitLabel={t("account.signIn.submit")}
        onSubmit={signInWithEmail}
      />
      <p className="text-sm text-text-muted">
        {t("account.signIn.noAccount")}{" "}
        <Link to="/sign-up" className="underline">
          {t("account.signUp.title")}
        </Link>
      </p>
    </AccountCard>
  );
}
