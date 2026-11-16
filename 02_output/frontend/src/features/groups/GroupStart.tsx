import { Navigate } from "react-router";
import { useMe } from "../../api/me";
import { useGroups } from "../../api/groups";
import { lastGroupId, startGroupId } from "./currentGroup";
import { GroupsPending } from "./GroupsPending";
import { NoGroupsPage } from "./NoGroupsPage";

/** `/`: opens the Group selected last, else the first Group, else WF-05 (R-002-05, EX-002-06). */
export function GroupStart() {
  const me = useMe();
  const groups = useGroups();
  if (!groups.isSuccess) return <GroupsPending query={groups} />;
  if (!me.isSuccess) return <GroupsPending query={me} />;
  const groupId = startGroupId(groups.data, lastGroupId(me.data.id));
  if (!groupId) return <NoGroupsPage />;
  return <Navigate to={`/groups/${groupId}/contacts`} replace />;
}
