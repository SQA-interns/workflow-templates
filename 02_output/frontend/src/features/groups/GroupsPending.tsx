import type { UseQueryResult } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { Button } from "@/components/ui/button";

/** Loading and error states while the Groups are read (UI-14); null once they are there. */
export function GroupsPending({ query }: { query: UseQueryResult }) {
  const { t } = useTranslation();
  if (query.isPending) return <p role="status">{t("state.loading")}</p>;
  if (query.isError) {
    return (
      <div role="alert" className="flex flex-col items-start gap-2">
        <p>{t("state.error")}</p>
        <Button variant="secondary" onClick={() => void query.refetch()}>
          {t("state.retry")}
        </Button>
      </div>
    );
  }
  return null;
}
