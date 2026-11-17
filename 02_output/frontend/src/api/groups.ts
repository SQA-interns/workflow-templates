import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiRequest } from "./apiClient";

/** A Group of the signed-in User with the User's Role (`Group` of the contract). */
export interface Group {
  id: string;
  name: string;
  role: "OWNER" | "EDITOR" | "VIEWER";
}

export const groupsQueryKey = ["groups"] as const;

/** The active Groups of the User, by name A–Z (R-002-03). */
export function useGroups() {
  return useQuery({
    queryKey: groupsQueryKey,
    queryFn: () => apiRequest<Group[]>("GET", "/groups"),
  });
}

/** Whether the User may change the Group's Contacts: Owner or Editor, not Viewer (BR-03). */
export function useCanChangeContacts(groupId: string): boolean {
  const role = useGroups().data?.find((group) => group.id === groupId)?.role;
  return role === "OWNER" || role === "EDITOR";
}

/** Whether the User is the Owner of the Group (BR-04). */
export function useIsOwner(groupId: string): boolean {
  return useGroups().data?.find((group) => group.id === groupId)?.role === "OWNER";
}

/** Creates a Group with the User as Owner (R-002-01); the Group list is read again. */
export function useCreateGroup() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (name: string) => apiRequest<Group>("POST", "/groups", { name }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: groupsQueryKey }),
  });
}

/** Renames the Group; Owner only (R-003-01); the Group list is read again. */
export function useRenameGroup(groupId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (name: string) => apiRequest<Group>("PATCH", `/groups/${groupId}`, { name }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: groupsQueryKey }),
  });
}

/** Moves the Group to the recycle bin; Owner only (R-003-02); the Group list is read again. */
export function useMoveGroupToRecycleBin(groupId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => apiRequest<undefined>("POST", `/groups/${groupId}/move-to-recycle-bin`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: groupsQueryKey }),
  });
}
