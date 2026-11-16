import i18n from "../i18n";

/** One invalid field of a 400 problem (specification §3, §5). */
export interface FieldError {
  field: string;
  code: string;
  params?: Record<string, unknown>;
}

interface ProblemBody {
  code?: string;
  params?: Record<string, unknown>;
  errors?: FieldError[];
}

/** An RFC 9457 problem of the API (ADR-06); the UI shows the catalog text of its code. */
export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly params: Record<string, unknown>;
  readonly errors: FieldError[];

  constructor(status: number, body: ProblemBody) {
    super(body.code ?? "internal");
    this.name = "ApiError";
    this.status = status;
    this.code = body.code ?? "internal";
    this.params = body.params ?? {};
    this.errors = body.errors ?? [];
  }
}

/** Reads the problem of a failed answer; an answer without one counts as `internal` (SB-07). */
export async function toApiError(response: Response): Promise<ApiError> {
  const type = response.headers.get("Content-Type") ?? "";
  const body = type.includes("json") ? ((await response.json()) as ProblemBody) : {};
  return new ApiError(response.status, body);
}

/** The catalog key of a problem code; unknown codes show the generic error text (UI-14). */
export function messageKey(code: string): string {
  const key = `errors.${code}`;
  return i18n.exists(key) ? key : "state.error";
}
