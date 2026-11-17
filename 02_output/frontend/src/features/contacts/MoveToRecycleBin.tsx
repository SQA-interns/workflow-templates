import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router";
import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import { Button, buttonVariants } from "@/components/ui/button";
import type { Contact } from "../../api/contacts";
import { ApiError, messageKey } from "../../api/problem";
import { useMoveToRecycleBin } from "../../api/recycleBin";

/**
 * "Move to recycle bin" with its confirmation (R-012-01, UI-15): a move opens the Contact list; a
 * refused move shows its message in the confirmation (BR-06); Cancel changes nothing (EX-012-03).
 */
export function MoveToRecycleBin({ groupId, contact }: { groupId: string; contact: Contact }) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const move = useMoveToRecycleBin(groupId, contact.id);
  const error = move.error;
  const message =
    error instanceof ApiError ? t(messageKey(error.code), error.params) : error && t("state.error");

  return (
    <AlertDialog onOpenChange={() => move.reset()}>
      <AlertDialogTrigger className={buttonVariants({ variant: "ghost" })}>
        {t("recycleBin.move.action")}
      </AlertDialogTrigger>
      <AlertDialogContent>
        <AlertDialogTitle>{t("recycleBin.move.title")}</AlertDialogTitle>
        <AlertDialogDescription>{t("recycleBin.move.description")}</AlertDialogDescription>
        <div className="flex flex-wrap gap-2">
          <Button
            disabled={move.isPending}
            aria-busy={move.isPending}
            onClick={() =>
              move.mutate(contact, {
                onSuccess: () => void navigate(`/groups/${groupId}/contacts`),
              })
            }
          >
            {t("recycleBin.move.action")}
          </Button>
          <AlertDialogCancel className={buttonVariants({ variant: "ghost" })}>
            {t("recycleBin.move.cancel")}
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
