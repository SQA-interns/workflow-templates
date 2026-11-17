import { ArrowLeft } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router";
import { buttonVariants } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { useContact } from "../../api/contacts";
import { useCanChangeContacts } from "../../api/groups";
import { ApiError } from "../../api/problem";
import { NotFoundPage } from "../../app/pages";
import { formatDateTime } from "../../i18n/format";
import { GroupsPending } from "../groups/GroupsPending";
import { AttributesSection, FieldsSection, TagsSection } from "./ContactContent";
import { contactName } from "./contactFields";
import { MoveToRecycleBin } from "./MoveToRecycleBin";

/**
 * WF-07 Contact details (R-014-06): Fields, Attributes, Tags, Edit, the way to the History,
 * Move to recycle bin (US-012) and "Merged from" each Contact merged into it (R-018-05).
 */
export function ContactDetailsPage() {
  const { t } = useTranslation();
  const { groupId = "", contactId = "" } = useParams();
  const contact = useContact(groupId, contactId);
  const canEdit = useCanChangeContacts(groupId);
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
        <div className="flex flex-wrap gap-2">
          {canEdit && (
            <Link to={`${path}/${contactId}/edit`} className={buttonVariants()}>
              {t("contacts.details.edit")}
            </Link>
          )}
          <Link to={history} className={buttonVariants({ variant: "secondary" })}>
            {t("contacts.details.history")}
          </Link>
          {canEdit && contact.data.state === "active" && (
            <MoveToRecycleBin groupId={groupId} contact={contact.data} />
          )}
        </div>
      </div>
      {contact.data.mergedFrom
        .filter((merge) => !merge.undone)
        .map((merge) => (
          <p key={merge.mergeId} className="border-l-4 border-text pl-2">
            {t("merge.mergedFrom")}{" "}
            <Link
              to={`${path}/${merge.contact.id}/history`}
              className="inline-flex min-h-11 items-center underline"
            >
              {merge.contact.name}
            </Link>
          </p>
        ))}
      <div className="grid gap-4 md:grid-cols-2">
        <FieldsSection fields={contact.data.fields} />
        <div className="flex flex-col gap-4">
          <AttributesSection attributes={contact.data.attributes} />
          <TagsSection names={contact.data.tags.map((tag) => tag.name)} />
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
