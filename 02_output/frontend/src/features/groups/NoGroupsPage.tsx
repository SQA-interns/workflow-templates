import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";

/** WF-05 No Groups yet: the empty state of a User who is a Member of no Group (R-001-07). */
export function NoGroupsPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  return (
    <div className="mx-auto w-full max-w-md">
      <Card className="flex flex-col items-center gap-4 text-center">
        <h1 className="text-xl font-semibold">{t("groups.none.title")}</h1>
        <p className="text-text-muted">{t("groups.none.body")}</p>
        <Button onClick={() => void navigate("/groups/new")}>{t("groups.none.create")}</Button>
      </Card>
    </div>
  );
}
