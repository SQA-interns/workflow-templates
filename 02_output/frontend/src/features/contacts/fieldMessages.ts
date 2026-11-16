import type { TFunction } from "i18next";
import { ApiError, messageKey } from "../../api/problem";
import type { Language } from "../../i18n";
import { formatNumber } from "../../i18n/format";
import { FIELD_NAMES, type FieldName, type FieldProblem } from "./contactFields";

/** The text of a Field problem; `field.tooLong` names the Field and its maximum (R-006-03). */
export function problemText(t: TFunction, language: Language, problem: FieldProblem): string {
  if (problem.code === "field.tooLong" && problem.params) {
    return t("errors.field.tooLong", {
      field: t(`contacts.field.${problem.field}`),
      max: formatNumber(problem.params.max, language),
    });
  }
  return t(messageKey(problem.code));
}

function isFieldProblem(problem: { field: string }): problem is FieldProblem {
  return problem.field === "name" || (FIELD_NAMES as readonly string[]).includes(problem.field);
}

/** The problems of a failed save: per Field, or one general text and its values (UI-13, UI-14). */
export function problemsOfError(error: unknown): {
  fields: FieldProblem[];
  general: string | null;
  params: Record<string, unknown>;
} {
  if (!(error instanceof ApiError)) return { fields: [], general: "state.error", params: {} };
  const fields = error.errors
    .filter(isFieldProblem)
    .map((e) => ({ field: e.field, code: e.code, params: maxOf(e.params) }));
  return {
    fields,
    general: fields.length > 0 ? null : messageKey(error.code),
    params: error.params,
  };
}

/** Whether the error is the API problem with this code. */
export function isProblem(error: unknown, code: string): boolean {
  return error instanceof ApiError && error.code === code;
}

function maxOf(params: Record<string, unknown> | undefined): { max: number } | undefined {
  return typeof params?.max === "number" ? { max: params.max } : undefined;
}

export type { FieldName };
