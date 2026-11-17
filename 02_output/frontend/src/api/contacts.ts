import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { AttributeRow } from "../features/contacts/attributes";
import type { ContactFields } from "../features/contacts/contactFields";
import { apiPath, type ListQuery } from "../features/contacts/listQuery";
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
  state: "active" | "inRecycleBin" | "merged";
  fields: ContactFields;
  attributes: AttributeRow[];
  tags: Tag[];
  updatedAt: string;
  /** The Contacts merged into this one, oldest first (R-018-05). */
  mergedFrom: MergedFrom[];
  /** The Primary contact a merged Contact was merged into. */
  mergedInto: ContactRef | null;
}

/** One Merge into a Primary contact (`Contact.mergedFrom` of the contract, R-018-05). */
export interface MergedFrom {
  mergeId: string;
  contact: ContactRef;
  undone: boolean;
}

/** The Fields, Attributes and Tag names of a save (`ContactWrite` of the contract). */
export interface ContactWrite {
  fields: ContactFields;
  attributes: AttributeRow[];
  tags: string[];
}

/** Adds a Contact with its Attributes and Tags to the Group (R-006-01, R-008-01, R-009-01). */
export function useCreateContact(groupId: string) {
  return useMutation({
    mutationFn: ({ fields, attributes, tags }: ContactWrite) =>
      apiRequest<Contact>("POST", `/groups/${groupId}/contacts`, { fields, attributes, tags }),
  });
}

/**
 * Saves edited Fields, Attributes and Tags of the Contact as read (R-007-01, R-008-01, R-009-01).
 * The saved Contact replaces the one read, and lists read again.
 */
export function useUpdateContact(groupId: string, contactId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ read, fields, attributes, tags }: ContactWrite & { read: Contact }) =>
      apiRequest<Contact>("PUT", `/groups/${groupId}/contacts/${contactId}`, {
        fields,
        attributes: attributes.map(({ key, value }) => ({ key, value })),
        tags,
        versionNumber: read.versionNumber,
      }),
    onSuccess: async (saved) => {
      queryClient.setQueryData(["groups", groupId, "contacts", contactId], saved);
      await queryClient.invalidateQueries({ queryKey: ["groups", groupId, "contacts", "list"] });
    },
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

/** One Page of the Contact list (`ContactPage` of the contract). */
export interface ContactPage {
  items: ContactListItem[];
  total: number;
  page: number;
  pageSize: number;
}

/** One Page of the Group's Contact list for the query (R-014-01 … R-014-04). */
export function useContactList(groupId: string, query: ListQuery) {
  return useQuery({
    queryKey: ["groups", groupId, "contacts", "list", query],
    queryFn: () => apiRequest<ContactPage>("GET", apiPath(groupId, query)),
    placeholderData: keepPreviousData,
  });
}

/** One Contact of the Group with its Attributes and Tags (R-014-06). */
export function useContact(groupId: string, contactId: string) {
  return useQuery({
    queryKey: ["groups", groupId, "contacts", contactId],
    queryFn: () => apiRequest<Contact>("GET", `/groups/${groupId}/contacts/${contactId}`),
  });
}
