import { useState } from "react";
import { useTranslation } from "react-i18next";
import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import { Button, buttonVariants } from "@/components/ui/button";
import { ApiError, messageKey } from "../../api/problem";
import {
  type RecycleBinEntry,
  useMergedIntoNames,
  usePermanentlyDelete,
} from "../../api/recycleBin";

/**
 * "Delete permanently" with its confirmation, for the Owner (R-013-01, BR-04, UI-15): a delete
 * closes it and the recycle bin is read again; a refused delete shows its message in the
 * confirmation (R-013-03); Cancel changes nothing (EX-013-01). The confirmation names the
 * Contacts merged into it, which are deleted too, and allows the delete only once they are read
 * (D-21).
 */
export function DeletePermanently({
  groupId,
  entry,
  disabled,
}: {
  groupId: string;
  entry: RecycleBinEntry;
  disabled: boolean;
}) {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const remove = usePermanentlyDelete(groupId);
  const merged = useMergedIntoNames(groupId, entry.contact.id, open);
  const error = remove.error ?? merged.error;
  const message =
    error instanceof ApiError ? t(messageKey(error.code), error.params) : error && t("state.error");

  return (
    <AlertDialog
      open={open}
      onOpenChange={(next) => {
        setOpen(next);
        remove.reset();
      }}
    >
      <AlertDialogTrigger className={buttonVariants({ variant: "ghost" })} disabled={disabled}>
        {t("recycleBin.permanentDelete.action")}
      </AlertDialogTrigger>
      <AlertDialogContent>
        <AlertDialogTitle>
          {t("recycleBin.permanentDelete.title", { name: entry.contact.name })}
        </AlertDialogTitle>
        <AlertDialogDescription>
          {t("recycleBin.permanentDelete.description")}
        </AlertDialogDescription>
        {merged.data && merged.data.length > 0 && (
          <p className="text-sm text-text-muted">
            {t("recycleBin.permanentDelete.alsoDeleted", { names: merged.data.join(", ") })}
          </p>
        )}
        <div className="flex flex-wrap gap-2">
          <Button
            disabled={remove.isPending || !merged.isSuccess}
            aria-busy={remove.isPending}
            onClick={() => remove.mutate(entry)}
          >
            {t("recycleBin.permanentDelete.action")}
          </Button>
          <AlertDialogCancel className={buttonVariants({ variant: "ghost" })}>
            {t("recycleBin.permanentDelete.cancel")}
          </AlertDialogCancel>
        </div>
        {message && (
          <p
            role="alert"
            className="border-l-4 border-danger pl-3 text-sm font-semibold text-danger"
          >
            {message}
          </p>
        )}
      </AlertDialogContent>
    </AlertDialog>
  );
}
