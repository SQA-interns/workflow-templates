import { useQuery } from "@tanstack/react-query";
import { apiRequest } from "./apiClient";
import type { Tag } from "./contacts";

/** Longest typed text the API takes (contract parameter `q` of `suggestTags`). */
const MAX_TEXT_LENGTH = 50;

/**
 * The Group's Tags on an active Contact that contain the text (R-009-02); nothing is asked for an
 * empty or too long text.
 */
export function useTagSuggestions(groupId: string, text: string) {
  return useQuery({
    queryKey: ["groups", groupId, "tags", text],
    queryFn: () =>
      apiRequest<Tag[]>("GET", `/groups/${groupId}/tags?q=${encodeURIComponent(text)}`),
    enabled: text !== "" && [...text].length <= MAX_TEXT_LENGTH,
  });
}

/** Every Tag of the Group on an active Contact, A–Z, for the Tag filters (R-015-01, R-015-02). */
export function useGroupTags(groupId: string) {
  return useQuery({
    queryKey: ["groups", groupId, "tags", ""],
    queryFn: () => apiRequest<Tag[]>("GET", `/groups/${groupId}/tags`),
  });
}
