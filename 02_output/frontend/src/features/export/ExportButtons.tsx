import { useTranslation } from "react-i18next";
import { Button } from "@/components/ui/button";
import { MAX_EXPORT, useExport } from "../../api/exports";
import { ApiError, messageKey } from "../../api/problem";
import type { Language } from "../../i18n";
import { formatNumber } from "../../i18n/format";
import type { ListQuery } from "../contacts/listQuery";

interface ExportButtonsProps {
  groupId: string;
  /** The list's search, sort and filters: the Export scope (R-016-01). */
  query: ListQuery;
  /** The number of Contacts of the filtered list (R-015-07). */
  total: number;
  /** The Checked contacts (R-016-02). */
  checked: string[];
}

/** WF-06 "Export" and "Export checked" with the count of Checked contacts (US-016). */
export function ExportButtons({ groupId, query, total, checked }: ExportButtonsProps) {
  const { t, i18n } = useTranslation();
  const exporting = useExport(groupId, query);
  const count = checked.length;
  const formatted = formatNumber(count, i18n.language as Language);
  const tooMany = total > MAX_EXPORT;
  const error = exporting.error instanceof ApiError ? exporting.error.code : "internal";

  return (
    <div className="flex flex-col gap-2">
      <div className="flex flex-wrap items-center gap-2">
        <Button
          variant="secondary"
          disabled={total === 0 || tooMany || exporting.isPending}
          onClick={() => exporting.mutate(undefined)}
        >
          {t("contacts.export.all")}
        </Button>
        <Button
          variant="secondary"
          disabled={count === 0 || exporting.isPending}
          onClick={() => exporting.mutate(checked)}
        >
          {count === 0
            ? t("contacts.export.checked")
            : t("contacts.export.checkedCount", { formatted })}
        </Button>
        {count > 0 && (
          <span className="text-text-muted">{t("contacts.export.count", { formatted })}</span>
        )}
      </div>
      {total === 0 && <p>{t("errors.export.empty")}</p>}
      {tooMany && <p className="text-danger">{t("errors.export.tooMany")}</p>}
      {exporting.isError && (
        <p role="alert" className="text-danger">
          {t(messageKey(error))}
        </p>
      )}
    </div>
  );
}
