import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { AttributeRow } from "../features/contacts/attributes";
import type { Contact } from "./contacts";
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
  /** The Version a `restored` Version made current again (R-011-01); null for other kinds. */
  restoredFrom?: number | null;
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

/**
 * Makes Version `number` current as a new Version `restored`, sent with the current Version number
 * read (R-011-01, BR-06). The restored Contact replaces the one read; its History and the lists
 * are read again.
 */
export function useRestoreVersion(groupId: string, contactId: string, number: string) {
  const queryClient = useQueryClient();
  const contactKey = ["groups", groupId, "contacts", contactId];
  return useMutation({
    mutationFn: (read: Contact) =>
      apiRequest<Contact>(
        "POST",
        `/groups/${groupId}/contacts/${contactId}/versions/${number}/restore`,
        { versionNumber: read.versionNumber },
      ),
    onSuccess: async (restored) => {
      queryClient.setQueryData(contactKey, restored);
      await queryClient.invalidateQueries({ queryKey: [...contactKey, "versions"] });
      await queryClient.invalidateQueries({ queryKey: ["groups", groupId, "contacts", "list"] });
    },
    onError: () => queryClient.invalidateQueries({ queryKey: contactKey, exact: true }),
  });
}
