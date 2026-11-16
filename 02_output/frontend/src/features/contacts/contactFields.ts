// The Fields of a Contact and the checks of specification §3, repeated in the browser for inline
// errors (UI-13); the API checks them again (SB-01).

/** The Fields in the order of the Contact form (D-23). */
export const FIELD_NAMES = [
  "firstName",
  "lastName",
  "email",
  "phone",
  "company",
  "jobTitle",
  "street",
  "postalCode",
  "city",
  "country",
  "note",
] as const;

export type FieldName = (typeof FIELD_NAMES)[number];

/** The form's values: every Field as typed. */
export type FieldValues = Record<FieldName, string>;

/** The Fields as sent: trimmed, an empty Field is null (R-006-07). */
export type ContactFields = Record<FieldName, string | null>;

/** One problem of a Field, or of the name parts as a whole (`name`). */
export interface FieldProblem {
  field: FieldName | "name";
  code: string;
  params?: { max: number };
}

/** Longest value of each text Field in characters (business-rules.md, Data model). */
const MAX_LENGTH = new Map<FieldName, number>([
  ["firstName", 100],
  ["lastName", 100],
  ["email", 254],
  ["phone", 30],
  ["company", 200],
  ["jobTitle", 100],
  ["street", 200],
  ["postalCode", 20],
  ["city", 100],
  ["note", 2000],
]);

/** One @, a local part, a dot in the domain, no spaces (R-006-04); digits, spaces, + - ( ) / (D-23). */
const FORMATS = new Map<FieldName, { pattern: RegExp; code: string }>([
  ["email", { pattern: /^[^@\s]+@[^@\s]*\.[^@\s]*$/, code: "field.emailInvalid" }],
  ["phone", { pattern: /^[0-9 +\-()/]*$/, code: "field.phoneInvalid" }],
]);

/** ISO 3166-1 alpha-2 codes, the list the API checks against (Java `Locale.getISOCountries()`). */
const COUNTRY_CODES =
  "AD AE AF AG AI AL AM AO AQ AR AS AT AU AW AX AZ BA BB BD BE BF BG BH BI BJ BL BM BN BO BQ BR BS BT BV BW BY BZ CA CC CD CF CG CH CI CK CL CM CN CO CR CU CV CW CX CY CZ DE DJ DK DM DO DZ EC EE EG EH ER ES ET FI FJ FK FM FO FR GA GB GD GE GF GG GH GI GL GM GN GP GQ GR GS GT GU GW GY HK HM HN HR HT HU ID IE IL IM IN IO IQ IR IS IT JE JM JO JP KE KG KH KI KM KN KP KR KW KY KZ LA LB LC LI LK LR LS LT LU LV LY MA MC MD ME MF MG MH MK ML MM MN MO MP MQ MR MS MT MU MV MW MX MY MZ NA NC NE NF NG NI NL NO NP NR NU NZ OM PA PE PF PG PH PK PL PM PN PR PS PT PW PY QA RE RO RS RU RW SA SB SC SD SE SG SH SI SJ SK SL SM SN SO SR SS ST SV SX SY SZ TC TD TF TG TH TJ TK TL TM TN TO TR TT TV TW TZ UA UG UM US UY UZ VA VC VE VG VI VN VU WF WS YE YT ZA ZM ZW".split(
    " ",
  );

export function emptyFields(): FieldValues {
  return Object.fromEntries(FIELD_NAMES.map((name) => [name, ""])) as FieldValues;
}

function problemOf(field: FieldName, value: string): FieldProblem | null {
  const max = MAX_LENGTH.get(field);
  if (max !== undefined && [...value].length > max) {
    return { field, code: "field.tooLong", params: { max } };
  }
  const format = FORMATS.get(field);
  return format && !format.pattern.test(value) ? { field, code: format.code } : null;
}

/** The trimmed Fields and every problem with them (R-006-02 … R-006-04, R-006-07). */
export function checkFields(values: FieldValues): {
  trimmed: ContactFields;
  problems: FieldProblem[];
} {
  const typed = new Map(Object.entries(values));
  const entries = FIELD_NAMES.map((name): [FieldName, string | null] => [
    name,
    (typed.get(name) ?? "").trim() || null,
  ]);
  const trimmed = Object.fromEntries(entries) as ContactFields;
  const problems: FieldProblem[] = [];
  if (!trimmed.firstName && !trimmed.lastName && !trimmed.company) {
    problems.push({ field: "name", code: "contact.nameRequired" });
  }
  for (const [name, value] of entries) {
    const problem = value === null ? null : problemOf(name, value);
    if (problem) problems.push(problem);
  }
  return { trimmed, problems };
}

/** The countries by name in the language, A–Z in its collation (R-006-05, project-wide facts). */
export function countryOptions(language: string): { code: string; name: string }[] {
  const names = new Intl.DisplayNames([language], { type: "region" });
  const collator = new Intl.Collator(language);
  return COUNTRY_CODES.map((code) => ({ code, name: names.of(code) ?? code })).sort((a, b) =>
    collator.compare(a.name, b.name),
  );
}
