import { useQueryClient } from "@tanstack/react-query";
import { Check, ChevronDown, LogOut, UserRound } from "lucide-react";
import { type KeyboardEvent, useEffect, useId, useState } from "react";
import { useTranslation } from "react-i18next";
import { Button } from "@/components/ui/button";
import { useChangeLanguage, useMe } from "../api/me";
import { signOutAccount } from "../auth/firebase";
import { type Language } from "../i18n";

/** Each language is named in itself, so a User finds it in any language (R-020-02). */
const languageNames: [Language, string][] = [
  ["sl", "Slovenščina"],
  ["en", "English"],
];

/** The user menu of the header, named by the display name (UI-10): language, Sign out. */
export function UserMenu() {
  const { t, i18n } = useTranslation();
  const queryClient = useQueryClient();
  const me = useMe();
  const changeLanguage = useChangeLanguage();
  const language = me.data?.language;

  /** Every text follows the User's language: Slovenian for a new User (R-020-01, R-020-02). */
  useEffect(() => {
    if (language) void i18n.changeLanguage(language);
  }, [i18n, language]);
  const [open, setOpen] = useState(false);
  const menuId = useId();

  /** Signs out and forgets every answer of the API kept in the browser (R-001-06). */
  async function signOut() {
    setOpen(false);
    await signOutAccount();
    queryClient.clear();
  }

  function closeOnEscape(event: KeyboardEvent) {
    if (event.key === "Escape") setOpen(false);
  }

  return (
    <div className="relative min-w-0">
      <Button
        variant="ghost"
        className="max-w-full px-2 md:px-4"
        aria-expanded={open}
        aria-controls={menuId}
        onKeyDown={closeOnEscape}
        onClick={() => setOpen((value) => !value)}
      >
        <UserRound aria-hidden="true" />
        <span className="truncate">{me.data?.displayName ?? t("app.userMenu")}</span>
        <ChevronDown aria-hidden="true" />
      </Button>
      {open && (
        <ul
          id={menuId}
          className="absolute right-0 z-10 mt-1 min-w-48 rounded-md border bg-surface-raised p-1"
        >
          <li className="border-b pb-1">
            <div role="group" aria-label={t("app.language")}>
              {languageNames.map(([code, name]) => (
                <Button
                  key={code}
                  variant="ghost"
                  lang={code}
                  className="w-full justify-start"
                  aria-pressed={language === code}
                  onKeyDown={closeOnEscape}
                  onClick={() => changeLanguage.mutate(code, { onSuccess: () => setOpen(false) })}
                >
                  <Check aria-hidden="true" className={language === code ? "" : "invisible"} />
                  {name}
                </Button>
              ))}
            </div>
            {changeLanguage.isError && (
              <p role="alert" className="px-3 py-1 text-sm text-danger">
                {t("state.error")}
              </p>
            )}
          </li>
          <li className="pt-1">
            <Button
              variant="ghost"
              className="w-full justify-start"
              onKeyDown={closeOnEscape}
              onClick={() => void signOut()}
            >
              <LogOut aria-hidden="true" />
              {t("account.signOut")}
            </Button>
          </li>
        </ul>
      )}
    </div>
  );
}
