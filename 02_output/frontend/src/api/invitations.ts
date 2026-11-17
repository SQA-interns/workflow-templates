import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiRequest } from "./apiClient";

/** The Roles an Invitation can grant; never Owner (R-004-01). */
export type InvitableRole = "EDITOR" | "VIEWER";

/** An open Invitation as the Owner sees it (`Invitation` of the contract, R-004-08). */
export interface Invitation {
  id: string;
  email: string;
  role: InvitableRole;
  expiresAt: string;
}

/** A new Invitation with its link, shown only once (`InvitationCreated`, R-004-01). */
export interface InvitationCreated {
  invitation: Invitation;
  link: string;
}

const openInvitationsKey = (groupId: string) => ["groups", groupId, "invitations"] as const;

/** The Group's open Invitations; Owner only (R-004-08). */
export function useOpenInvitations(groupId: string) {
  return useQuery({
    queryKey: openInvitationsKey(groupId),
    queryFn: () => apiRequest<Invitation[]>("GET", `/groups/${groupId}/invitations`),
  });
}

/** Creates an Invitation (R-004-01); the open Invitations are read again (R-004-03). */
export function useCreateInvitation(groupId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: { email: string; role: InvitableRole }) =>
      apiRequest<InvitationCreated>("POST", `/groups/${groupId}/invitations`, request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: openInvitationsKey(groupId) }),
  });
}

/** Revokes an open Invitation (R-004-09); the open Invitations are read again. */
export function useRevokeInvitation(groupId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (invitationId: string) =>
      apiRequest<undefined>("DELETE", `/groups/${groupId}/invitations/${invitationId}`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: openInvitationsKey(groupId) }),
  });
}
