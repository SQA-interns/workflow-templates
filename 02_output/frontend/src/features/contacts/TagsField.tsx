import { X } from "lucide-react";
import type { KeyboardEvent } from "react";
import { useTranslation } from "react-i18next";
import { Badge } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

interface TagsFieldProps {
  /** The id of the Tag input, so that the form can focus it. */
  inputId: string;
  tags: string[];
  /** The typed name not yet added. */
  text: string;
  onTextChange: (text: string) => void;
  /** Adds a typed or chosen name; a suggested Tag that equals it gives the spelling (R-009-03). */
  onAdd: (name: string, suggested: string[]) => void;
  onRemove: (name: string) => void;
  /** The text of the Tags' problem, if any (R-009-05). */
  error: string | undefined;
}

/** WF-08 Tags section: the Tags as chips and the input to add one (US-009). */
export function TagsField({
  inputId,
  tags,
  text,
  onTextChange,
  onAdd,
  onRemove,
  error,
}: TagsFieldProps) {
  const { t } = useTranslation();
  const hintId = `${inputId}-hint`;
  const errorId = `${inputId}-error`;

  function onKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === "Enter") {
      event.preventDefault();
      onAdd(text, []);
    }
  }

  return (
    <Card className="flex flex-col gap-4">
      <h2 className="text-xl font-semibold">{t("contacts.tags.title")}</h2>
      {tags.length > 0 && (
        <ul className="flex flex-wrap gap-2">
          {tags.map((tag) => (
            <li key={tag.toLowerCase()}>
              <Badge className="gap-1 py-0 pr-0">
                {tag}
                <button
                  type="button"
                  className="inline-flex min-h-11 min-w-11 items-center justify-center rounded-full"
                  aria-label={t("contacts.tags.remove", { name: tag })}
                  onClick={() => onRemove(tag)}
                >
                  <X aria-hidden="true" className="size-4" />
                </button>
              </Badge>
            </li>
          ))}
        </ul>
      )}
      <div className="flex flex-col gap-1">
        <Label htmlFor={inputId}>{t("contacts.tags.add")}</Label>
        <Input
          id={inputId}
          value={text}
          autoComplete="off"
          aria-invalid={error ? true : undefined}
          aria-errormessage={error ? errorId : undefined}
          aria-describedby={error ? `${errorId} ${hintId}` : hintId}
          onChange={(e) => onTextChange(e.target.value)}
          onKeyDown={onKeyDown}
        />
        <p id={hintId} className="text-sm text-text-muted">
          {t("contacts.tags.hint")}
        </p>
        {error && (
          <p
            id={errorId}
            className="border-l-4 border-danger pl-3 text-sm font-semibold text-danger"
          >
            {error}
          </p>
        )}
      </div>
    </Card>
  );
}
