import { X } from "lucide-react";
import { type KeyboardEvent, useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useParams } from "react-router";
import { Badge } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useTagSuggestions } from "../../api/tags";

/** Suggestions are asked this long after the last key, as the list search does (D-52 (f)). */
const SUGGEST_DELAY_MS = 300;

/** The text after the User stopped typing for {@link SUGGEST_DELAY_MS}. */
function useSettled(text: string): string {
  const [settled, setSettled] = useState(text);
  useEffect(() => {
    const timer = setTimeout(() => setSettled(text), SUGGEST_DELAY_MS);
    return () => clearTimeout(timer);
  }, [text]);
  return settled;
}

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

/** WF-08 Tags section: the Tags as chips and the input with suggestions (US-009). */
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
  const { groupId = "" } = useParams();
  const typed = useSettled(text.trim());
  const found = useTagSuggestions(groupId, typed).data ?? [];
  const lowerTags = new Set(tags.map((tag) => tag.toLowerCase()));
  const options =
    text.trim() === "" ? [] : found.filter((tag) => !lowerTags.has(tag.name.toLowerCase()));
  const names = found.map((tag) => tag.name);
  const [active, setActive] = useState(-1);
  const listId = `${inputId}-suggestions`;
  const hintId = `${inputId}-hint`;
  const errorId = `${inputId}-error`;
  const open = options.length > 0;

  function add(name: string) {
    onAdd(name, names);
    setActive(-1);
  }

  function onKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === "ArrowDown" && open) {
      event.preventDefault();
      setActive((index) => (index + 1) % options.length);
    } else if (event.key === "ArrowUp" && open) {
      event.preventDefault();
      setActive((index) => (index <= 0 ? options.length - 1 : index - 1));
    } else if (event.key === "Escape") {
      setActive(-1);
    } else if (event.key === "Enter") {
      event.preventDefault();
      add((active >= 0 ? options.at(active)?.name : undefined) ?? text);
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
      <div className="relative flex flex-col gap-1">
        <Label htmlFor={inputId}>{t("contacts.tags.add")}</Label>
        <Input
          id={inputId}
          role="combobox"
          value={text}
          autoComplete="off"
          aria-autocomplete="list"
          aria-expanded={open}
          aria-controls={listId}
          aria-activedescendant={open && active >= 0 ? `${listId}-${active}` : undefined}
          aria-invalid={error ? true : undefined}
          aria-errormessage={error ? errorId : undefined}
          aria-describedby={error ? `${errorId} ${hintId}` : hintId}
          onChange={(e) => {
            onTextChange(e.target.value);
            setActive(-1);
          }}
          onKeyDown={onKeyDown}
        />
        <ul
          id={listId}
          role="listbox"
          aria-label={t("contacts.tags.suggestions")}
          hidden={!open}
          className="flex flex-col rounded-md border border-border bg-surface py-1 shadow-sm"
        >
          {options.map((tag, index) => (
            // The keyboard stays on the combobox input (ARIA combobox pattern): Arrow keys and Enter
            // choose an option there, so the options themselves take only clicks.
            // eslint-disable-next-line jsx-a11y/click-events-have-key-events
            <li
              key={tag.id}
              id={`${listId}-${index}`}
              role="option"
              aria-selected={index === active}
              className="flex min-h-11 cursor-pointer items-center px-3 aria-selected:bg-muted"
              onMouseDown={(e) => e.preventDefault()}
              onClick={() => add(tag.name)}
            >
              {tag.name}
            </li>
          ))}
        </ul>
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
