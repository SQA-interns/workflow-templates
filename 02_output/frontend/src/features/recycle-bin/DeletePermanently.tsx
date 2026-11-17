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
import { type RecycleBinEntry, usePermanentlyDelete } from "../../api/recycleBin";

/**
 * "Delete permanently" with its confirmation, for the Owner (R-013-01, BR-04, UI-15): a delete
 * closes it and the recycle bin is read again; a refused delete shows its message in the
 * confirmation (R-013-03); Cancel changes nothing (EX-013-01).
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
  const remove = usePermanentlyDelete(groupId);
  const error = remove.error;
  const message =
    error instanceof ApiError ? t(messageKey(error.code), error.params) : error && t("state.error");

  return (
    <AlertDialog onOpenChange={() => remove.reset()}>
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
        <div className="flex flex-wrap gap-2">
          <Button
            disabled={remove.isPending}
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
