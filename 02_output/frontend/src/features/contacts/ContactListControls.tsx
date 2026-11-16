import { useId } from "react";
import { useTranslation } from "react-i18next";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { NativeSelect } from "@/components/ui/native-select";
import { type Dir, type Sort, SORTS } from "./listQuery";

interface ContactListControlsProps {
  search: string;
  sort: Sort;
  dir: Dir;
  onSearch: (search: string) => void;
  onSort: (sort: Sort, dir: Dir) => void;
}

/** Search, Sort by and Order of WF-06 (R-014-03, R-014-04). */
export function ContactListControls({
  search,
  sort,
  dir,
  onSearch,
  onSort,
}: ContactListControlsProps) {
  const { t } = useTranslation();
  const id = useId();
  return (
    <section
      aria-label={t("contacts.list.controls")}
      className="grid gap-4 rounded-lg border border-border bg-surface p-4 md:grid-cols-[2fr_1fr_1fr]"
    >
      <div className="flex flex-col gap-1">
        <Label htmlFor={`${id}-search`}>{t("contacts.list.search")}</Label>
        <Input
          id={`${id}-search`}
          type="search"
          placeholder={t("contacts.list.searchHint")}
          value={search}
          onChange={(e) => onSearch(e.target.value)}
        />
      </div>
      <div className="grid grid-cols-2 gap-4 md:contents">
        <div className="flex flex-col gap-1">
          <Label htmlFor={`${id}-sort`}>{t("contacts.list.sortBy")}</Label>
          <NativeSelect
            id={`${id}-sort`}
            value={sort}
            onChange={(e) => onSort(e.target.value as Sort, dir)}
          >
            {SORTS.map((key) => (
              <option key={key} value={key}>
                {t(`contacts.list.sort.${key}`)}
              </option>
            ))}
          </NativeSelect>
        </div>
        <div className="flex flex-col gap-1">
          <Label htmlFor={`${id}-dir`}>{t("contacts.list.order")}</Label>
          <NativeSelect
            id={`${id}-dir`}
            value={dir}
            onChange={(e) => onSort(sort, e.target.value as Dir)}
          >
            <option value="asc">{t("contacts.list.ascending")}</option>
            <option value="desc">{t("contacts.list.descending")}</option>
          </NativeSelect>
        </div>
      </div>
    </section>
  );
}
