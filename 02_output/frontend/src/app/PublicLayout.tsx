import { useTranslation } from "react-i18next";
import { Outlet } from "react-router";
import { Footer } from "./Footer";

/** Layout of the screens before sign-in: header with the text logo only (WF-01, WF-02, WF-03). */
export function PublicLayout() {
  const { t } = useTranslation();
  return (
    <div className="flex min-h-dvh flex-col">
      <header className="border-b bg-surface">
        <div className="mx-auto flex max-w-[1200px] items-center px-4 py-2 md:px-6">
          <span className="rounded-md border px-2 py-1 text-lg font-semibold">
            {t("app.productName")}
          </span>
        </div>
      </header>
      <main className="mx-auto w-full max-w-[1200px] flex-1 px-4 py-6 md:px-6">
        <Outlet />
      </main>
      <Footer />
    </div>
  );
}
