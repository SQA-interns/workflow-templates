import { ArrowLeft } from "lucide-react";
import { type FormEvent, useId, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import type { Language } from "../../i18n";
import { formatNumber } from "../../i18n/format";
import {
  checkFields,
  type ContactFields,
  emptyFields,
  FIELD_NAMES,
  type FieldName,
  type FieldProblem,
  type FieldValues,
  valuesOf,
} from "./contactFields";
import { ContactFieldInput, type FieldProps } from "./ContactFieldInput";
import { isProblem, problemsOfError, problemText } from "./fieldMessages";

/** Hints under a Field (WF-08): the limits a User may run into. */
const HINTS = new Map<FieldName, { key: string; max?: number }>([
  ["firstName", { key: "contacts.hint.maxLength", max: 100 }],
  ["phone", { key: "contacts.hint.phone" }],
  ["note", { key: "contacts.hint.maxLength", max: 2000 }],
]);

interface ContactFormProps {
  title: string;
  /** The saved Fields to edit; an empty form without them. */
  initial?: ContactFields;
  backTo: string;
  busy: boolean;
  /** The error of the last save, if it failed. */
  error: unknown;
  onSave: (fields: ContactFields) => void;
  onCancel: () => void;
  /** Called with the e-mail when the User leaves its box (R-006-06); no lookup without it. */
  onEmailLeave?: (email: string) => void;
  /** The warning under the e-mail, "" for none (R-006-06). */
  emailNotice?: string;
  /** Reads the current Version again after a conflict (BR-06); no Reload without it. */
  onReload?: () => void;
}

/** WF-08 Contact form: the Fields in form order with inline errors (UI-13). */
export function ContactForm({
  title,
  initial,
  backTo,
  busy,
  error,
  onSave,
  onCancel,
  onEmailLeave,
  emailNotice,
  onReload,
}: ContactFormProps) {
  const { t, i18n } = useTranslation();
  const language = i18n.language as Language;
  const id = useId();
  const [values, setValues] = useState(
    () => new Map(Object.entries(initial ? valuesOf(initial) : emptyFields())),
  );
  const [problems, setProblems] = useState<FieldProblem[]>([]);
  const server = problemsOfError(error);
  const shown = problems.length > 0 || !error ? problems : server.fields;
  const nameProblem = shown.find((p) => p.field === "name");
  const general = nameProblem
    ? problemText(t, language, nameProblem)
    : error && server.general
      ? t(server.general, server.params)
      : null;
  /** R-007-03: "No changes to save." stands beside Save (WF-08). */
  const atSave = !nameProblem && isProblem(error, "contact.noChanges");
  const reload = onReload && !nameProblem && isProblem(error, "version.conflict");

  function submit(event: FormEvent) {
    event.preventDefault();
    const checked = checkFields(Object.fromEntries(values) as FieldValues);
    setProblems(checked.problems);
    const [first] = checked.problems;
    if (first) {
      const focus = first.field === "name" ? "firstName" : first.field;
      document.getElementById(`${id}-${focus}`)?.focus();
      return;
    }
    onSave(checked.trimmed);
  }

  function field(name: FieldName): FieldProps {
    const problem = shown.find((p) => p.field === name);
    const hint = HINTS.get(name);
    return {
      id: `${id}-${name}`,
      label: t(`contacts.field.${name}`),
      value: values.get(name) ?? "",
      onChange: (value) => setValues((current) => new Map(current).set(name, value)),
      hint: hint && t(hint.key, { max: hint.max && formatNumber(hint.max, language) }),
      error: problem && problemText(t, language, problem),
      ...(name === "email" &&
        onEmailLeave && {
          onBlur: () => onEmailLeave(values.get("email") ?? ""),
          notice: emailNotice,
        }),
    };
  }

  return (
    <form className="flex flex-col gap-4" onSubmit={submit} noValidate>
      <Link to={backTo} className="inline-flex min-h-11 items-center gap-1 self-start underline">
        <ArrowLeft aria-hidden="true" className="size-4" />
        {t("contacts.back")}
      </Link>
      <h1 className="text-2xl font-semibold">{title}</h1>
      {general && !atSave && (
        <div
          role="alert"
          className="flex flex-wrap items-center gap-2 border-l-4 border-danger pl-3"
        >
          <p className="text-sm font-semibold text-danger">{general}</p>
          {reload && (
            <Button type="button" variant="secondary" onClick={onReload}>
              {t("contacts.reload")}
            </Button>
          )}
        </div>
      )}
      <Card className="flex flex-col gap-4">
        <h2 className="text-xl font-semibold">{t("contacts.fields")}</h2>
        <div className="grid gap-4 md:grid-cols-2">
          {FIELD_NAMES.map((name) => (
            <ContactFieldInput key={name} name={name} props={field(name)} language={language} />
          ))}
        </div>
        <p className="text-sm text-text-muted">{t("contacts.hint.name")}</p>
      </Card>
      <div className="flex flex-wrap items-center gap-2">
        <Button type="submit" disabled={busy} aria-busy={busy}>
          {t("contacts.save")}
        </Button>
        <Button type="button" variant="ghost" onClick={onCancel}>
          {t("contacts.cancel")}
        </Button>
        {atSave && (
          <p role="status" className="text-sm text-text-muted">
            {general}
          </p>
        )}
      </div>
    </form>
  );
}
