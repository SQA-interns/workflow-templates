import { type ReactNode, useMemo } from "react";
import { useTranslation } from "react-i18next";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { NativeSelect } from "@/components/ui/native-select";
import { Textarea } from "@/components/ui/textarea";
import type { Language } from "../../i18n";
import { countryOptions, type FieldName } from "./contactFields";

export interface FieldProps {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  hint?: string;
  error?: string;
  /** Called when the box loses focus. */
  onBlur?: () => void;
  /** A warning that does not stop saving, announced politely (R-006-06); "" for none. */
  notice?: string;
}

/** One Field: a text box, the note area or the country list. */
export function ContactFieldInput({
  name,
  props,
  language,
}: {
  name: FieldName;
  props: FieldProps;
  language: Language;
}) {
  const control = {
    id: props.id,
    value: props.value,
    "aria-invalid": props.error ? true : undefined,
    "aria-errormessage": props.error ? `${props.id}-error` : undefined,
    "aria-describedby":
      [
        props.hint && `${props.id}-hint`,
        props.error && `${props.id}-error`,
        props.notice && `${props.id}-notice`,
      ]
        .filter(Boolean)
        .join(" ") || undefined,
  };
  let input: ReactNode;
  if (name === "note") {
    input = <Textarea {...control} onChange={(e) => props.onChange(e.target.value)} />;
  } else if (name === "country") {
    input = <CountrySelect control={control} language={language} onChange={props.onChange} />;
  } else {
    input = (
      <Input
        {...control}
        type={name === "email" ? "email" : name === "phone" ? "tel" : "text"}
        autoComplete="off"
        onChange={(e) => props.onChange(e.target.value)}
        onBlur={props.onBlur}
      />
    );
  }
  return (
    <div className={name === "note" ? "flex flex-col gap-1 md:col-span-2" : "flex flex-col gap-1"}>
      <Label htmlFor={props.id}>{props.label}</Label>
      {input}
      {props.hint && (
        <p id={`${props.id}-hint`} className="text-sm text-text-muted">
          {props.hint}
        </p>
      )}
      {props.notice !== undefined && (
        <div role="status">
          {props.notice && (
            <p
              id={`${props.id}-notice`}
              className="rounded-md border border-dashed border-border bg-surface p-3 text-sm"
            >
              {props.notice}
            </p>
          )}
        </div>
      )}
      {props.error && (
        <p id={`${props.id}-error`} className="text-sm font-semibold text-danger">
          {props.error}
        </p>
      )}
    </div>
  );
}

/** The country as an ISO code, chosen by name (R-006-05). */
function CountrySelect({
  control,
  language,
  onChange,
}: {
  control: { id: string; value: string };
  language: Language;
  onChange: (value: string) => void;
}) {
  const { t } = useTranslation();
  const countries = useMemo(() => countryOptions(language), [language]);
  return (
    <NativeSelect {...control} onChange={(e) => onChange(e.target.value)}>
      <option value="">{t("contacts.countryPlaceholder")}</option>
      {countries.map((country) => (
        <option key={country.code} value={country.code}>
          {country.name}
        </option>
      ))}
    </NativeSelect>
  );
}
