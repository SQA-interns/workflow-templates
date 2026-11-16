import { useMutation, useQuery } from "@tanstack/react-query";
import type { ContactFields } from "../features/contacts/contactFields";
import { apiRequest } from "./apiClient";

/** A Tag (`Tag` of the contract). */
export interface Tag {
  id: string;
  name: string;
}

/** One row of the Contact list (`ContactListItem` of the contract, R-014-01). */
export interface ContactListItem {
  id: string;
  firstName: string | null;
  lastName: string | null;
  company: string | null;
  email: string | null;
  phone: string | null;
  tags: Tag[];
  updatedAt: string;
}

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

/** A link to a Contact (`ContactRef` of the contract). */
export interface ContactRef {
  id: string;
  name: string;
}

/** The active Contacts of the Group with this e-mail (R-006-06); nothing is asked while null. */
export function useEmailMatches(groupId: string, email: string | null) {
  return useQuery({
    queryKey: ["groups", groupId, "contacts", "email-matches", email],
    queryFn: () =>
      apiRequest<ContactRef[]>(
        "GET",
        `/groups/${groupId}/contacts/email-matches?email=${encodeURIComponent(email ?? "")}`,
      ),
    enabled: email !== null,
  });
}
