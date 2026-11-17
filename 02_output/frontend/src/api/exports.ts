import { useMutation } from "@tanstack/react-query";
import type { ListQuery } from "../features/contacts/listQuery";
import { apiDownload, type DownloadedFile } from "./apiClient";

/** Most Contacts in one Export (R-016-05). */
export const MAX_EXPORT = 10_000;

/** Hands the file to the browser as a download with its name. */
function save({ blob, fileName }: DownloadedFile): void {
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = fileName;
  document.body.append(link);
  link.click();
  link.remove();
  // The download has started from the click; the address is no longer needed afterwards.
  setTimeout(() => URL.revokeObjectURL(url), 0);
}

/**
 * Exports the list of the query, or only the Checked contacts within it, and saves the file
 * (`ExportRequest` of the contract, R-016-01, R-016-02). The Page is not sent: the Export is the
 * whole list.
 */
export function useExport(groupId: string, query: ListQuery) {
  return useMutation({
    mutationFn: async (contactIds?: string[]) =>
      save(
        await apiDownload(`/groups/${groupId}/exports`, {
          q: query.q,
          sort: query.sort,
          dir: query.dir,
          ...query.filters,
          ...(contactIds && { contactIds }),
        }),
      ),
  });
}
