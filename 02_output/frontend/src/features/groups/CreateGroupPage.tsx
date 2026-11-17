import { type FormEvent, useId, useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useCreateGroup } from "../../api/groups";
import { NAME_LENGTH_KEY, nameErrorKeyOf, nameIsValid } from "./groupName";

/** WF-04 Create a Group: the new Group opens as the current Group (R-002-01). */
export function CreateGroupPage() {
  const { t } = useTranslation();
  const id = useId();
  const navigate = useNavigate();
  const createGroup = useCreateGroup();
  const [name, setName] = useState("");
  const [errorKey, setErrorKey] = useState<string | null>(null);

  function submit(event: FormEvent) {
    event.preventDefault();
    const trimmed = name.trim();
    if (!nameIsValid(trimmed)) {
      setErrorKey(NAME_LENGTH_KEY);
      return;
    }
    setErrorKey(null);
    createGroup.mutate(trimmed, {
      onSuccess: (group) => void navigate(`/groups/${group.id}/contacts`),
      onError: (error) => setErrorKey(nameErrorKeyOf(error)),
    });
  }

  return (
    <div className="mx-auto w-full max-w-md">
      <Card>
        <form className="flex flex-col gap-4" onSubmit={submit} noValidate>
          <h1 className="text-xl font-semibold">{t("groups.create.title")}</h1>
          <div className="flex flex-col gap-1">
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
              {t("groups.create.hint")}
            </p>
          </div>
          {errorKey && (
            <p role="alert" className="text-sm font-semibold text-danger">
              {t(errorKey)}
            </p>
          )}
          <div className="flex flex-wrap gap-2">
            <Button type="submit" disabled={createGroup.isPending}>
              {t("groups.create.submit")}
            </Button>
            <Button type="button" variant="ghost" onClick={() => void navigate(-1)}>
              {t("groups.create.cancel")}
            </Button>
          </div>
        </form>
      </Card>
    </div>
  );
}
