import { useTranslation } from "react-i18next";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import type { AttributeRow } from "./attributes";

/** An Attribute row of the form; `id` keeps React's identity while rows come and go. */
export interface EditableAttribute extends AttributeRow {
  id: number;
}

let nextId = 0;

/** Rows for the form from saved Attributes, or one empty row. */
export function editable(row: AttributeRow = { key: "", value: "" }): EditableAttribute {
  nextId += 1;
  return { ...row, id: nextId };
}

/** The id of the control of a problem: `attributes[1].key` → `<prefix>-attributes-1-key`. */
export function attributeControlId(prefix: string, field: string): string {
  return `${prefix}-${field.replace(/\]?\.|\[/g, "-")}`;
}

interface AttributesFieldProps {
  /** The form's id prefix; the controls' ids follow {@link attributeControlId}. */
  idPrefix: string;
  rows: EditableAttribute[];
  onChange: (rows: EditableAttribute[]) => void;
  /** The text of the problem of `attributes[i].key`, `attributes[i].value` or `attributes`. */
  errorOf: (field: string) => string | undefined;
}

/** WF-08 Attributes section: one row of Key, Value and Remove per Attribute (US-008). */
export function AttributesField({ idPrefix, rows, onChange, errorOf }: AttributesFieldProps) {
  const { t } = useTranslation();
  const listError = errorOf("attributes");
  const listErrorId = attributeControlId(idPrefix, "attributes-error");

  function inputOf(index: number, part: keyof AttributeRow, value: string) {
    const field = `attributes[${index}].${part}`;
    return {
      id: attributeControlId(idPrefix, field),
      label: t(`contacts.attributes.${part}`),
      value,
      wide: part === "value",
      error: errorOf(field),
      onChange: (text: string) =>
        onChange(rows.map((row, i) => (i === index ? { ...row, [part]: text } : row))),
    };
  }

  return (
    <Card className="flex flex-col gap-4">
      <h2 className="text-xl font-semibold">{t("contacts.attributes.title")}</h2>
      {rows.map((row, index) => (
        <div key={row.id} className="flex flex-wrap items-start gap-2">
          <AttributeInput {...inputOf(index, "key", row.key)} />
          <AttributeInput {...inputOf(index, "value", row.value)} />
          <Button
            type="button"
            variant="ghost"
            className="mt-6"
            onClick={() => onChange(rows.filter((_, i) => i !== index))}
          >
            {t("contacts.attributes.remove")}
          </Button>
        </div>
      ))}
      {listError && (
        <p id={listErrorId} className="text-sm font-semibold text-danger">
          {listError}
        </p>
      )}
      <Button
        id={attributeControlId(idPrefix, "attributes")}
        type="button"
        variant="secondary"
        className="self-start"
        aria-describedby={listError ? listErrorId : undefined}
        onClick={() => onChange([...rows, editable()])}
      >
        {t("contacts.attributes.add")}
      </Button>
    </Card>
  );
}

/** The key or the value of one Attribute with its inline error (UI-13). */
function AttributeInput(props: {
  id: string;
  label: string;
  value: string;
  wide: boolean;
  error: string | undefined;
  onChange: (text: string) => void;
}) {
  const errorId = `${props.id}-error`;
  return (
    <div
      className={
        props.wide
          ? "flex min-w-0 grow-2 basis-48 flex-col gap-1"
          : "flex min-w-0 grow basis-36 flex-col gap-1"
      }
    >
      <Label htmlFor={props.id}>{props.label}</Label>
      <Input
        id={props.id}
        value={props.value}
        autoComplete="off"
        aria-invalid={props.error ? true : undefined}
        aria-errormessage={props.error ? errorId : undefined}
        aria-describedby={props.error ? errorId : undefined}
        onChange={(e) => props.onChange(e.target.value)}
      />
      {props.error && (
        <p id={errorId} className="text-sm font-semibold text-danger">
          {props.error}
        </p>
      )}
    </div>
  );
}
