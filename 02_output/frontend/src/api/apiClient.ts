import { toApiError } from "./problem";

/** Supplies the current Firebase ID token, or null when signed out (ADR-03). */
export type TokenSource = () => Promise<string | null>;

const API_BASE = "/api/v1";
let tokenSource: TokenSource = async () => null;

/** Sets where requests get their ID token (the auth provider, R-001-02). */
export function setTokenSource(source: TokenSource): void {
  tokenSource = source;
}

/** Calls the Contacts API; throws an ApiError for every answer that is not 2xx (ADR-06). */
export async function apiRequest<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers = new Headers({ Accept: "application/json" });
  const token = await tokenSource();
  if (token) headers.set("Authorization", `Bearer ${token}`);
  if (body !== undefined) headers.set("Content-Type", "application/json");

  const response = await fetch(`${API_BASE}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (!response.ok) throw await toApiError(response);
  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}
