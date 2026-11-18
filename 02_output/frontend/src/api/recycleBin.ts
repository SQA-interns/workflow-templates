import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { Contact } from "./contacts";
import { apiRequest } from "./apiClient";

/** A Contact in the recycle bin (`RecycleBinEntry` of the contract, R-012-02). */
export interface RecycleBinEntry {
  contact: { id: string; name: string };
  versionNumber: number;
  deletedBy: string;
  deletedAt: string;
  removalAt: string;
}

/** The Group's Contacts in the recycle bin, newest first; Owner and Editors (R-012-02). */
export function useRecycleBin(groupId: string) {
  return useQuery({
    queryKey: ["groups", groupId, "recycle-bin"],
    queryFn: () => apiRequest<RecycleBinEntry[]>("GET", `/groups/${groupId}/recycle-bin`),
  });
}

/**
 * The names of the Contacts a permanent delete of the Contact also deletes: those merged into it
 * and not undone, and those merged into one of them (D-21). Read with `GET /contacts/{id}`, which
 * answers a Contact in the recycle bin too; asked only while `enabled`.
 */
export function useMergedIntoNames(groupId: string, contactId: string, enabled: boolean) {
  return useQuery({
    queryKey: ["groups", groupId, "contacts", contactId, "merged-into"],
    queryFn: () => mergedIntoNames(groupId, contactId),
    enabled,
  });
}

async function mergedIntoNames(groupId: string, contactId: string): Promise<string[]> {
  const read = await apiRequest<Contact>("GET", `/groups/${groupId}/contacts/${contactId}`);
  const merged = read.mergedFrom.filter((link) => !link.undone).map((link) => link.contact);
  const deeper = await Promise.all(merged.map((ref) => mergedIntoNames(groupId, ref.id)));
  return [...merged.map((ref) => ref.name), ...deeper.flat()];
}

/**
 * Restores a Contact from the recycle bin with the Version number read (R-012-03, BR-06). The
 * recycle bin, the Contact and the lists are read again, also after a refusal (R-012-04).
 */
export function useRestoreFromRecycleBin(groupId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (entry: RecycleBinEntry) =>
      apiRequest<Contact>("POST", `/groups/${groupId}/recycle-bin/${entry.contact.id}/restore`, {
        versionNumber: entry.versionNumber,
      }),
    onSettled: async (_restored, _error, entry) => {
      await queryClient.invalidateQueries({ queryKey: ["groups", groupId, "recycle-bin"] });
      await queryClient.invalidateQueries({
        queryKey: ["groups", groupId, "contacts", entry.contact.id],
        exact: true,
      });
      await queryClient.invalidateQueries({ queryKey: ["groups", groupId, "contacts", "list"] });
    },
  });
}

/**
 * Moves the Contact to the recycle bin, sent with the current Version number read (R-012-01,
 * BR-06). The Contact, the lists and the recycle bin are read again.
 */
export function useMoveToRecycleBin(groupId: string, contactId: string) {
  const queryClient = useQueryClient();
  const contactKey = ["groups", groupId, "contacts", contactId];
  return useMutation({
    mutationFn: (read: Contact) =>
      apiRequest<Contact>("POST", `/groups/${groupId}/contacts/${contactId}/move-to-recycle-bin`, {
        versionNumber: read.versionNumber,
      }),
    onSuccess: async (moved) => {
      queryClient.setQueryData(contactKey, moved);
      await queryClient.invalidateQueries({ queryKey: ["groups", groupId, "contacts", "list"] });
      await queryClient.invalidateQueries({ queryKey: ["groups", groupId, "recycle-bin"] });
    },
    onError: () => queryClient.invalidateQueries({ queryKey: contactKey, exact: true }),
  });
}

/**
 * Permanently deletes a Contact in the recycle bin (R-013-01); the Owner only (BR-04). The recycle
 * bin is read again, the Contact is forgotten and the lists and Tags are read again.
 */
export function usePermanentlyDelete(groupId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (entry: RecycleBinEntry) =>
      apiRequest<undefined>("DELETE", `/groups/${groupId}/recycle-bin/${entry.contact.id}`),
    onSuccess: async (_deleted, entry) => {
      queryClient.removeQueries({ queryKey: ["groups", groupId, "contacts", entry.contact.id] });
      await queryClient.invalidateQueries({ queryKey: ["groups", groupId, "recycle-bin"] });
      await queryClient.invalidateQueries({ queryKey: ["groups", groupId, "contacts", "list"] });
      await queryClient.invalidateQueries({ queryKey: ["groups", groupId, "tags"] });
    },
  });
}
