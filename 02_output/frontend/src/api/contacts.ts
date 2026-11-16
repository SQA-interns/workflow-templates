import { useMutation } from "@tanstack/react-query";
import type { ContactFields } from "../features/contacts/contactFields";
import { apiRequest } from "./apiClient";

/** A Contact as the API answers it (`Contact` of the contract), as far as the UI reads it. */
export interface Contact {
  id: string;
  versionNumber: number;
  fields: ContactFields;
}

/** Adds a Contact to the Group (R-006-01); Attributes and Tags follow with US-008 and US-009. */
export function useCreateContact(groupId: string) {
  return useMutation({
    mutationFn: (fields: ContactFields) =>
      apiRequest<Contact>("POST", `/groups/${groupId}/contacts`, {
        fields,
        attributes: [],
        tags: [],
      }),
  });
}
