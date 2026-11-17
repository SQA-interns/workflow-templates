import { useTranslation } from "react-i18next";
import { useParams } from "react-router";
import { Button } from "@/components/ui/button";
import { ApiError, messageKey } from "../../api/problem";
import {
  type RecycleBinEntry,
  useRecycleBin,
  useRestoreFromRecycleBin,
} from "../../api/recycleBin";
import { formatDateTime } from "../../i18n/format";
import { GroupsPending } from "../groups/GroupsPending";

const COLUMNS = ["name", "deletedBy", "deletedAt", "removalAt"] as const;

/**
 * WF-11 Recycle bin (R-012-02): the Group's Contacts in the recycle bin, newest first, each with
 * Restore (R-012-03); a refused restore shows its message below the list (R-012-04).
 */
export function RecycleBinPage() {
  const { t } = useTranslation();
  const { groupId = "" } = useParams();
  const bin = useRecycleBin(groupId);
  const restore = useRestoreFromRecycleBin(groupId);
  const error = restore.error;

  if (bin.error instanceof ApiError && bin.error.status === 403) {
    return <p role="alert">{t(messageKey(bin.error.code))}</p>;
  }
  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-2xl font-semibold">{t("recycleBin.title")}</h1>
      <p className="text-text-muted">{t("recycleBin.intro")}</p>
      {!bin.isSuccess ? (
        <GroupsPending query={bin} />
      ) : bin.data.length === 0 ? (
        <p>{t("recycleBin.empty")}</p>
      ) : (
        <EntryTable
          entries={bin.data}
          busyId={restore.isPending ? restore.variables.contact.id : null}
          onRestore={(entry) => restore.mutate(entry)}
        />
      )}
      {error && (
        <p role="alert" className="border-l-4 border-danger pl-3 text-sm font-semibold text-danger">
          {error instanceof ApiError ? t(messageKey(error.code), error.params) : t("state.error")}
        </p>
      )}
    </div>
  );
}

/** The entries: a table from 768 px, stacked cards below it (UI-16), one set of elements. */
function EntryTable({
  entries,
  busyId,
  onRestore,
}: {
  entries: RecycleBinEntry[];
  busyId: string | null;
  onRestore: (entry: RecycleBinEntry) => void;
}) {
  const { t } = useTranslation();
  const cell = "px-3 py-2 max-md:block max-md:p-0";
  return (
    <table className="w-full border-collapse text-left max-md:block">
      <thead className="max-md:sr-only">
        <tr className="border-b border-border">
          {COLUMNS.map((column) => (
            <th key={column} scope="col" className="px-3 py-2 text-sm font-semibold">
              {t(`recycleBin.column.${column}`)}
            </th>
          ))}
          <th scope="col" className="px-3 py-2">
            <span className="sr-only">{t("recycleBin.column.actions")}</span>
          </th>
        </tr>
      </thead>
      <tbody className="max-md:flex max-md:flex-col max-md:gap-3">
        {entries.map((entry) => (
          <tr
            key={entry.contact.id}
            className="border-b border-border max-md:block max-md:rounded-lg max-md:border max-md:p-3"
          >
            <td className={`${cell} font-semibold`}>{entry.contact.name}</td>
            <td className={`${cell} max-md:text-text-muted`}>
              <span className="md:hidden">{t("recycleBin.card.deletedBy")} </span>
              {entry.deletedBy}
            </td>
            <td className={`${cell} max-md:text-text-muted`}>
              <span className="md:hidden">{t("recycleBin.card.deletedAt")} </span>
              {formatDateTime(entry.deletedAt)}
            </td>
            <td className={`${cell} max-md:text-text-muted`}>
              <span className="md:hidden">{t("recycleBin.card.removalAt")} </span>
              {formatDateTime(entry.removalAt)}
            </td>
            <td className={`${cell} max-md:pt-2 md:text-right`}>
              <Button
                variant="secondary"
                disabled={busyId !== null}
                aria-busy={busyId === entry.contact.id}
                onClick={() => onRestore(entry)}
              >
                {t("recycleBin.restore")}
              </Button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
