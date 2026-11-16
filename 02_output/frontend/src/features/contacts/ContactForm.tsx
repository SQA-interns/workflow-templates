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
} from "./contactFields";
import { ContactFieldInput, type FieldProps } from "./ContactFieldInput";
import { problemsOfError, problemText } from "./fieldMessages";

/** Hints under a Field (WF-08): the limits a User may run into. */
const HINTS = new Map<FieldName, { key: string; max?: number }>([
  ["firstName", { key: "contacts.hint.maxLength", max: 100 }],
  ["phone", { key: "contacts.hint.phone" }],
  ["note", { key: "contacts.hint.maxLength", max: 2000 }],
]);

interface ContactFormProps {
  title: string;
  backTo: string;
  busy: boolean;
  /** The error of the last save, if it failed. */
  error: unknown;
  onSave: (fields: ContactFields) => void;
  onCancel: () => void;
}

/** WF-08 Contact form: the Fields in form order with inline errors (UI-13). */
export function ContactForm({ title, backTo, busy, error, onSave, onCancel }: ContactFormProps) {
  const { t, i18n } = useTranslation();
  const language = i18n.language as Language;
  const id = useId();
  const [values, setValues] = useState(() => new Map(Object.entries(emptyFields())));
  const [problems, setProblems] = useState<FieldProblem[]>([]);
  const server = problemsOfError(error);
  const shown = problems.length > 0 || !error ? problems : server.fields;
  const nameProblem = shown.find((p) => p.field === "name");
  const general = nameProblem
    ? problemText(t, language, nameProblem)
    : error && server.general
      ? t(server.general)
      : null;

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
    };
  }

  return (
    <form className="flex flex-col gap-4" onSubmit={submit} noValidate>
      <Link to={backTo} className="inline-flex min-h-11 items-center gap-1 self-start underline">
        <ArrowLeft aria-hidden="true" className="size-4" />
        {t("contacts.back")}
      </Link>
      <h1 className="text-2xl font-semibold">{title}</h1>
      {general && (
        <p role="alert" className="border-l-4 border-danger pl-3 text-sm font-semibold text-danger">
          {general}
        </p>
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
      </div>
    </form>
  );
}
