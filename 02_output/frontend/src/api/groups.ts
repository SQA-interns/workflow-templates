import { useMutation, useQueryClient } from "@tanstack/react-query";
import { apiRequest } from "./apiClient";

/** A Group of the signed-in User with the User's Role (`Group` of the contract). */
export interface Group {
  id: string;
  name: string;
  role: "OWNER" | "EDITOR" | "VIEWER";
}

export const groupsQueryKey = ["groups"] as const;

/** Creates a Group with the User as Owner (R-002-01); the Group list is read again. */
export function useCreateGroup() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (name: string) => apiRequest<Group>("POST", "/groups", { name }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: groupsQueryKey }),
  });
}
