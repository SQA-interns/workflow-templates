import { useTranslation } from "react-i18next";
import { Link, useNavigate, useParams } from "react-router";
import { Button } from "@/components/ui/button";
import { type DuplicateCandidate, useDuplicates } from "../../api/duplicates";
import { useCanChangeContacts } from "../../api/groups";
import { GroupsPending } from "../groups/GroupsPending";

/**
 * WF-17 Possible duplicates (R-017-01): every pair with links to both Contacts and every reason;
 * it only reads (R-017-05). The Owner and Editors get Merge for each pair (R-018-01, BR-03).
 */
export function DuplicatesPage() {
  const { t } = useTranslation();
  const { groupId = "" } = useParams();
  const duplicates = useDuplicates(groupId);
  const canMerge = useCanChangeContacts(groupId);

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
            <PairCard
              key={`${pair.first.id}-${pair.second.id}`}
              groupId={groupId}
              pair={pair}
              canMerge={canMerge}
            />
          ))}
        </ul>
      )}
    </div>
  );
}

interface PairCardProps {
  groupId: string;
  pair: DuplicateCandidate;
  canMerge: boolean;
}

/** One pair as a card: both Contacts as links, a chip per reason (UI-16), then Merge. */
function PairCard({ groupId, pair, canMerge }: PairCardProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();
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
      {canMerge && (
        <Button
          variant="secondary"
          className="ml-auto"
          onClick={() =>
            void navigate(
              `/groups/${groupId}/merge?first=${pair.first.id}&second=${pair.second.id}`,
            )
          }
        >
          {t("merge.title")}
        </Button>
      )}
    </li>
  );
}
