import { Menu } from "lucide-react";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { NavLink, Outlet, useParams } from "react-router";
import { Button } from "@/components/ui/button";
import { useCanChangeContacts, useIsOwner } from "../api/groups";
import { GroupSwitcher } from "../features/groups/GroupSwitcher";
import { Footer } from "./Footer";
import { UserMenu } from "./UserMenu";

/**
 * Responsive base layout (UI-08 … UI-10, UI-22 … UI-25): header with text logo, navigation and
 * user menu; main content at most 1200 px wide with 16 px gutters on mobile and 24 px above.
 * The Group switcher (US-002) follows the logo; the Recycle bin of the Group is for the Owner and
 * Editors (R-012-02), the Group settings for the Owner (BR-04, WF-14); more user menu items come
 * with US-021.
 */
export function AppLayout() {
  const { t } = useTranslation();
  const [menuOpen, setMenuOpen] = useState(false);
  const { groupId = "" } = useParams();
  const canUseRecycleBin = useCanChangeContacts(groupId);
  const isOwner = useIsOwner(groupId);

  return (
    <div className="flex min-h-dvh flex-col">
      <header className="relative border-b bg-surface">
        <div className="mx-auto flex max-w-[1200px] flex-wrap items-center gap-2 px-4 py-2 md:flex-nowrap md:gap-4 md:px-6">
          <span className="rounded-md border px-2 py-1 text-lg font-semibold">
            {t("app.productName")}
          </span>
          <GroupSwitcher />
          <Button
            variant="ghost"
            className="ml-auto md:hidden"
            aria-expanded={menuOpen}
            aria-controls="main-navigation"
            onClick={() => setMenuOpen((open) => !open)}
          >
            <Menu aria-hidden="true" />
            <span className="sr-only">{t("app.openMenu")}</span>
          </Button>
          <nav
            id="main-navigation"
            aria-label={t("app.mainNavigation")}
            className={`${menuOpen ? "flex" : "hidden"} absolute top-full right-0 left-0 z-10 flex-col gap-1 border-b bg-surface p-4 md:static md:ml-4 md:flex md:flex-row md:border-0 md:p-0`}
          >
            <NavLink to="/" className="rounded-md px-3 py-2 text-sm font-semibold">
              {t("nav.contacts")}
            </NavLink>
            {canUseRecycleBin && (
              <NavLink
                to={`/groups/${groupId}/recycle-bin`}
                className="rounded-md px-3 py-2 text-sm font-semibold"
              >
                {t("nav.recycleBin")}
              </NavLink>
            )}
            {isOwner && (
              <NavLink
                to={`/groups/${groupId}/settings`}
                className="rounded-md px-3 py-2 text-sm font-semibold"
              >
                {t("nav.settings")}
              </NavLink>
            )}
          </nav>
          <div className="min-w-0 md:ml-auto">
            <UserMenu />
          </div>
        </div>
      </header>
      <main className="mx-auto w-full max-w-[1200px] flex-1 px-4 py-6 md:px-6">
        <Outlet />
      </main>
      <Footer />
    </div>
  );
}
