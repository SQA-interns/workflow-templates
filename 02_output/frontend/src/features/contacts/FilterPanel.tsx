import { X } from "lucide-react";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { NativeSelect } from "@/components/ui/native-select";
import type { Tag } from "../../api/contacts";
import { countryOptions } from "./contactFields";
import type { Filters } from "./listQuery";

interface FilterPanelProps {
  id: string;
  filters: Filters;
  tags: Tag[];
  onApply: (filters: Filters) => void;
  onClear: () => void;
}

/** The open Filters panel of WF-06: edits a copy of the Active filters until Apply (US-015). */
export function FilterPanel({ id, filters, tags, onApply, onClear }: FilterPanelProps) {
  const { t, i18n } = useTranslation();
  const [draft, setDraft] = useState(filters);
  const set = (next: Partial<Filters>) => setDraft({ ...draft, ...next });
  const text = (name: string, label: string, value: string, onChange: (value: string) => void) => (
    <div className="flex flex-col gap-1">
      <Label htmlFor={`${id}-${name}`}>{label}</Label>
      <Input id={`${id}-${name}`} value={value} onChange={(e) => onChange(e.target.value)} />
    </div>
  );
  return (
    <section
      id={id}
      aria-label={t("contacts.filters.open")}
      className="flex flex-col gap-4 rounded-lg border border-border bg-surface p-4 md:max-w-xl"
    >
      <TagPicker
        id={`${id}-has`}
        label={t("contacts.filters.hasTags")}
        hint={t("contacts.filters.hasTagsHint")}
        tags={tags}
        chosen={draft.hasTagIds}
        onChange={(hasTagIds) => set({ hasTagIds })}
      />
      <TagPicker
        id={`${id}-without`}
        label={t("contacts.filters.withoutTags")}
        hint={t("contacts.filters.withoutTagsHint")}
        tags={tags}
        chosen={draft.withoutTagIds}
        onChange={(withoutTagIds) => set({ withoutTagIds })}
      />
      <div className="grid gap-4 sm:grid-cols-2">
        {text("key", t("contacts.filters.attributeKey"), draft.attributeKey, (attributeKey) =>
          set({ attributeKey }),
        )}
        {text(
          "value",
          t("contacts.filters.attributeValue"),
          draft.attributeValue,
          (attributeValue) => set({ attributeValue }),
        )}
      </div>
      {text("company", t("contacts.filters.company"), draft.company, (company) => set({ company }))}
      <div className="flex flex-col gap-1">
        <Label htmlFor={`${id}-country`}>{t("contacts.filters.country")}</Label>
        <NativeSelect
          id={`${id}-country`}
          value={draft.country}
          onChange={(e) => set({ country: e.target.value })}
        >
          <option value="">{t("contacts.filters.select")}</option>
          {countryOptions(i18n.language).map(({ code, name }) => (
            <option key={code} value={code}>
              {name}
            </option>
          ))}
        </NativeSelect>
      </div>
      <div className="flex flex-wrap gap-2">
        <Button onClick={() => onApply(draft)}>{t("contacts.filters.apply")}</Button>
        <Button variant="ghost" onClick={onClear}>
          {t("contacts.filters.clear")}
        </Button>
      </div>
    </section>
  );
}

interface TagPickerProps {
  id: string;
  label: string;
  hint: string;
  tags: Tag[];
  chosen: string[];
  onChange: (chosen: string[]) => void;
}

/** "Select …" adds a Tag of the Group; each chosen Tag is a chip that can be taken out again. */
function TagPicker({ id, label, hint, tags, chosen, onChange }: TagPickerProps) {
  const { t } = useTranslation();
  const name = (tagId: string) => tags.find((tag) => tag.id === tagId)?.name ?? "…";
  return (
    <div className="flex flex-col gap-1">
      <Label htmlFor={id}>{label}</Label>
      <NativeSelect
        id={id}
        value=""
        aria-describedby={`${id}-hint`}
        onChange={(e) => onChange([...chosen, e.target.value])}
      >
        <option value="">{t("contacts.filters.select")}</option>
        {tags
          .filter((tag) => !chosen.includes(tag.id))
          .map((tag) => (
            <option key={tag.id} value={tag.id}>
              {tag.name}
            </option>
          ))}
      </NativeSelect>
      <span id={`${id}-hint`} className="text-sm text-text-muted">
        {hint}
      </span>
      {chosen.length > 0 && (
        <ul className="flex flex-wrap gap-2">
          {chosen.map((tagId) => (
            <li key={tagId}>
              <Badge className="gap-1">
                {name(tagId)}
                <button
                  type="button"
                  className="inline-flex min-h-11 min-w-11 items-center justify-center rounded-full"
                  aria-label={t("contacts.filters.removeTag", { name: name(tagId) })}
                  onClick={() => onChange(chosen.filter((other) => other !== tagId))}
                >
                  <X aria-hidden="true" className="size-4" />
                </button>
              </Badge>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
