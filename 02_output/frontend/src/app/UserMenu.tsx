import { useQueryClient } from "@tanstack/react-query";
import { ChevronDown, LogOut, UserRound } from "lucide-react";
import { type KeyboardEvent, useId, useState } from "react";
import { useTranslation } from "react-i18next";
import { Button } from "@/components/ui/button";
import { useMe } from "../api/me";
import { signOutAccount } from "../auth/firebase";

/** The user menu of the header, named by the display name (UI-10); items per story. */
export function UserMenu() {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const me = useMe();
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
    <div className="relative">
      <Button
        variant="ghost"
        aria-expanded={open}
        aria-controls={menuId}
        onKeyDown={closeOnEscape}
        onClick={() => setOpen((value) => !value)}
      >
        <UserRound aria-hidden="true" />
        <span>{me.data?.displayName ?? t("app.userMenu")}</span>
        <ChevronDown aria-hidden="true" />
      </Button>
      {open && (
        <ul
          id={menuId}
          className="absolute right-0 z-10 mt-1 min-w-48 rounded-md border bg-surface-raised p-1"
        >
          <li>
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
