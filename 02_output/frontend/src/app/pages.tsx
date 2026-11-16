import { useTranslation } from "react-i18next";
import { Link } from "react-router";

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

/**
 * The Contact list of the current Group (R-002-04); only its heading until US-014 builds WF-06
 * (task T-12) in its place.
 */
export function ContactListPlaceholder() {
  const { t } = useTranslation();
  return <h1 className="text-xl font-semibold">{t("page.contacts.title")}</h1>;
}
