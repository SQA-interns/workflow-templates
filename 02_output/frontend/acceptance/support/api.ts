// Stack and API helpers for browser tests against the local stack (environments.md "local", D-32, D-37, D-38).
// Setup ("Given") goes through the public API and the Auth emulator; the steps under test go
// through the UI. Each test starts from an empty stack.
import { execFileSync } from "node:child_process";
import { expect } from "@playwright/test";

export const BASE_URL = process.env.E2E_BASE_URL ?? "http://localhost:8080";
export const EMULATOR_URL = process.env.E2E_EMULATOR_URL ?? "http://localhost:9199";
const DB_CONTAINER = process.env.E2E_DB_CONTAINER ?? "kontakti-postgres-1";
const PROJECT = "demo-contacts";
const KEY = "key=fake-api-key";

/** Password of every test account (seed-local.sh, AC-001-01). */
export const PASSWORD = "Kontakti#2026";
export const ANA = "ana.kovac@example.si";
export const MARKO = "marko.kranjc@gmail.com";
export const PETRA = "petra.horvat@example.si";

export interface User {
  email: string;
  displayName: string;
  uid: string;
  token: string;
}

export interface Answer {
  status: number;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  json: any;
}

/** Empties the database (every table but Flyway's) and deletes every emulator account (D-38). */
export async function resetStack(): Promise<void> {
  const sql =
    "DO $$ DECLARE t text; BEGIN SELECT string_agg(quote_ident(tablename), ', ') INTO t " +
    "FROM pg_tables WHERE schemaname = 'public' AND tablename <> 'flyway_schema_history'; " +
    "IF t IS NOT NULL THEN EXECUTE 'TRUNCATE ' || t || ' CASCADE'; END IF; END $$;";
  execFileSync("docker", [
    "exec",
    DB_CONTAINER,
    "psql",
    "-q",
    "-U",
    "contacts",
    "-d",
    "contacts",
    "-c",
    sql,
  ]);
  await emulator("DELETE", `/emulator/v1/projects/${PROJECT}/accounts`, undefined, true);
}

async function emulator(
  method: string,
  path: string,
  body?: unknown,
  admin = false,
): Promise<Answer> {
  const response = await fetch(EMULATOR_URL + path, {
    method,
    headers: {
      "Content-Type": "application/json",
      ...(admin ? { Authorization: "Bearer owner" } : {}),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await response.text();
  return { status: response.status, json: text ? JSON.parse(text) : undefined };
}

/** Creates an e-mail/password account, verified, and signs in (no API call yet). */
export async function account(email: string, displayName: string): Promise<User> {
  const created = await emulator(
    "POST",
    `/identitytoolkit.googleapis.com/v1/accounts:signUp?${KEY}`,
    {
      email,
      password: PASSWORD,
      returnSecureToken: true,
    },
  );
  expect(created.status, JSON.stringify(created.json)).toBe(200);
  const uid: string = created.json.localId;
  await emulator(
    "POST",
    `/identitytoolkit.googleapis.com/v1/projects/${PROJECT}/accounts:update`,
    { localId: uid, emailVerified: true, displayName },
    true,
  );
  return { email, displayName, uid, token: await signInToken(email) };
}

/** A fresh ID token for an e-mail/password account. */
export async function signInToken(email: string): Promise<string> {
  const answer = await emulator(
    "POST",
    `/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?${KEY}`,
    {
      email,
      password: PASSWORD,
      returnSecureToken: true,
    },
  );
  expect(answer.status, JSON.stringify(answer.json)).toBe(200);
  return answer.json.idToken as string;
}

/** Marks the e-mail of an account verified, as the link in the e-mail would. */
export async function verifyEmail(email: string): Promise<void> {
  const found = await emulator(
    "POST",
    `/identitytoolkit.googleapis.com/v1/projects/${PROJECT}/accounts:lookup`,
    { email: [email] },
    true,
  );
  const uid: string = found.json.users[0].localId;
  await emulator(
    "POST",
    `/identitytoolkit.googleapis.com/v1/projects/${PROJECT}/accounts:update`,
    { localId: uid, emailVerified: true },
    true,
  );
}

/** Number of emulator accounts with this e-mail. */
export async function accountsWithEmail(email: string): Promise<number> {
  const found = await emulator(
    "POST",
    `/identitytoolkit.googleapis.com/v1/projects/${PROJECT}/accounts:lookup`,
    { email: [email] },
    true,
  );
  return (found.json?.users ?? []).length;
}

/** The e-mail links the emulator "sent" to this address. */
export async function oobCodes(email: string): Promise<{ requestType: string }[]> {
  const answer = await emulator("GET", `/emulator/v1/projects/${PROJECT}/oobCodes`);
  return (answer.json.oobCodes ?? []).filter(
    (c: { email: string }) => c.email.toLowerCase() === email.toLowerCase(),
  );
}

/** A verified User who exists in the backend, with the language set (English by default, D-37). */
export async function user(
  email: string,
  displayName: string,
  language: "en" | "sl" = "en",
): Promise<User> {
  const u = await account(email, displayName);
  expect((await api(u, "GET", "/me")).status).toBe(200);
  if (language === "en") {
    expect((await api(u, "PATCH", "/me", { language: "en" })).status).toBe(200);
  }
  return u;
}

/** A call of the Contacts API through the frontend's proxy (`/api/v1`); user null = no token. */
export async function api(
  u: User | null,
  method: string,
  path: string,
  body?: unknown,
): Promise<Answer> {
  const response = await fetch(`${BASE_URL}/api/v1${path}`, {
    method,
    headers: {
      ...(u ? { Authorization: `Bearer ${u.token}` } : {}),
      ...(body === undefined ? {} : { "Content-Type": "application/json" }),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await response.text();
  let json: unknown = undefined;
  try {
    json = text ? JSON.parse(text) : undefined;
  } catch {
    json = text;
  }
  return { status: response.status, json };
}

async function ok(answer: Promise<Answer>, status = 200): Promise<Answer> {
  const a = await answer;
  expect(a.status, JSON.stringify(a.json)).toBe(status);
  return a;
}

export async function createGroup(owner: User, name: string): Promise<string> {
  return (await ok(api(owner, "POST", "/groups", { name }), 201)).json.id as string;
}

/** Creates an Invitation and returns the token of its link. */
export async function invite(
  groupId: string,
  owner: User,
  email: string,
  role: string,
): Promise<string> {
  const link: string = (
    await ok(api(owner, "POST", `/groups/${groupId}/invitations`, { email, role }), 201)
  ).json.link;
  return link.substring(link.lastIndexOf("/") + 1);
}

/** Makes the User a Member through an Invitation (R-004-01, R-004-04). */
export async function join(
  groupId: string,
  owner: User,
  member: User,
  role: string,
): Promise<void> {
  const token = await invite(groupId, owner, member.email, role);
  await ok(api(member, "POST", `/invitations/${token}/accept`));
}

export interface Team {
  ana: User;
  marko: User;
  petra: User;
  groupId: string;
}

/** "Konferenca 2025" with Ana Owner, Marko Editor, Petra Viewer. */
export async function team(): Promise<Team> {
  const ana = await user(ANA, "Ana Kovač");
  const marko = await user(MARKO, "Marko Kranjc");
  const petra = await user(PETRA, "Petra Horvat");
  const groupId = await createGroup(ana, "Konferenca 2025");
  await join(groupId, ana, marko, "EDITOR");
  await join(groupId, ana, petra, "VIEWER");
  return { ana, marko, petra, groupId };
}

export interface ContactInput {
  fields: Record<string, string>;
  attributes?: { key: string; value: string }[];
  tags?: string[];
}

export async function createContact(
  u: User,
  groupId: string,
  input: ContactInput,
): Promise<string> {
  const body = { fields: input.fields, attributes: input.attributes ?? [], tags: input.tags ?? [] };
  return (await ok(api(u, "POST", `/groups/${groupId}/contacts`, body), 201)).json.id as string;
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export async function getContact(u: User, groupId: string, id: string): Promise<any> {
  return (await ok(api(u, "GET", `/groups/${groupId}/contacts/${id}`))).json;
}

/** Saves a Contact as a form would: current content changed by the editor function. */
export async function editContact(
  u: User,
  groupId: string,
  id: string,
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  change: (update: any) => void,
): Promise<void> {
  const c = await getContact(u, groupId, id);
  const update = {
    fields: { ...c.fields },
    attributes: c.attributes.map((a: { key: string; value: string }) => ({
      key: a.key,
      value: a.value,
    })),
    tags: c.tags.map((t: { name: string }) => t.name),
    versionNumber: c.versionNumber,
  };
  change(update);
  await ok(api(u, "PUT", `/groups/${groupId}/contacts/${id}`, update));
}

export async function moveContactToRecycleBin(u: User, groupId: string, id: string): Promise<void> {
  const c = await getContact(u, groupId, id);
  await ok(
    api(u, "POST", `/groups/${groupId}/contacts/${id}/move-to-recycle-bin`, {
      versionNumber: c.versionNumber,
    }),
  );
}

/** "dd.mm.yyyy HH:mm" in Europe/Ljubljana (project-wide facts) of an instant. */
export function ljubljana(instant: string): string {
  const parts = new Intl.DateTimeFormat("en-GB", {
    timeZone: "Europe/Ljubljana",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
  }).formatToParts(new Date(instant));
  const part = (type: string) => parts.find((p) => p.type === type)?.value ?? "";
  return `${part("day")}.${part("month")}.${part("year")} ${part("hour")}:${part("minute")}`;
}

/** Runs SQL in the local database (only for data no API can create fast enough, D-38). */
export function sql(statement: string): void {
  execFileSync("docker", [
    "exec",
    DB_CONTAINER,
    "psql",
    "-q",
    "-v",
    "ON_ERROR_STOP=1",
    "-U",
    "contacts",
    "-d",
    "contacts",
    "-c",
    statement,
  ]);
}

/** Inserts Contacts through the persistence contract, as seed/seed.sql does (D-38). */
export function bulkContacts(groupId: string, ownerId: string, count: number): void {
  sql(
    "INSERT INTO contact (id, group_id, first_name, last_name, state, version_number, search_text, sort_name," +
      " created_at, created_by, updated_at, updated_by)" +
      ` SELECT gen_random_uuid(), '${groupId}', 'Oseba', 'Priimek' || lpad(n::text, 5, '0'), 'active', 1,` +
      " 'oseba priimek' || lpad(n::text, 5, '0'), 'Priimek' || lpad(n::text, 5, '0'), now()," +
      ` '${ownerId}', now(), '${ownerId}' FROM generate_series(1, ${count}) AS n`,
  );
}
