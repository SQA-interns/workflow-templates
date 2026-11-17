/** Where a User goes after signing in or confirming the e-mail: back to an Invitation (R-004-07). */
const KEY = "contacts.returnTo";

/** Only Invitation links are kept, so the value can never lead off the product. */
const INVITATION_PATH = /^\/invitations\/[A-Za-z0-9_-]+$/;

/**
 * Remembers the Invitation the signed-out visitor opened (R-004-07).
 *
 * @param path the path of the Invitation link
 */
export function rememberReturnTo(path: string): void {
  if (!INVITATION_PATH.test(path)) return;
  try {
    sessionStorage.setItem(KEY, path);
  } catch {
    // Storage blocked: after signing in the User starts at the start screen.
  }
}

/** The remembered Invitation, else the start screen. */
export function returnTo(): string {
  try {
    const path = sessionStorage.getItem(KEY);
    return path !== null && INVITATION_PATH.test(path) ? path : "/";
  } catch {
    return "/";
  }
}

/** Forgets the remembered Invitation once it is shown to the signed-in User. */
export function forgetReturnTo(): void {
  try {
    sessionStorage.removeItem(KEY);
  } catch {
    // Nothing was stored.
  }
}
