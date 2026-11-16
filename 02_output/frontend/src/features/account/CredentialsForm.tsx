import { type FormEvent, useId, useState } from "react";
import { useTranslation } from "react-i18next";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { authErrorKey, signInWithGoogle } from "../../auth/firebase";

/** Runs a Firebase call; returns the catalog key of its error, null on success or no message. */
async function attempt(action: () => Promise<void>): Promise<string | null> {
  try {
    await action();
    return null;
  } catch (error) {
    return authErrorKey(error);
  }
}

interface CredentialsFormProps {
  submitLabel: string;
  /** `current-password` to sign in, `new-password` to create an account. */
  passwordAutoComplete: "current-password" | "new-password";
  onSubmit: (email: string, password: string) => Promise<void>;
}

/**
 * E-mail and password, the error of the last attempt, the submit button, then "Sign in with
 * Google" (WF-01, WF-02, R-001-01). Success is followed by the route guard, not by this form.
 */
export function CredentialsForm({
  submitLabel,
  passwordAutoComplete,
  onSubmit,
}: CredentialsFormProps) {
  const { t } = useTranslation();
  const id = useId();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [errorKey, setErrorKey] = useState<string | null>(null);

  async function run(action: () => Promise<void>) {
    setBusy(true);
    setErrorKey(null);
    setErrorKey(await attempt(action));
    setBusy(false);
  }

  function submit(event: FormEvent) {
    event.preventDefault();
    void run(() => onSubmit(email.trim(), password));
  }

  return (
    <>
      <form className="flex flex-col gap-4" onSubmit={submit}>
        <div className="flex flex-col gap-1">
          <Label htmlFor={`${id}-email`}>{t("account.field.email")}</Label>
          <Input
            id={`${id}-email`}
            type="email"
            autoComplete="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
        </div>
        <div className="flex flex-col gap-1">
          <Label htmlFor={`${id}-password`}>{t("account.field.password")}</Label>
          <Input
            id={`${id}-password`}
            type="password"
            autoComplete={passwordAutoComplete}
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </div>
        {errorKey && (
          <p role="alert" className="text-sm font-semibold text-danger">
            {t(errorKey)}
          </p>
        )}
        <Button type="submit" disabled={busy}>
          {submitLabel}
        </Button>
      </form>
      <hr className="border-border" />
      <Button variant="secondary" disabled={busy} onClick={() => void run(signInWithGoogle)}>
        {t("account.google")}
      </Button>
    </>
  );
}
