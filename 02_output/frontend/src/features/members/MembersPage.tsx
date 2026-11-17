import { useTranslation } from "react-i18next";
import { useParams } from "react-router";
import { useGroups } from "../../api/groups";
import { GroupsPending } from "../groups/GroupsPending";
import { InvitationsSection } from "./InvitationsSection";

/**
 * WF-13 Members and Invitations, for the Owner only (BR-04). US-004 adds Invite a Member and Open
 * Invitations; the Member list comes with US-005.
 */
export function MembersPage() {
  const { t } = useTranslation();
  const { groupId = "" } = useParams();
  const groups = useGroups();
  const group = groups.data?.find((candidate) => candidate.id === groupId);

  if (!groups.isSuccess) return <GroupsPending query={groups} />;
  if (group?.role !== "OWNER") return <p role="alert">{t("errors.auth.forbidden")}</p>;
  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-2xl font-semibold">{t("members.title")}</h1>
      <InvitationsSection groupId={group.id} />
    </div>
  );
}
