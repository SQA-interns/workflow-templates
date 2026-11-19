import { Menu } from "lucide-react";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { NavLink, Outlet, useParams } from "react-router";
import { Button } from "@/components/ui/button";
import { useCanChangeContacts, useIsMember, useIsOwner } from "../api/groups";
import { GroupSwitcher } from "../features/groups/GroupSwitcher";
import { Footer } from "./Footer";
import { UserMenu } from "./UserMenu";

/**
 * Responsive base layout (UI-08 … UI-10, UI-22 … UI-25): header with text logo, navigation and
 * user menu; main content at most 1200 px wide with 16 px gutters on mobile and 24 px above.
 * From 768 to 1279 px the navigation and the user menu form a second header row (WF-06 tablet);
 * from 1280 px the header is one row (WF-06 desktop).
 * The Group switcher (US-002) follows the logo; the Recycle bin of the Group is for the Owner and
 * Editors (R-012-02), Possible duplicates for every Member (R-017-01), Members and the Group settings for the Owner (BR-04, WF-13, WF-14); more user menu items come
 * with US-021.
 */
/** A navigation item never wraps (UI-24). */
const navItem =
  "rounded-md px-3 py-2 text-sm font-semibold whitespace-nowrap hover:bg-surface aria-[current=page]:bg-primary-subtle aria-[current=page]:text-primary-subtle-foreground";

export function AppLayout() {
  const { t } = useTranslation();
  const [menuOpen, setMenuOpen] = useState(false);
  const { groupId = "" } = useParams();
  const canUseRecycleBin = useCanChangeContacts(groupId);
  const isOwner = useIsOwner(groupId);
  const isMember = useIsMember(groupId);

  return (
    <div className="flex min-h-dvh flex-col">
      <header className="relative border-b bg-surface-raised shadow-sm">
        <div className="mx-auto flex max-w-[1200px] flex-wrap items-center gap-2 px-4 py-2 md:gap-x-4 md:px-6 xl:flex-nowrap">
          <span className="rounded-md px-2 py-1 text-lg font-semibold text-primary">
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
          <div className="contents md:flex md:basis-full md:items-center xl:min-w-0 xl:flex-1 xl:basis-auto">
            <nav
              id="main-navigation"
              aria-label={t("app.mainNavigation")}
              className={`${menuOpen ? "flex" : "hidden"} absolute top-full right-0 left-0 z-10 flex-col gap-1 border-b bg-surface-raised p-4 md:static md:flex md:flex-row md:border-0 md:p-0 xl:ml-4`}
            >
              {/* In a Group the item is the Group's Contact list, so it is marked as the
                  current page there and on the Contact pages below it (UI-10, F-44). */}
              <NavLink to={groupId ? `/groups/${groupId}/contacts` : "/"} className={navItem}>
                {t("nav.contacts")}
              </NavLink>
              {canUseRecycleBin && (
                <NavLink to={`/groups/${groupId}/recycle-bin`} className={navItem}>
                  {t("nav.recycleBin")}
                </NavLink>
              )}
              {isMember && (
                <NavLink to={`/groups/${groupId}/duplicates`} className={navItem}>
                  {t("nav.duplicates")}
                </NavLink>
              )}
              {isOwner && (
                <>
                  <NavLink to={`/groups/${groupId}/members`} className={navItem}>
                    {t("nav.members")}
                  </NavLink>
                  <NavLink to={`/groups/${groupId}/settings`} className={navItem}>
                    {t("nav.settings")}
                  </NavLink>
                </>
              )}
            </nav>
            <div className="min-w-0 md:ml-auto md:shrink-0">
              <UserMenu />
            </div>
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
