import { useEffect } from "react";
import { Outlet, useParams } from "react-router";
import { useMe } from "../../api/me";
import { useGroups } from "../../api/groups";
import { NotFoundPage } from "../../app/pages";
import { rememberGroup } from "./currentGroup";
import { GroupsPending } from "./GroupsPending";

/**
 * `/groups/:groupId/*`: the screens of one Group of the User; another Group is not found (BR-02).
 * The Group shown is remembered as the Group selected last (R-002-04, R-002-05).
 */
export function GroupRoute() {
  const { groupId } = useParams();
  const me = useMe();
  const groups = useGroups();
  const isMember = groups.data?.some((group) => group.id === groupId) ?? false;
  const userId = me.data?.id;

  useEffect(() => {
    if (isMember && userId && groupId) rememberGroup(userId, groupId);
  }, [isMember, userId, groupId]);

  if (!groups.isSuccess) return <GroupsPending query={groups} />;
  return isMember ? <Outlet /> : <NotFoundPage />;
}
