import { ArrowLeft } from "lucide-react";
import type { ReactNode } from "react";
import { useId } from "react";
import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router";
import { Badge } from "@/components/ui/badge";
import { buttonVariants } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { type Contact, useContact } from "../../api/contacts";
import { ApiError } from "../../api/problem";
import { NotFoundPage } from "../../app/pages";
import { formatDateTime } from "../../i18n/format";
import { GroupsPending } from "../groups/GroupsPending";
import { contactName, countryName, FIELD_NAMES } from "./contactFields";

/** WF-07 Contact details (R-014-06): Fields, Attributes, Tags and the way to the History. */
export function ContactDetailsPage() {
  const { t } = useTranslation();
  const { groupId = "", contactId = "" } = useParams();
  const contact = useContact(groupId, contactId);
  const path = `/groups/${groupId}/contacts`;

  if (contact.error instanceof ApiError && contact.error.status === 404) return <NotFoundPage />;
  if (!contact.isSuccess) return <GroupsPending query={contact} />;
  const history = `${path}/${contactId}/history`;
  return (
    <div className="flex flex-col gap-4">
      <Link to={path} className="inline-flex min-h-11 items-center gap-1 self-start underline">
        <ArrowLeft aria-hidden="true" className="size-4" />
        {t("contacts.back")}
      </Link>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h1 className="text-2xl font-semibold break-words">{contactName(contact.data.fields)}</h1>
        <Link to={history} className={buttonVariants({ variant: "secondary" })}>
          {t("contacts.details.history")}
        </Link>
      </div>
      <div className="grid gap-4 md:grid-cols-2">
        <FieldsCard contact={contact.data} />
        <div className="flex flex-col gap-4">
          <Section title={t("contacts.details.attributes")}>
            {contact.data.attributes.length === 0 ? (
              <p className="text-text-muted">{t("contacts.details.noAttributes")}</p>
            ) : (
              <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1">
                {contact.data.attributes.map((attribute) => (
                  <Pair key={attribute.key} term={attribute.key} value={attribute.value} />
                ))}
              </dl>
            )}
          </Section>
          <Section title={t("contacts.details.tags")}>
            {contact.data.tags.length === 0 ? (
              <p className="text-text-muted">{t("contacts.details.noTags")}</p>
            ) : (
              <p className="flex flex-wrap gap-1">
                {contact.data.tags.map((tag) => (
                  <Badge key={tag.id}>{tag.name}</Badge>
                ))}
              </p>
            )}
          </Section>
          <Card className="flex flex-wrap items-center gap-x-2">
            <Link to={history} className="inline-flex min-h-11 items-center underline">
              {t("contacts.details.history")}
            </Link>
            <span>
              {t("contacts.details.version", {
                number: contact.data.versionNumber,
                at: formatDateTime(contact.data.updatedAt),
              })}
            </span>
          </Card>
        </div>
      </div>
    </div>
  );
}

/** The Fields in form order; an empty one shows a dash, the country its name (UI-18). */
function FieldsCard({ contact }: { contact: Contact }) {
  const { t, i18n } = useTranslation();
  const values = new Map(Object.entries(contact.fields));
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

function Section({ title, children }: { title: string; children: ReactNode }) {
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
