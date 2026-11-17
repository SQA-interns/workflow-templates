import { useState } from "react";

/** The Checked contacts of the list and how to change them (R-016-02). */
export interface Selection {
  /** The ids of the Checked contacts, in the order they were checked. */
  checked: string[];
  isChecked: (id: string) => boolean;
  check: (id: string, on: boolean) => void;
  /** Checks or unchecks every given Contact, e.g. those of the Page. */
  checkAll: (ids: string[], on: boolean) => void;
}

/**
 * The Checked contacts, kept across Pages and sort changes and cleared when the scope (search and
 * filters) changes (D-24 (b)).
 *
 * @param scope a key of the search and filters
 */
export function useSelection(scope: string): Selection {
  const [checked, setChecked] = useState<string[]>([]);
  const [shownScope, setShownScope] = useState(scope);
  if (scope !== shownScope) {
    // Another search or other filters: the checks belonged to another list.
    setShownScope(scope);
    setChecked([]);
  }

  function checkAll(ids: string[], on: boolean) {
    setChecked((current) =>
      on
        ? [...current, ...ids.filter((id) => !current.includes(id))]
        : current.filter((id) => !ids.includes(id)),
    );
  }

  return {
    checked,
    isChecked: (id) => checked.includes(id),
    check: (id, on) => checkAll([id], on),
    checkAll,
  };
}
