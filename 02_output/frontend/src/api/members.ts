import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiRequest } from "./apiClient";
import type { InvitableRole } from "./invitations";

/** A Member as the Owner sees it (`Member` of the contract, R-005-01). */
export interface Member {
  userId: string;
  displayName: string;
  email: string;
  role: "OWNER" | InvitableRole;
}

const membersKey = (groupId: string) => ["groups", groupId, "members"] as const;

/** The Group's Members with display name, e-mail and Role; Owner only (R-005-01). */
export function useMembers(groupId: string) {
  return useQuery({
    queryKey: membersKey(groupId),
    queryFn: () => apiRequest<Member[]>("GET", `/groups/${groupId}/members`),
  });
}

/** Changes a Member between Editor and Viewer (R-005-02); the answer replaces the Member. */
export function useChangeRole(groupId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ userId, role }: { userId: string; role: InvitableRole }) =>
      apiRequest<Member>("PATCH", `/groups/${groupId}/members/${userId}`, { role }),
    onSuccess: (changed) =>
      queryClient.setQueryData<Member[]>(membersKey(groupId), (members) =>
        members?.map((member) => (member.userId === changed.userId ? changed : member)),
      ),
  });
}
