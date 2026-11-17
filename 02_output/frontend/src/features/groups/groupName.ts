import { ApiError, messageKey } from "../../api/problem";

/** Longest Group name after trimming (R-002-02, specification §3). */
const NAME_MAX = 100;
const NAME_LENGTH = "group.nameLength";

/** The catalog key of the name rule (R-002-02, R-003-01). */
export const NAME_LENGTH_KEY = `errors.${NAME_LENGTH}`;

/** R-002-02: 1 to 100 characters after trimming, counted as characters. */
export function nameIsValid(name: string): boolean {
  const length = [...name].length;
  return length > 0 && length <= NAME_MAX;
}

/** The catalog key of a failed save of a name: the name rule, or the problem's own text (UI-13). */
export function nameErrorKeyOf(error: unknown): string {
  if (error instanceof ApiError && error.errors.some((e) => e.code === NAME_LENGTH)) {
    return NAME_LENGTH_KEY;
  }
  return error instanceof ApiError ? messageKey(error.code) : "state.error";
}
