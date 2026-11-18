import { useQueryClient } from "@tanstack/react-query";
import { type FormEvent, useId, useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { NativeSelect } from "@/components/ui/native-select";
import { type Me, useChangeLanguage, useDeleteAccount, useMe } from "../../api/me";
import { ApiError, messageKey } from "../../api/problem";
import { signOutAccount } from "../../auth/firebase";
import { type Language, languageNames } from "../../i18n";
import { GroupsPending } from "../groups/GroupsPending";

/** WF-19 Account: the language (R-020-02) and "Delete my account" (US-021). */
export function AccountPage() {
  const { t } = useTranslation();
  const me = useMe();

  if (!me.isSuccess) return <GroupsPending query={me} />;
  return (
    <div className="mx-auto flex w-full max-w-2xl flex-col gap-4">
      <h1 className="text-2xl font-semibold">{t("account.settings.title")}</h1>
      <LanguageCard me={me.data} />
      <DeleteAccountCard me={me.data} />
    </div>
  );
}

/** The language of the product, kept for later sign-ins (R-020-02). */
function LanguageCard({ me }: { me: Me }) {
  const { t } = useTranslation();
  const id = useId();
  const changeLanguage = useChangeLanguage();

  return (
    <Card className="flex flex-col gap-3">
      <h2 className="text-lg font-semibold">{t("app.language")}</h2>
      <div className="flex flex-col gap-1">
        <Label htmlFor={`${id}-language`}>{t("app.language")}</Label>
        <NativeSelect
          id={`${id}-language`}
          aria-describedby={`${id}-hint`}
          value={me.language}
          disabled={changeLanguage.isPending}
          onChange={(event) => changeLanguage.mutate(event.target.value as Language)}
        >
          {languageNames.map(([code, name]) => (
            <option key={code} value={code} lang={code}>
              {name}
            </option>
          ))}
        </NativeSelect>
        <p id={`${id}-hint`} className="text-sm text-text-muted">
          {t("account.settings.languageHint")}
        </p>
      </div>
      {changeLanguage.isError && (
        <p role="alert" className="text-sm font-semibold text-danger">
          {t("state.error")}
        </p>
      )}
    </Card>
  );
}

/** E-mails are compared ignoring case (BR-11); outer spaces of the typed one are ignored. */
function sameEmail(typed: string, email: string): boolean {
  return typed.trim().toLowerCase() === email.toLowerCase();
}

/**
 * "Delete my account": enabled once the typed e-mail matches (EX-021-01); a deletion signs out
 * and opens the sign-in screen (AC-021-01); a refusal is shown below the button (AC-021-02).
 */
function DeleteAccountCard({ me }: { me: Me }) {
  const { t } = useTranslation();
  const id = useId();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const deletion = useDeleteAccount();
  const [typed, setTyped] = useState("");
  const matches = sameEmail(typed, me.email);
  const error = deletion.error;

  function submit(event: FormEvent) {
    event.preventDefault();
    if (!matches) return;
    deletion.mutate(typed.trim(), {
      onSuccess: async () => {
        await signOutAccount();
        queryClient.clear();
        await navigate("/sign-in", { replace: true });
      },
    });
  }

  return (
    <Card>
      <form className="flex flex-col items-start gap-3" onSubmit={submit} noValidate>
        <h2 className="text-lg font-semibold">{t("account.settings.delete.title")}</h2>
        <p className="text-text-muted">{t("account.settings.delete.description")}</p>
        <div className="flex w-full flex-col gap-1">
          <Label htmlFor={`${id}-email`}>
            {t("account.settings.delete.confirm")}
            <span aria-hidden="true"> *</span>
          </Label>
          <Input
            id={`${id}-email`}
            type="email"
            autoComplete="off"
            aria-required="true"
            value={typed}
            onChange={(event) => setTyped(event.target.value)}
          />
        </div>
        <Button
          variant="danger"
          type="submit"
          disabled={!matches || deletion.isPending}
          aria-busy={deletion.isPending}
        >
          {t("account.settings.delete.action")}
        </Button>
        {error && (
          <p
            role="alert"
            className="border-l-4 border-danger pl-3 text-sm font-semibold text-danger"
          >
            {error instanceof ApiError ? t(messageKey(error.code), error.params) : t("state.error")}
          </p>
        )}
      </form>
    </Card>
  );
}
