import { useQuery } from "@tanstack/react-query";
import { apiRequest } from "./apiClient";

/** The signed-in User (`Me` of the contract); the first call creates the User (R-001-04). */
export interface Me {
  id: string;
  email: string;
  displayName: string;
  language: "sl" | "en";
  emailVerified: boolean;
}

export const meQueryKey = ["me"] as const;

export function useMe() {
  return useQuery({ queryKey: meQueryKey, queryFn: () => apiRequest<Me>("GET", "/me") });
}
