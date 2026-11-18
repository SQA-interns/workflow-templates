import i18n from "./index";

/** The code the API sends instead of the name of a User who deleted the account (R-021-03). */
const DELETED_USER = "user.deleted";

/** The name of a User as shown: "Deleted user" in the current language for a deleted User. */
export function userName(name: string): string {
  return name === DELETED_USER ? i18n.t(DELETED_USER) : name;
}
