import { type FormEvent, useId, useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useCreateGroup } from "../../api/groups";
import { ApiError, messageKey } from "../../api/problem";

/** Longest Group name after trimming (R-002-02, specification §3). */
const NAME_MAX = 100;
const NAME_LENGTH = "group.nameLength";
const NAME_LENGTH_KEY = `errors.${NAME_LENGTH}`;

/** R-002-02: 1 to 100 characters after trimming, counted as characters. */
function nameIsValid(name: string): boolean {
  const length = [...name].length;
  return length > 0 && length <= NAME_MAX;
}

/** The catalog key of a failed create: the name rule, or the problem's own text (UI-13). */
function errorKeyOf(error: unknown): string {
  if (error instanceof ApiError && error.errors.some((e) => e.code === NAME_LENGTH)) {
    return NAME_LENGTH_KEY;
  }
  return error instanceof ApiError ? messageKey(error.code) : "state.error";
}

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
      onError: (error) => setErrorKey(errorKeyOf(error)),
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
