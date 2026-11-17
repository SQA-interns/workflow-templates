import { type FormEvent, useId, useState } from "react";
import { useTranslation } from "react-i18next";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { NativeSelect } from "@/components/ui/native-select";
import {
  type InvitableRole,
  type Invitation,
  useCreateInvitation,
  useOpenInvitations,
  useRevokeInvitation,
} from "../../api/invitations";
import { ApiError, messageKey } from "../../api/problem";
import { formatDateTime } from "../../i18n/format";
import { EMAIL_PATTERN } from "../contacts/contactFields";
import { GroupsPending } from "../groups/GroupsPending";

const EMAIL_INVALID = "errors.field.emailInvalid";

/** WF-13 for the Owner: Invite a Member (R-004-01, R-004-02) and Open Invitations (R-004-08). */
export function InvitationsSection({ groupId }: { groupId: string }) {
  return (
    <>
      <InviteMember groupId={groupId} />
      <OpenInvitations groupId={groupId} />
    </>
  );
}

/** The form; the link of a new Invitation is shown once with "Copy link" (AC-004-01). */
function InviteMember({ groupId }: { groupId: string }) {
  const { t } = useTranslation();
  const id = useId();
  const create = useCreateInvitation(groupId);
  const [email, setEmail] = useState("");
  const [role, setRole] = useState<InvitableRole>("EDITOR");
  const [errorKey, setErrorKey] = useState<string | null>(null);

  function submit(event: FormEvent) {
    event.preventDefault();
    create.reset();
    const trimmed = email.trim();
    if (!EMAIL_PATTERN.test(trimmed)) {
      setErrorKey(EMAIL_INVALID);
      return;
    }
    setErrorKey(null);
    create.mutate(
      { email: trimmed, role },
      { onSuccess: () => setEmail(""), onError: (error) => setErrorKey(errorKeyOf(error)) },
    );
  }

  return (
    <Card>
      <form className="flex flex-col items-start gap-3" onSubmit={submit} noValidate>
        <h2 className="text-lg font-semibold">{t("invitations.invite.title")}</h2>
        <div className="grid w-full gap-3 md:grid-cols-2">
          <div className="flex flex-col gap-1">
            <Label htmlFor={`${id}-email`}>
              {t("invitations.invite.email")}
              <span aria-hidden="true"> *</span>
            </Label>
            <Input
              id={`${id}-email`}
              type="email"
              autoComplete="off"
              aria-required="true"
              aria-invalid={errorKey === EMAIL_INVALID}
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>
          <div className="flex flex-col gap-1">
            <Label htmlFor={`${id}-role`}>
              {t("invitations.invite.role")}
              <span aria-hidden="true"> *</span>
            </Label>
            <NativeSelect
              id={`${id}-role`}
              value={role}
              onChange={(e) => setRole(e.target.value as InvitableRole)}
            >
              <option value="EDITOR">{t("roles.EDITOR")}</option>
              <option value="VIEWER">{t("roles.VIEWER")}</option>
            </NativeSelect>
          </div>
        </div>
        {errorKey && (
          <p role="alert" className="text-sm font-semibold text-danger">
            {t(errorKey)}
          </p>
        )}
        <Button type="submit" disabled={create.isPending} aria-busy={create.isPending}>
          {t("invitations.invite.submit")}
        </Button>
        {create.isSuccess && <InvitationLink link={create.data.link} />}
      </form>
    </Card>
  );
}

function errorKeyOf(error: Error): string {
  if (!(error instanceof ApiError)) return "state.error";
  return messageKey(error.errors[0]?.code ?? error.code);
}

/** The link, shown only now; the Owner sends it (Q-35). */
function InvitationLink({ link }: { link: string }) {
  const { t } = useTranslation();
  const [copied, setCopied] = useState<"done" | "failed" | null>(null);

  function copy() {
    navigator.clipboard.writeText(link).then(
      () => setCopied("done"),
      () => setCopied("failed"),
    );
  }

  return (
    <div className="flex w-full flex-col gap-2 rounded-md border border-border bg-muted p-3">
      <p>
        <strong>{t("invitations.invite.linkTitle")}</strong> {t("invitations.invite.linkHint")}
      </p>
      <div className="flex w-full flex-wrap items-center gap-2">
        <code className="min-w-0 flex-1 basis-52 rounded-md border border-input bg-background px-3 py-2 text-sm break-all">
          {link}
        </code>
        <Button type="button" variant="secondary" onClick={copy}>
          {t("invitations.invite.copy")}
        </Button>
      </div>
      {copied && (
        <p role="status" className="text-sm">
          {t(copied === "done" ? "invitations.invite.copied" : "invitations.invite.copyFailed")}
        </p>
      )}
    </div>
  );
}

/** The open Invitations: a table from 768 px, stacked cards below it (UI-16); four states. */
function OpenInvitations({ groupId }: { groupId: string }) {
  const { t } = useTranslation();
  const open = useOpenInvitations(groupId);
  const revoke = useRevokeInvitation(groupId);

  return (
    <Card className="flex flex-col gap-3">
      <h2 className="text-lg font-semibold">{t("invitations.open.title")}</h2>
      {!open.isSuccess ? (
        <GroupsPending query={open} />
      ) : open.data.length === 0 ? (
        <p>{t("invitations.open.empty")}</p>
      ) : (
        <InvitationTable
          invitations={open.data}
          busyId={revoke.isPending ? revoke.variables : null}
          onRevoke={(invitation) => revoke.mutate(invitation.id)}
        />
      )}
      {revoke.isError && (
        <p role="alert" className="text-sm font-semibold text-danger">
          {t(errorKeyOf(revoke.error))}
        </p>
      )}
    </Card>
  );
}

function InvitationTable({
  invitations,
  busyId,
  onRevoke,
}: {
  invitations: Invitation[];
  busyId: string | null;
  onRevoke: (invitation: Invitation) => void;
}) {
  const { t } = useTranslation();
  const cell = "px-3 py-2 max-md:block max-md:p-0";
  const head = "px-3 py-2 text-sm font-semibold";
  return (
    <table className="w-full border-collapse text-left max-md:block">
      <thead className="max-md:sr-only">
        <tr className="border-b border-border">
          <th scope="col" className={head}>
            {t("invitations.open.column.email")}
          </th>
          <th scope="col" className={head}>
            {t("invitations.open.column.role")}
          </th>
          <th scope="col" className={head}>
            {t("invitations.open.column.expiresAt")}
          </th>
          <th scope="col" className="px-3 py-2">
            <span className="sr-only">{t("invitations.open.column.actions")}</span>
          </th>
        </tr>
      </thead>
      <tbody className="max-md:flex max-md:flex-col max-md:gap-3">
        {invitations.map((invitation) => (
          <tr
            key={invitation.id}
            className="border-b border-border max-md:block max-md:rounded-lg max-md:border max-md:p-3"
          >
            <td className={`${cell} font-semibold break-all`}>{invitation.email}</td>
            <td className={`${cell} max-md:inline max-md:text-text-muted`}>
              {t(`roles.${invitation.role}`)}
            </td>
            <td className={`${cell} max-md:inline max-md:text-text-muted`}>
              <span className="md:hidden"> · {t("invitations.open.card.expires")} </span>
              {formatDateTime(invitation.expiresAt)}
            </td>
            <td className={`${cell} max-md:pt-2 md:text-right`}>
              <Button
                variant="ghost"
                disabled={busyId !== null}
                aria-busy={busyId === invitation.id}
                onClick={() => onRevoke(invitation)}
              >
                {t("invitations.open.revoke")}
              </Button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
