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
import { type Group, useMoveGroupToRecycleBin } from "../../api/groups";
import { ApiError, messageKey } from "../../api/problem";

/**
 * "Move Group to recycle bin" with its confirmation (R-003-02, UI-15): a move opens the start, which
 * shows another Group or WF-05; Cancel changes nothing (EX-003-04).
 */
export function MoveGroupToRecycleBin({ group }: { group: Group }) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const move = useMoveGroupToRecycleBin(group.id);
  const error = move.error;
  const message =
    error instanceof ApiError ? t(messageKey(error.code), error.params) : error && t("state.error");

  return (
    <AlertDialog onOpenChange={() => move.reset()}>
      <AlertDialogTrigger className={buttonVariants({ variant: "secondary" })}>
        {t("groups.settings.move.action")}
      </AlertDialogTrigger>
      <AlertDialogContent>
        <AlertDialogTitle>
          {t("groups.settings.move.confirm", { name: group.name })}
        </AlertDialogTitle>
        <AlertDialogDescription>{t("groups.settings.move.description")}</AlertDialogDescription>
        <div className="flex flex-wrap gap-2">
          <Button
            disabled={move.isPending}
            aria-busy={move.isPending}
            onClick={() => move.mutate(undefined, { onSuccess: () => void navigate("/") })}
          >
            {t("groups.settings.move.action")}
          </Button>
          <AlertDialogCancel className={buttonVariants({ variant: "ghost" })}>
            {t("groups.settings.move.cancel")}
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
