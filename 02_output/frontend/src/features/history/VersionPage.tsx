import { ArrowLeft } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router";
import { useVersion } from "../../api/history";
import { ApiError } from "../../api/problem";
import { NotFoundPage } from "../../app/pages";
import { formatDateTime } from "../../i18n/format";
import { AttributesSection, FieldsSection, TagsSection } from "../contacts/ContactContent";
import { contactName } from "../contacts/contactFields";
import { GroupsPending } from "../groups/GroupsPending";

/** WF-10 Version (R-010-02): the full content of the Contact after one Version, read-only. */
export function VersionPage() {
  const { t } = useTranslation();
  const { groupId = "", contactId = "", number = "" } = useParams();
  const version = useVersion(groupId, contactId, number);

  if (version.error instanceof ApiError && version.error.status === 404) return <NotFoundPage />;
  if (!version.isSuccess) return <GroupsPending query={version} />;
  const { kind, changedBy, changedAt, snapshot } = version.data;
  return (
    <div className="flex flex-col gap-4">
      <Link
        to={`/groups/${groupId}/contacts/${contactId}/history`}
        className="inline-flex min-h-11 items-center gap-1 self-start underline"
      >
        <ArrowLeft aria-hidden="true" className="size-4" />
        {t("contacts.details.history")}
      </Link>
      <h1 className="text-2xl font-semibold break-words">
        {t("history.versionOf", {
          number: version.data.number,
          name: contactName(snapshot.fields),
        })}
      </h1>
      <p className="text-text-muted">
        {[
          t(`history.kind.${kind}`),
          changedBy,
          formatDateTime(changedAt),
          t("history.readOnly"),
        ].join(" · ")}
      </p>
      <div className="grid gap-4 md:grid-cols-2">
        <FieldsSection fields={snapshot.fields} />
        <div className="flex flex-col gap-4">
          <AttributesSection attributes={snapshot.attributes} />
          <TagsSection names={snapshot.tags} />
        </div>
      </div>
    </div>
  );
}
