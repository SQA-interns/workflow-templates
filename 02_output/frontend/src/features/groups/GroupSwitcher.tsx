import { Check, ChevronDown, FolderOpen, Plus } from "lucide-react";
import { type KeyboardEvent, useId, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router";
import { Button } from "@/components/ui/button";
import { useGroups } from "../../api/groups";
import { useMe } from "../../api/me";
import { lastGroupId, startGroupId } from "./currentGroup";

/**
 * The header control that shows the current Group and lists the User's Groups (R-002-03);
 * selecting one opens its Contact list (R-002-04). Hidden while the User has no Group (WF-05).
 */
export function GroupSwitcher() {
  const { t } = useTranslation();
  const { groupId } = useParams();
  const me = useMe();
  const groups = useGroups().data ?? [];
  const [open, setOpen] = useState(false);
  const listId = useId();

  const currentId = groupId ?? (me.data ? startGroupId(groups, lastGroupId(me.data.id)) : null);
  const current = groups.find((group) => group.id === currentId);
  if (!current) return null;

  function closeOnEscape(event: KeyboardEvent) {
    if (event.key === "Escape") setOpen(false);
  }

  return (
    <div className="relative min-w-0">
      <Button
        variant="secondary"
        className="max-w-full px-2 md:px-3"
        aria-label={t("groups.switcher.label", { name: current.name })}
        aria-expanded={open}
        aria-controls={listId}
        onKeyDown={closeOnEscape}
        onClick={() => setOpen((value) => !value)}
      >
        <FolderOpen aria-hidden="true" />
        <span className="truncate">{current.name}</span>
        <ChevronDown aria-hidden="true" />
      </Button>
      {open && (
        <ul
          id={listId}
          className="absolute left-0 z-10 mt-1 max-h-96 min-w-56 overflow-y-auto rounded-md border bg-surface-raised p-1"
        >
          {groups.map((group) => (
            <li key={group.id}>
              <Link
                to={`/groups/${group.id}/contacts`}
                aria-current={group.id === current.id ? "true" : undefined}
                className="flex min-h-11 items-center gap-2 rounded-md px-3 text-sm hover:bg-surface focus-visible:outline-2 focus-visible:outline-ring"
                onKeyDown={closeOnEscape}
                onClick={() => setOpen(false)}
              >
                <Check
                  aria-hidden="true"
                  className={`size-4 shrink-0 ${group.id === current.id ? "" : "invisible"}`}
                />
                <span className="truncate">{group.name}</span>
              </Link>
            </li>
          ))}
          <li className="mt-1 border-t pt-1">
            <Link
              to="/groups/new"
              className="flex min-h-11 items-center gap-2 rounded-md px-3 text-sm hover:bg-surface focus-visible:outline-2 focus-visible:outline-ring"
              onKeyDown={closeOnEscape}
              onClick={() => setOpen(false)}
            >
              <Plus aria-hidden="true" className="size-4 shrink-0" />
              {t("groups.switcher.create")}
            </Link>
          </li>
        </ul>
      )}
    </div>
  );
}
