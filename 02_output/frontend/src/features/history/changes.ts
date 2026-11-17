import type { Change } from "../../api/history";
import { countryName } from "../contacts/contactFields";

const ATTRIBUTE = "attribute:";
const TAG = "tag:";

type Translate = (key: string) => string;

/**
 * One changed item as a line of the History (R-010-01): `Label: old → new` for a Field,
 * `key: old → new` for an Attribute (R-008-01), `tag:<name> added|removed` for a Tag (R-009-01).
 */
export function changeText(change: Change, t: Translate, language: string): string {
  const { item, oldValue, newValue } = change;
  if (item.startsWith(TAG)) {
    return `${item} ${t(newValue ? "history.tagAdded" : "history.tagRemoved")}`;
  }
  if (item.startsWith(ATTRIBUTE)) {
    const shownNew = newValue ?? t("history.removed");
    return `${item.slice(ATTRIBUTE.length)}: ${oldValue ?? t("history.empty")} → ${shownNew}`;
  }
  const shown = (value: string | null) =>
    !value ? t("history.empty") : item === "country" ? countryName(value, language) : value;
  return `${t(`contacts.field.${item}`)}: ${shown(oldValue)} → ${shown(newValue)}`;
}
