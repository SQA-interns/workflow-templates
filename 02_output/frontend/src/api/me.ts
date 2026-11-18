import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { Language } from "../i18n";
import { apiRequest } from "./apiClient";

/** The signed-in User (`Me` of the contract); the first call creates the User (R-001-04). */
export interface Me {
  id: string;
  email: string;
  displayName: string;
  language: Language;
  emailVerified: boolean;
}

export const meQueryKey = ["me"] as const;

export function useMe() {
  return useQuery({ queryKey: meQueryKey, queryFn: () => apiRequest<Me>("GET", "/me") });
}

/** Chooses the language; the server keeps it for later sign-ins (R-020-02). */
export function useChangeLanguage() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (language: Language) => apiRequest<Me>("PATCH", "/me", { language }),
    onSuccess: (me) => queryClient.setQueryData(meQueryKey, me),
  });
}

/**
 * Deletes the signed-in User's account after the typed e-mail (R-021-01); 409
 * `account.ownsGroups` for an Owner (R-021-02).
 */
export function useDeleteAccount() {
  return useMutation({
    mutationFn: (confirmEmail: string) => apiRequest<unknown>("DELETE", "/me", { confirmEmail }),
  });
}
