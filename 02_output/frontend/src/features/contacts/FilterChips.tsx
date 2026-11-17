import { X } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router";
import { Badge } from "@/components/ui/badge";
import type { Tag } from "../../api/contacts";
import { countryName } from "./contactFields";
import { type Filters, NO_FILTERS } from "./listQuery";

interface FilterChipsProps {
  filters: Filters;
  tags: Tag[];
  clearTo: string;
  onChange: (filters: Filters) => void;
}

interface Chip {
  name: string;
  value: string;
  without: Partial<Filters>;
}

/** The Active filters as removable chips and "Clear filters" (R-015-06). */
export function FilterChips({ filters, tags, clearTo, onChange }: FilterChipsProps) {
  const { t } = useTranslation();
  const chips = useChips(filters, tags);
  if (chips.length === 0) return null;
  return (
    <>
      <ul className="contents">
        {chips.map((chip) => (
          <li key={chip.name}>
            <Badge className="gap-1">
              {t("contacts.filters.chip", { name: chip.name, value: chip.value })}
              <button
                type="button"
                className="inline-flex min-h-11 min-w-11 items-center justify-center rounded-full"
                aria-label={t("contacts.filters.remove", { name: chip.name })}
                onClick={() => onChange({ ...filters, ...chip.without })}
              >
                <X aria-hidden="true" className="size-4" />
              </button>
            </Badge>
          </li>
        ))}
      </ul>
      <Link to={clearTo} className="underline">
        {t("contacts.filters.clear")}
      </Link>
    </>
  );
}

/** One chip per set filter, in the order of the panel. */
function useChips(filters: Filters, tags: Tag[]): Chip[] {
  const { t, i18n } = useTranslation();
  const names = (ids: string[]) =>
    ids.map((id) => tags.find((tag) => tag.id === id)?.name ?? "…").join(", ");
  const { attributeKey: key, attributeValue: value } = filters;
  const all: (Chip | false)[] = [
    filters.hasTagIds.length > 0 && {
      name: t("contacts.filters.hasTags"),
      value: names(filters.hasTagIds),
      without: { hasTagIds: NO_FILTERS.hasTagIds },
    },
    filters.withoutTagIds.length > 0 && {
      name: t("contacts.filters.withoutTags"),
      value: names(filters.withoutTagIds),
      without: { withoutTagIds: NO_FILTERS.withoutTagIds },
    },
    key !== "" && {
      name: t("contacts.filters.attribute"),
      value: value === "" ? key : t("contacts.filters.attributeChip", { key, value }),
      without: { attributeKey: "", attributeValue: "" },
    },
    filters.company !== "" && {
      name: t("contacts.filters.company"),
      value: filters.company,
      without: { company: "" },
    },
    filters.country !== "" && {
      name: t("contacts.filters.country"),
      value: countryName(filters.country, i18n.language),
      without: { country: "" },
    },
  ];
  return all.filter((chip): chip is Chip => chip !== false);
}
