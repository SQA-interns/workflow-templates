import type { ReactNode } from "react";
import { useId } from "react";
import { useTranslation } from "react-i18next";
import { Badge } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";
import type { AttributeRow } from "./attributes";
import { type ContactFields, countryName, FIELD_NAMES } from "./contactFields";

/** The Fields in form order; an empty one shows a dash, the country its name (UI-18). */
export function FieldsSection({ fields }: { fields: ContactFields }) {
  const { t, i18n } = useTranslation();
  const values = new Map(Object.entries(fields));
  return (
    <Section title={t("contacts.fields")}>
      <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1">
        {FIELD_NAMES.map((name) => {
          const value = values.get(name);
          const shown = name === "country" && value ? countryName(value, i18n.language) : value;
          return <Pair key={name} term={t(`contacts.field.${name}`)} value={shown || "—"} />;
        })}
      </dl>
    </Section>
  );
}

/** The Attributes as key and value (R-008-06). */
export function AttributesSection({ attributes }: { attributes: AttributeRow[] }) {
  const { t } = useTranslation();
  return (
    <Section title={t("contacts.details.attributes")}>
      {attributes.length === 0 ? (
        <p className="text-text-muted">{t("contacts.details.noAttributes")}</p>
      ) : (
        <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1">
          {attributes.map((attribute) => (
            <Pair key={attribute.key} term={attribute.key} value={attribute.value} />
          ))}
        </dl>
      )}
    </Section>
  );
}

/** The Tags as chips. */
export function TagsSection({ names }: { names: string[] }) {
  const { t } = useTranslation();
  return (
    <Section title={t("contacts.details.tags")}>
      {names.length === 0 ? (
        <p className="text-text-muted">{t("contacts.details.noTags")}</p>
      ) : (
        <p className="flex flex-wrap gap-1">
          {names.map((name) => (
            <Badge key={name}>{name}</Badge>
          ))}
        </p>
      )}
    </Section>
  );
}

/** A titled card of the Contact content, named by its heading. */
export function Section({ title, children }: { title: string; children: ReactNode }) {
  const id = useId();
  return (
    <Card aria-labelledby={id} className="flex flex-col gap-3">
      <h2 id={id} className="text-xl font-semibold">
        {title}
      </h2>
      {children}
    </Card>
  );
}

function Pair({ term, value }: { term: string; value: string }) {
  return (
    <>
      <dt className="text-text-muted">{term}</dt>
      <dd className="break-words whitespace-pre-line">{value}</dd>
    </>
  );
}
