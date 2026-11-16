import type { Language } from "./index";

/** Project-wide facts: everything shown to a User is in Europe/Ljubljana (R-020-03). */
const TIME_ZONE = "Europe/Ljubljana";

/** Number conventions per language (project-wide facts: `10.000` / `10,000`). */
const slovenianNumbers = new Intl.NumberFormat("sl-SI");
const englishNumbers = new Intl.NumberFormat("en-GB");

const dateParts = new Intl.DateTimeFormat("en-GB", {
  timeZone: TIME_ZONE,
  day: "2-digit",
  month: "2-digit",
  year: "numeric",
  hour: "2-digit",
  minute: "2-digit",
  hourCycle: "h23",
});

function parts(iso: string): Record<string, string> {
  return Object.fromEntries(
    dateParts.formatToParts(new Date(iso)).map((part) => [part.type, part.value]),
  );
}

/** A date as `dd.mm.yyyy`, the same in both languages (project-wide facts). */
export function formatDate(iso: string): string {
  const { day, month, year } = parts(iso);
  return `${day}.${month}.${year}`;
}

/** A time as `dd.mm.yyyy HH:mm`, 24-hour, in both languages (R-020-03, AC-020-03). */
export function formatDateTime(iso: string): string {
  const { hour, minute } = parts(iso);
  return `${formatDate(iso)} ${hour}:${minute}`;
}

/** A number with the separators of the language (R-020-03, EX-020-02). */
export function formatNumber(value: number, language: Language): string {
  return (language === "sl" ? slovenianNumbers : englishNumbers).format(value);
}
