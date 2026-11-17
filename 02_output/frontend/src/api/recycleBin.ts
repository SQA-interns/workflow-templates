import { useMutation, useQueryClient } from "@tanstack/react-query";
import type { Contact } from "./contacts";
import { apiRequest } from "./apiClient";

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
