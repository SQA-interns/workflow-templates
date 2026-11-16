import { useTranslation } from "react-i18next";
import { Link } from "react-router";

/** Placeholder of the Contact list until US-014; its title is the product's page title (AC-020-01). */
export function HomePage() {
  const { t } = useTranslation();
  return <h1 className="text-xl font-semibold">{t("page.contacts.title")}</h1>;
}

/** Shown for every unknown route and for data the User may not see (BR-02). */
export function NotFoundPage() {
  const { t } = useTranslation();
  return (
    <section className="flex flex-col gap-2">
      <h1 className="text-xl font-semibold">{t("page.notFound.title")}</h1>
      <p className="text-text-muted">{t("page.notFound.body")}</p>
      <Link to="/" className="underline">
        {t("page.notFound.back")}
      </Link>
    </section>
  );
}
