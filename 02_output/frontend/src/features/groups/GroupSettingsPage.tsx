import { type FormEvent, useId, useState } from "react";
import { useTranslation } from "react-i18next";
import { useParams } from "react-router";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { type Group, useGroups, useRenameGroup } from "../../api/groups";
import { NAME_LENGTH_KEY, nameErrorKeyOf, nameIsValid } from "./groupName";
import { GroupsPending } from "./GroupsPending";
import { MoveGroupToRecycleBin } from "./MoveGroupToRecycleBin";

/**
 * WF-14 Group settings, for the Owner only (BR-04): rename the Group (R-003-01) and move it to the
 * recycle bin after a confirmation (R-003-02, EX-003-04).
 */
export function GroupSettingsPage() {
  const { t } = useTranslation();
  const { groupId = "" } = useParams();
  const groups = useGroups();
  const group = groups.data?.find((candidate) => candidate.id === groupId);

  if (!groups.isSuccess) return <GroupsPending query={groups} />;
  if (group?.role !== "OWNER") return <p role="alert">{t("errors.auth.forbidden")}</p>;
  return (
    <div className="mx-auto flex w-full max-w-2xl flex-col gap-4">
      <h1 className="text-2xl font-semibold">{t("groups.settings.title")}</h1>
      <RenameGroup key={group.id} group={group} />
      <Card className="flex flex-col items-start gap-3">
        <h2 className="text-lg font-semibold">{t("groups.settings.move.title")}</h2>
        <p className="text-text-muted">{t("groups.settings.move.description")}</p>
        <MoveGroupToRecycleBin group={group} />
      </Card>
    </div>
  );
}

/** The rename card: the name is checked as when creating a Group (R-002-02, EX-003-06). */
function RenameGroup({ group }: { group: Group }) {
  const { t } = useTranslation();
  const id = useId();
  const rename = useRenameGroup(group.id);
  const [name, setName] = useState(group.name);
  const [errorKey, setErrorKey] = useState<string | null>(null);

  function submit(event: FormEvent) {
    event.preventDefault();
    rename.reset();
    const trimmed = name.trim();
    if (!nameIsValid(trimmed)) {
      setErrorKey(NAME_LENGTH_KEY);
      return;
    }
    setErrorKey(null);
    rename.mutate(trimmed, { onError: (error) => setErrorKey(nameErrorKeyOf(error)) });
  }

  return (
    <Card>
      <form className="flex flex-col items-start gap-3" onSubmit={submit} noValidate>
        <h2 className="text-lg font-semibold">{t("groups.settings.rename.title")}</h2>
        <div className="flex w-full flex-col gap-1">
          <Label htmlFor={`${id}-name`}>
            {t("groups.create.name")}
            <span aria-hidden="true"> *</span>
          </Label>
          <Input
            id={`${id}-name`}
            autoComplete="off"
            aria-required="true"
            aria-invalid={errorKey === NAME_LENGTH_KEY}
            aria-describedby={`${id}-hint`}
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
          <p id={`${id}-hint`} className="text-sm text-text-muted">
            {t("groups.settings.rename.hint")}
          </p>
        </div>
        {errorKey && (
          <p role="alert" className="text-sm font-semibold text-danger">
            {t(errorKey)}
          </p>
        )}
        {rename.isSuccess && (
          <p role="status" className="text-sm">
            {t("groups.settings.rename.saved")}
          </p>
        )}
        <Button type="submit" disabled={rename.isPending} aria-busy={rename.isPending}>
          {t("groups.settings.rename.submit")}
        </Button>
      </form>
    </Card>
  );
}
