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
import { Card } from "@/components/ui/card";
import { NativeSelect } from "@/components/ui/native-select";
import type { InvitableRole } from "../../api/invitations";
import { type Member, useChangeRole, useMembers, useRemoveMember } from "../../api/members";
import { ApiError, messageKey } from "../../api/problem";
import { GroupsPending } from "../groups/GroupsPending";

/**
 * WF-13 Members for the Owner (R-005-01): a table from 768 px, stacked cards below it (UI-16). The
 * Role of a Member changes as soon as another is chosen (R-005-02); the Owner's Role and Membership
 * cannot be changed (R-005-05).
 */
export function MemberList({ groupId }: { groupId: string }) {
  const { t } = useTranslation();
  const members = useMembers(groupId);
  const changeRole = useChangeRole(groupId);

  return (
    <Card className="flex flex-col gap-3">
      {!members.isSuccess ? (
        <GroupsPending query={members} />
      ) : (
        <MemberTable
          groupId={groupId}
          members={members.data}
          busyId={changeRole.isPending ? changeRole.variables.userId : null}
          onRoleChange={(userId, role) => changeRole.mutate({ userId, role })}
        />
      )}
      {changeRole.isError && (
        <p role="alert" className="border-l-4 border-danger pl-3 text-sm font-semibold text-danger">
          {t(problemKey(changeRole.error))}
        </p>
      )}
    </Card>
  );
}

function problemKey(error: Error): string {
  return error instanceof ApiError ? messageKey(error.code) : "state.error";
}

function MemberTable({
  groupId,
  members,
  busyId,
  onRoleChange,
}: {
  groupId: string;
  members: Member[];
  busyId: string | null;
  onRoleChange: (userId: string, role: InvitableRole) => void;
}) {
  const { t } = useTranslation();
  const cell = "px-3 py-2 max-md:block max-md:p-0";
  const head = "px-3 py-2 text-sm font-semibold";
  return (
    <table className="w-full border-collapse text-left max-md:block">
      <thead className="max-md:sr-only">
        <tr className="border-b border-border">
          <th scope="col" className={head}>
            {t("members.column.name")}
          </th>
          <th scope="col" className={head}>
            {t("members.column.email")}
          </th>
          <th scope="col" className={head}>
            {t("members.column.role")}
          </th>
          <th scope="col" className="px-3 py-2">
            <span className="sr-only">{t("members.column.actions")}</span>
          </th>
        </tr>
      </thead>
      <tbody className="max-md:flex max-md:flex-col max-md:gap-3">
        {members.map((member) => (
          <tr
            key={member.userId}
            className="border-b border-border max-md:block max-md:rounded-lg max-md:border max-md:p-3"
          >
            <td className={`${cell} font-semibold`}>{member.displayName}</td>
            <td
              className={`${cell} break-all max-md:text-text-muted md:break-normal md:wrap-break-word`}
            >
              {member.email}
            </td>
            <td className={`${cell} max-md:inline-block max-md:pt-2 max-md:align-middle`}>
              {member.role === "OWNER" ? (
                t("roles.OWNER")
              ) : (
                <NativeSelect
                  className="max-w-40"
                  aria-label={t("members.roleOf", { name: member.displayName })}
                  value={member.role}
                  disabled={busyId !== null}
                  aria-busy={busyId === member.userId}
                  onChange={(e) => onRoleChange(member.userId, e.target.value as InvitableRole)}
                >
                  <option value="EDITOR">{t("roles.EDITOR")}</option>
                  <option value="VIEWER">{t("roles.VIEWER")}</option>
                </NativeSelect>
              )}
            </td>
            <td
              className={`${cell} max-md:ml-2 max-md:inline-block max-md:pt-2 max-md:align-middle md:text-right`}
            >
              {member.role !== "OWNER" && <RemoveMember groupId={groupId} member={member} />}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

/**
 * "Remove from Group" with its confirmation (R-005-03, UI-15): a removal closes it and the Members
 * are read again; a refused one shows its message in the confirmation; Cancel changes nothing
 * (EX-005-04).
 */
function RemoveMember({ groupId, member }: { groupId: string; member: Member }) {
  const { t } = useTranslation();
  const remove = useRemoveMember(groupId);

  return (
    <AlertDialog onOpenChange={() => remove.reset()}>
      <AlertDialogTrigger className={buttonVariants({ variant: "danger-ghost" })}>
        {t("members.remove.action")}
      </AlertDialogTrigger>
      <AlertDialogContent>
        <AlertDialogTitle>
          {t("members.remove.title", { name: member.displayName })}
        </AlertDialogTitle>
        <AlertDialogDescription>
          {t("members.remove.description", { name: member.displayName })}
        </AlertDialogDescription>
        <div className="flex flex-wrap gap-2">
          <Button
            variant="danger"
            disabled={remove.isPending}
            aria-busy={remove.isPending}
            onClick={() => remove.mutate(member.userId)}
          >
            {t("members.remove.action")}
          </Button>
          <AlertDialogCancel className={buttonVariants({ variant: "ghost" })}>
            {t("members.remove.cancel")}
          </AlertDialogCancel>
        </div>
        {remove.isError && (
          <p
            role="alert"
            className="border-l-4 border-danger pl-3 text-sm font-semibold text-danger"
          >
            {t(problemKey(remove.error))}
          </p>
        )}
      </AlertDialogContent>
    </AlertDialog>
  );
}
