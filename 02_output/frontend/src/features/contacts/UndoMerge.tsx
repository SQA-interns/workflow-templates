import { useState } from "react";
import { useTranslation } from "react-i18next";
import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import { Button, buttonVariants } from "@/components/ui/button";
import type { Contact, MergedFrom } from "../../api/contacts";
import { useUndoMerge } from "../../api/duplicates";
import { ApiError, messageKey } from "../../api/problem";

/**
 * "Undo merge" with its confirmation (R-019-01, UI-15, WF-07): an undo closes the confirmation and
 * the Contact is read again; a refused undo shows its message in the confirmation (R-019-02,
 * R-019-03); Cancel changes nothing (EX-019-03).
 */
export function UndoMerge({
  groupId,
  contact,
  merge,
}: {
  groupId: string;
  contact: Contact;
  merge: MergedFrom;
}) {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const undo = useUndoMerge(groupId, contact.id);
  const error = undo.error;
  const message =
    error instanceof ApiError ? t(messageKey(error.code), error.params) : error && t("state.error");

  return (
    <AlertDialog
      open={open}
      onOpenChange={(next) => {
        undo.reset();
        setOpen(next);
      }}
    >
      <AlertDialogTrigger className={buttonVariants({ variant: "secondary" })}>
        {t("merge.undo.action")}
      </AlertDialogTrigger>
      <AlertDialogContent aria-describedby={undefined}>
        <AlertDialogTitle>{t("merge.undo.title")}</AlertDialogTitle>
        <div className="flex flex-wrap gap-2">
          <Button
            disabled={undo.isPending}
            aria-busy={undo.isPending}
            onClick={() =>
              undo.mutate(
                {
                  mergeId: merge.mergeId,
                  otherId: merge.contact.id,
                  versionNumber: contact.versionNumber,
                },
                { onSuccess: () => setOpen(false) },
              )
            }
          >
            {t("merge.undo.action")}
          </Button>
          <AlertDialogCancel className={buttonVariants({ variant: "ghost" })}>
            {t("merge.cancel")}
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
