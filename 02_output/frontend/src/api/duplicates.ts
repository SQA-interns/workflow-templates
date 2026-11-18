import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiRequest } from "./apiClient";

/** Why two Contacts may be the same (`DuplicateCandidate.reasons`, R-017-02 … R-017-04). */
export type DuplicateReason = "sameEmail" | "samePhone" | "similarNameSameCompany";

/** A pair of Possible duplicates with every reason (`DuplicateCandidate`, R-017-01). */
export interface DuplicateCandidate {
  first: { id: string; name: string };
  second: { id: string; name: string };
  reasons: DuplicateReason[];
}

/** The Group's Possible duplicates; read only, every Member (R-017-01, R-017-05). */
export function useDuplicates(groupId: string) {
  return useQuery({
    queryKey: ["groups", groupId, "duplicates"],
    queryFn: () => apiRequest<DuplicateCandidate[]>("GET", `/groups/${groupId}/duplicates`),
  });
}

/** Keep the Primary contact's or the Other contact's value (`Choice` of the contract). */
export type Choice = "primary" | "other";

/** A Merge as the User confirms it (`MergeRequest` of the contract, R-018-02). */
export interface MergeRequest {
  primaryId: string;
  otherId: string;
  primaryVersionNumber: number;
  otherVersionNumber: number;
  fieldChoices: Record<string, Choice>;
  attributeChoices: Record<string, Choice>;
}

/**
 * Merges the Other contact into the Primary contact (R-018-02 … R-018-04); afterwards the lists,
 * the Possible duplicates and both Contacts are read again.
 */
export function useMerge(groupId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: MergeRequest) =>
      apiRequest<unknown>("POST", `/groups/${groupId}/merges`, request),
    onSuccess: async (_merge, request) => {
      const contacts = ["groups", groupId, "contacts"];
      await queryClient.invalidateQueries({ queryKey: [...contacts, "list"] });
      await queryClient.invalidateQueries({ queryKey: [...contacts, request.primaryId] });
      await queryClient.invalidateQueries({ queryKey: [...contacts, request.otherId] });
      await queryClient.invalidateQueries({ queryKey: ["groups", groupId, "duplicates"] });
    },
  });
}

/** The Merge to undo and the Primary contact's Version number read (BR-06). */
export interface UndoMergeRequest {
  mergeId: string;
  otherId: string;
  versionNumber: number;
}

/**
 * Undoes a Merge into the Primary contact (R-019-01); afterwards the lists, the Possible duplicates
 * and both Contacts are read again, after a refusal the Primary contact (R-019-02, R-019-03).
 */
export function useUndoMerge(groupId: string, primaryId: string) {
  const queryClient = useQueryClient();
  const contacts = ["groups", groupId, "contacts"];
  return useMutation({
    mutationFn: ({ mergeId, versionNumber }: UndoMergeRequest) =>
      apiRequest<unknown>("POST", `/groups/${groupId}/merges/${mergeId}/undo`, { versionNumber }),
    onSuccess: async (_merge, request) => {
      await queryClient.invalidateQueries({ queryKey: [...contacts, "list"] });
      await queryClient.invalidateQueries({ queryKey: [...contacts, primaryId] });
      await queryClient.invalidateQueries({ queryKey: [...contacts, request.otherId] });
      await queryClient.invalidateQueries({ queryKey: ["groups", groupId, "duplicates"] });
    },
    onError: () =>
      queryClient.invalidateQueries({ queryKey: [...contacts, primaryId], exact: true }),
  });
}
