import { useQuery } from "@tanstack/react-query";
import type { AttributeRow } from "../features/contacts/attributes";
import type { ContactFields } from "../features/contacts/contactFields";
import { apiRequest } from "./apiClient";

/** One changed item of a Version (`Change` of the contract). */
export interface Change {
  item: string;
  oldValue: string | null;
  newValue: string | null;
}

/** One entry of the History (`VersionSummary` of the contract, R-010-01). */
export interface VersionSummary {
  number: number;
  kind: string;
  changedBy: string;
  changedAt: string;
  changes: Change[];
}

/** One Version with the content after it (`Version` of the contract, R-010-02). */
export interface Version extends VersionSummary {
  snapshot: { fields: ContactFields; attributes: AttributeRow[]; tags: string[] };
}

/** Every Version of the Contact, newest first (R-010-01). */
export function useHistory(groupId: string, contactId: string) {
  return useQuery({
    queryKey: ["groups", groupId, "contacts", contactId, "versions"],
    queryFn: () =>
      apiRequest<VersionSummary[]>("GET", `/groups/${groupId}/contacts/${contactId}/versions`),
  });
}

/** Version `number` of the Contact with the content after it (R-010-02). */
export function useVersion(groupId: string, contactId: string, number: string) {
  return useQuery({
    queryKey: ["groups", groupId, "contacts", contactId, "versions", number],
    queryFn: () =>
      apiRequest<Version>("GET", `/groups/${groupId}/contacts/${contactId}/versions/${number}`),
  });
}
