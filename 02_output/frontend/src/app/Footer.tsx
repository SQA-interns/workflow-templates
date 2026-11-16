import { useTranslation } from "react-i18next";

/** Page footer with the product name, as on every wireframe (UI-10). */
export function Footer() {
  const { t } = useTranslation();
  return (
    <footer className="border-t">
      <div className="mx-auto max-w-[1200px] px-4 py-4 text-sm text-text-muted md:px-6">
        {t("app.productName")}
      </div>
    </footer>
  );
}
