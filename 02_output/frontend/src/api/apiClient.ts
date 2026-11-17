import { toApiError } from "./problem";

/** Supplies the current Firebase ID token, or null when signed out (ADR-03). */
export type TokenSource = () => Promise<string | null>;

const API_BASE = "/api/v1";
let tokenSource: TokenSource = async () => null;

/** Sets where requests get their ID token (the auth provider, R-001-02). */
export function setTokenSource(source: TokenSource): void {
  tokenSource = source;
}

/** Sends a request to the Contacts API with the ID token; throws an ApiError unless 2xx (ADR-06). */
async function send(
  method: string,
  path: string,
  body: unknown,
  accept: string,
): Promise<Response> {
  const headers = new Headers({ Accept: accept });
  const token = await tokenSource();
  if (token) headers.set("Authorization", `Bearer ${token}`);
  if (body !== undefined) headers.set("Content-Type", "application/json");

  const response = await fetch(`${API_BASE}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (!response.ok) throw await toApiError(response);
  return response;
}

/** Calls the Contacts API; throws an ApiError for every answer that is not 2xx (ADR-06). */
export async function apiRequest<T>(method: string, path: string, body?: unknown): Promise<T> {
  const response = await send(method, path, body, "application/json");
  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}

/** A file the API answered. */
export interface DownloadedFile {
  blob: Blob;
  fileName: string;
}

/** The RFC 5987 UTF-8 file name of a `Content-Disposition` header (ADR-09). */
function fileNameOf(disposition: string): string {
  const encoded = /filename\*=UTF-8''([^;]+)/i.exec(disposition)?.[1];
  if (encoded) return decodeURIComponent(encoded);
  return /filename="([^"]*)"/i.exec(disposition)?.[1] ?? "";
}

/** Posts to the Contacts API and answers the file of the answer (US-016). */
export async function apiDownload(path: string, body: unknown): Promise<DownloadedFile> {
  const response = await send("POST", path, body, "*/*");
  return {
    blob: await response.blob(),
    fileName: fileNameOf(response.headers.get("Content-Disposition") ?? ""),
  };
}
