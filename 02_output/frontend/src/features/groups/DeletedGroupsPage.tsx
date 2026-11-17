import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router";
import { Button } from "@/components/ui/button";
import { type DeletedGroup, useDeletedGroups, useRestoreGroup } from "../../api/groups";
import { ApiError, messageKey } from "../../api/problem";
import { formatDateTime } from "../../i18n/format";
import { GroupsPending } from "./GroupsPending";

/**
 * WF-15 Deleted Groups: the User's own Groups in the recycle bin with their removal date in
 * Europe/Ljubljana (R-003-04); Restore opens the Group again (R-003-05); a refused restore shows its
 * message below the list (R-003-06).
 */
export function DeletedGroupsPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const deleted = useDeletedGroups();
  const restore = useRestoreGroup();
  const error = restore.error;

  function restoreGroup(group: DeletedGroup) {
    restore.mutate(group.id, {
      onSuccess: () => void navigate(`/groups/${group.id}/contacts`),
    });
  }

  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-2xl font-semibold">{t("deletedGroups.title")}</h1>
      <p className="text-text-muted">{t("deletedGroups.intro")}</p>
      {!deleted.isSuccess ? (
        <GroupsPending query={deleted} />
      ) : deleted.data.length === 0 ? (
        <p>{t("deletedGroups.empty")}</p>
      ) : (
        <DeletedGroupTable
          groups={deleted.data}
          busyId={restore.isPending ? restore.variables : null}
          onRestore={restoreGroup}
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

/** The Groups: a table from 768 px, stacked cards below it (UI-16), one set of elements. */
function DeletedGroupTable({
  groups,
  busyId,
  onRestore,
}: {
  groups: DeletedGroup[];
  busyId: string | null;
  onRestore: (group: DeletedGroup) => void;
}) {
  const { t } = useTranslation();
  const cell = "px-3 py-2 max-md:block max-md:p-0";
  return (
    <table className="w-full border-collapse text-left max-md:block">
      <thead className="max-md:sr-only">
        <tr className="border-b border-border">
          <th scope="col" className="px-3 py-2 text-sm font-semibold">
            {t("deletedGroups.column.name")}
          </th>
          <th scope="col" className="px-3 py-2 text-sm font-semibold">
            {t("deletedGroups.column.removalAt")}
          </th>
          <th scope="col" className="px-3 py-2">
            <span className="sr-only">{t("deletedGroups.column.actions")}</span>
          </th>
        </tr>
      </thead>
      <tbody className="max-md:flex max-md:flex-col max-md:gap-3">
        {groups.map((group) => (
          <tr
            key={group.id}
            className="border-b border-border max-md:block max-md:rounded-lg max-md:border max-md:p-3"
          >
            <td className={`${cell} font-semibold`}>{group.name}</td>
            <td className={`${cell} max-md:text-text-muted`}>
              <span className="md:hidden">{t("deletedGroups.card.removalAt")} </span>
              {formatDateTime(group.removalAt)}
            </td>
            <td className={`${cell} max-md:pt-2 md:text-right`}>
              <Button
                variant="secondary"
                disabled={busyId !== null}
                aria-busy={busyId === group.id}
                onClick={() => onRestore(group)}
              >
                {t("deletedGroups.restore")}
              </Button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
