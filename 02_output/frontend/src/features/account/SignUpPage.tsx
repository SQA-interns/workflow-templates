import { useTranslation } from "react-i18next";
import { Link } from "react-router";
import { signUpWithEmail } from "../../auth/firebase";
import { AccountCard } from "./AccountCard";
import { CredentialsForm } from "./CredentialsForm";

/** WF-02 Create an account; Firebase then sends the confirmation link (R-001-01, EX-001-05). */
export function SignUpPage() {
  const { t } = useTranslation();
  return (
    <AccountCard>
      <h1 className="text-xl font-semibold">{t("account.signUp.title")}</h1>
      <CredentialsForm
        passwordAutoComplete="new-password"
        submitLabel={t("account.signUp.title")}
        onSubmit={signUpWithEmail}
      />
      <p className="text-sm text-text-muted">
        {t("account.signUp.haveAccount")}{" "}
        <Link to="/sign-in" className="underline">
          {t("account.signIn.submit")}
        </Link>
      </p>
    </AccountCard>
  );
}
