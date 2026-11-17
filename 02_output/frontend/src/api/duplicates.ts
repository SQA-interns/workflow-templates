import { useQuery } from "@tanstack/react-query";
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
