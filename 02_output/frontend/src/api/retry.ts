import { ApiError } from "./problem";

/** How often a failed read is asked again; TanStack Query's default. */
const MAX_RETRIES = 3;

/**
 * Whether to ask again after a failure: a client error (4xx: not found, invalid, forbidden) is
 * the final answer, so the screen shows it at once; a server or network failure is retried.
 */
export function retryUnlessClientError(failureCount: number, error: Error): boolean {
  if (error instanceof ApiError && error.status >= 400 && error.status < 500) return false;
  return failureCount < MAX_RETRIES;
}
