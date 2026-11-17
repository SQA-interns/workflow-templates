import { ArrowLeft } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router";
import { Badge } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";
import { useContact } from "../../api/contacts";
import { useHistory, type VersionSummary } from "../../api/history";
import { ApiError } from "../../api/problem";
import { NotFoundPage } from "../../app/pages";
import { formatDateTime } from "../../i18n/format";
import { contactName } from "../contacts/contactFields";
import { GroupsPending } from "../groups/GroupsPending";
import { changeText } from "./changes";

/** WF-09 History (R-010-01): every Version newest first with kind, who, when and what changed. */
export function HistoryPage() {
  const { t } = useTranslation();
  const { groupId = "", contactId = "" } = useParams();
  const contact = useContact(groupId, contactId);
  const history = useHistory(groupId, contactId);
  const details = `/groups/${groupId}/contacts/${contactId}`;

  const notFound = [contact.error, history.error].some(
    (error) => error instanceof ApiError && error.status === 404,
  );
  if (notFound) return <NotFoundPage />;
  if (!contact.isSuccess) return <GroupsPending query={contact} />;
  if (!history.isSuccess) return <GroupsPending query={history} />;
  const name = contactName(contact.data.fields);
  return (
    <div className="flex flex-col gap-4">
      <Link to={details} className="inline-flex min-h-11 items-center gap-1 self-start underline">
        <ArrowLeft aria-hidden="true" className="size-4" />
        {name}
      </Link>
      <h1 className="text-2xl font-semibold break-words">{t("history.title", { name })}</h1>
      <p className="text-text-muted">{t("history.order")}</p>
      {history.data.map((version) => (
        <VersionCard key={version.number} version={version} path={`${details}/history`} />
      ))}
    </div>
  );
}

function VersionCard({ version, path }: { version: VersionSummary; path: string }) {
  const { t, i18n } = useTranslation();
  const title = t("history.version", { number: version.number });
  return (
    <Card aria-label={title} className="flex flex-col gap-2">
      <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
        <strong>{title}</strong>
        <Badge>
          {[
            t(`history.kind.${version.kind}`),
            ...(version.restoredFrom ? [t("history.from", { number: version.restoredFrom })] : []),
          ].join(" · ")}
        </Badge>
        <span>{version.changedBy}</span>
        <span className="text-text-muted">{formatDateTime(version.changedAt)}</span>
        <Link
          to={`${path}/${version.number}`}
          className="ml-auto inline-flex min-h-11 items-center underline"
        >
          {t("history.view")}
        </Link>
      </div>
      <ul className="break-words text-text-muted">
        {version.changes.map((change) => (
          <li key={change.item}>{changeText(change, t, i18n.language)}</li>
        ))}
      </ul>
    </Card>
  );
}
