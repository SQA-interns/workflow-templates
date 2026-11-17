import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router";
import { type DuplicateCandidate, useDuplicates } from "../../api/duplicates";
import { GroupsPending } from "../groups/GroupsPending";

/**
 * WF-17 Possible duplicates (R-017-01): every pair with links to both Contacts and every reason;
 * it only reads (R-017-05). Merge comes with US-018.
 */
export function DuplicatesPage() {
  const { t } = useTranslation();
  const { groupId = "" } = useParams();
  const duplicates = useDuplicates(groupId);

  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-2xl font-semibold">{t("duplicates.title")}</h1>
      <p className="text-text-muted">{t("duplicates.intro")}</p>
      {!duplicates.isSuccess ? (
        <GroupsPending query={duplicates} />
      ) : duplicates.data.length === 0 ? (
        <p>{t("duplicates.empty")}</p>
      ) : (
        <ul className="flex flex-col gap-3">
          {duplicates.data.map((pair) => (
            <PairCard key={`${pair.first.id}-${pair.second.id}`} groupId={groupId} pair={pair} />
          ))}
        </ul>
      )}
    </div>
  );
}

/** One pair as a card: both Contacts as links, then a chip per reason (UI-16). */
function PairCard({ groupId, pair }: { groupId: string; pair: DuplicateCandidate }) {
  const { t } = useTranslation();
  const link = "inline-flex min-h-11 items-center font-semibold underline";
  return (
    <li className="flex flex-wrap items-center gap-2 rounded-lg border border-border bg-surface p-3">
      <Link className={link} to={`/groups/${groupId}/contacts/${pair.first.id}`}>
        {pair.first.name}
      </Link>
      <span aria-hidden="true">↔</span>
      <Link className={link} to={`/groups/${groupId}/contacts/${pair.second.id}`}>
        {pair.second.name}
      </Link>
      {pair.reasons.map((reason) => (
        <span key={reason} className="rounded-full border border-border px-2 py-0.5 text-sm">
          {t(`duplicates.reason.${reason}`)}
        </span>
      ))}
    </li>
  );
}
