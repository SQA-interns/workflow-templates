import type { Group } from "../../api/groups";

/** Browser storage key of the Group a User selected last (R-002-05, specification §4 US-002). */
function storageKey(userId: string): string {
  return `contacts.lastGroup.${userId}`;
}

/** Keeps the Group the User selected last; blocked storage only forgets the choice. */
export function rememberGroup(userId: string, groupId: string): void {
  try {
    localStorage.setItem(storageKey(userId), groupId);
  } catch {
    // Storage blocked or full: the first Group opens next time (R-002-05).
  }
}

/** The Group the User selected last in this browser, or null. */
export function lastGroupId(userId: string): string | null {
  try {
    return localStorage.getItem(storageKey(userId));
  } catch {
    return null;
  }
}

/** The Group to open: the one selected last if still available, else the first (R-002-05). */
export function startGroupId(groups: Group[], lastId: string | null): string | null {
  const last = groups.find((group) => group.id === lastId);
  return (last ?? groups[0])?.id ?? null;
}
