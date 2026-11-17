import { ArrowLeft } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate, useParams } from "react-router";
import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import { Button, buttonVariants } from "@/components/ui/button";
import { useContact } from "../../api/contacts";
import { useCanChangeContacts } from "../../api/groups";
import { useRestoreVersion, useVersion } from "../../api/history";
import { ApiError, messageKey } from "../../api/problem";
import { NotFoundPage } from "../../app/pages";
import { formatDateTime } from "../../i18n/format";
import { AttributesSection, FieldsSection, TagsSection } from "../contacts/ContactContent";
import { contactName } from "../contacts/contactFields";
import { GroupsPending } from "../groups/GroupsPending";

/**
 * WF-10 Version (R-010-02): the full content of the Contact after one Version, read-only; the
 * Owner and Editors may restore it (US-011).
 */
export function VersionPage() {
  const { t } = useTranslation();
  const { groupId = "", contactId = "", number = "" } = useParams();
  const version = useVersion(groupId, contactId, number);
  const canRestore = useCanChangeContacts(groupId);

  if (version.error instanceof ApiError && version.error.status === 404) return <NotFoundPage />;
  if (!version.isSuccess) return <GroupsPending query={version} />;
  const { kind, changedBy, changedAt, snapshot } = version.data;
  return (
    <div className="flex flex-col gap-4">
      <Link
        to={`/groups/${groupId}/contacts/${contactId}/history`}
        className="inline-flex min-h-11 items-center gap-1 self-start underline"
      >
        <ArrowLeft aria-hidden="true" className="size-4" />
        {t("contacts.details.history")}
      </Link>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold break-words">
          {t("history.versionOf", {
            number: version.data.number,
            name: contactName(snapshot.fields),
          })}
        </h1>
        {canRestore && <RestoreVersion number={version.data.number} />}
      </div>
      <p className="text-text-muted">
        {[
          t(`history.kind.${kind}`),
          changedBy,
          formatDateTime(changedAt),
          t("history.readOnly"),
        ].join(" · ")}
      </p>
      <div className="grid gap-4 md:grid-cols-2">
        <FieldsSection fields={snapshot.fields} />
        <div className="flex flex-col gap-4">
          <AttributesSection attributes={snapshot.attributes} />
          <TagsSection names={snapshot.tags} />
        </div>
      </div>
    </div>
  );
}

/**
 * "Restore this Version" with its confirmation (UI-15): restoring opens the History with the new
 * Version; a refused restore shows its message in the confirmation (R-011-03, EX-011-04, BR-06).
 */
function RestoreVersion({ number }: { number: number }) {
  const { t } = useTranslation();
  const { groupId = "", contactId = "" } = useParams();
  const navigate = useNavigate();
  const contact = useContact(groupId, contactId);
  const restore = useRestoreVersion(groupId, contactId, String(number));
  const error = restore.error;
  const message =
    error instanceof ApiError ? t(messageKey(error.code), error.params) : error && t("state.error");

  return (
    <AlertDialog onOpenChange={() => restore.reset()}>
      <AlertDialogTrigger className={buttonVariants()}>
        {t("history.restore.action")}
      </AlertDialogTrigger>
      <AlertDialogContent>
        <AlertDialogTitle>{t("history.restore.title")}</AlertDialogTitle>
        <AlertDialogDescription>
          {t("history.restore.description", { number })}
        </AlertDialogDescription>
        <div className="flex flex-wrap gap-2">
          <Button
            disabled={!contact.isSuccess || restore.isPending}
            aria-busy={restore.isPending}
            onClick={() =>
              contact.data &&
              restore.mutate(contact.data, {
                onSuccess: () => void navigate(`/groups/${groupId}/contacts/${contactId}/history`),
              })
            }
          >
            {t("history.restore.action")}
          </Button>
          <AlertDialogCancel className={buttonVariants({ variant: "ghost" })}>
            {t("history.restore.cancel")}
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
