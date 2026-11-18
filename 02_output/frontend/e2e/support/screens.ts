// Every screen of specification §9.1 with the data it shows, for the NFR checks in the browser:
// accessibility (NFR-07, e2e/accessibility.spec.ts) and both languages (NFR-08,
// e2e/languages.spec.ts). The data is made through the API, as the acceptance tests do (D-38).
import { expect, type Page } from "@playwright/test";
import {
  PASSWORD,
  api,
  createContact,
  createGroup,
  editContact,
  invite,
  moveContactToRecycleBin,
  resetStack,
  signIn,
  team,
  user,
  type Team,
  type User,
} from "../../acceptance/support/stack";

/** The three widths of UI-22, in the order mobile, tablet, desktop. */
export const WIDTHS = [
  { label: "mobile", width: 375, height: 812 },
  { label: "tablet", width: 768, height: 1024 },
  { label: "desktop", width: 1440, height: 900 },
] as const;

/** A person who is a Member of no Group (WF-05) and the invitee of WF-16. */
const NEWCOMER = { email: "nova.clanica@example.si", displayName: "Nova Članica" };

/** The data every screen of the list is shown with. */
export interface Fixture {
  team: Team;
  newcomer: User;
  janez: string;
  jNovak: string;
  invitationToken: string;
}

/**
 * Empties the stack and makes "Konferenca 2025" (Ana Owner, Marko Editor, Petra Viewer) with
 * "Janez Novak" (two Versions, an Attribute, a Tag), his possible duplicate "J. Novak" (E2E-04),
 * "Maja Zupan" in the recycle bin, Ana's "Podjetje Beta" in the recycle bin (WF-15) and an
 * Invitation for the newcomer (WF-16).
 */
export async function makeFixture(): Promise<Fixture> {
  await resetStack();
  const t = await team();
  const newcomer = await user(NEWCOMER.email, NEWCOMER.displayName);
  const janez = await createContact(t.ana, t.groupId, {
    fields: {
      firstName: "Janez",
      lastName: "Novak",
      email: "janez.novak@example.si",
      phone: "+386 41 123 456",
      company: "Podjetje d.o.o.",
    },
    attributes: [{ key: "kompetenca", value: "spletnoProgramiranje" }],
    tags: ["udelezenec2025"],
  });
  await editContact(t.ana, t.groupId, janez, (c) => {
    c.fields.city = "Ljubljana";
  });
  const jNovak = await createContact(t.ana, t.groupId, {
    fields: { firstName: "J.", lastName: "Novak", email: "janez.novak@example.si" },
  });
  const maja = await createContact(t.ana, t.groupId, {
    fields: { firstName: "Maja", lastName: "Zupan" },
  });
  await moveContactToRecycleBin(t.ana, t.groupId, maja);
  const beta = await createGroup(t.ana, "Podjetje Beta");
  expect((await api(t.ana, "POST", `/groups/${beta}/move-to-recycle-bin`)).status).toBe(204);
  const invitationToken = await invite(t.groupId, t.ana, NEWCOMER.email, "VIEWER");
  return { team: t, newcomer, janez, jNovak, invitationToken };
}

/** Sets the language of every signed-up User of the fixture (R-020-02). */
export async function setLanguage(fx: Fixture, language: "sl" | "en"): Promise<void> {
  for (const u of [fx.team.ana, fx.team.marko, fx.team.petra, fx.newcomer]) {
    expect((await api(u, "PATCH", "/me", { language })).status).toBe(200);
  }
}

/** One screen: its wireframe id, name, and how to open it with the fixture. */
export interface Screen {
  id: string;
  name: string;
  /** Before sign-in the UI is Slovenian only (R-020-01, D-37). */
  signedIn: boolean;
  open(page: Page, fx: Fixture): Promise<void>;
}

/** Signs the User in and opens the path; done when the page title and its data are shown. */
function as(who: (fx: Fixture) => User, path: (fx: Fixture) => string) {
  return async (page: Page, fx: Fixture) => {
    await signIn(page, who(fx));
    await page.goto(path(fx));
    await ready(page);
  };
}

/** The page title is shown and no request is still running. */
async function ready(page: Page): Promise<void> {
  await expect(page.getByRole("heading", { level: 1 }).first()).toBeVisible();
  await page.waitForLoadState("networkidle");
}

// The screens are shared out among the Members by Role, so that no one exceeds the rate limit of
// 100 requests a minute (SR-03) during a run: Owner-only screens as Ana, changes as Marko (Editor),
// reading as Petra (Viewer).
const ana = (fx: Fixture) => fx.team.ana;
const marko = (fx: Fixture) => fx.team.marko;
const petra = (fx: Fixture) => fx.team.petra;
const group = (fx: Fixture) => `/groups/${fx.team.groupId}`;
const janez = (fx: Fixture) => `${group(fx)}/contacts/${fx.janez}`;

export const SCREENS: Screen[] = [
  {
    id: "WF-01",
    name: "sign-in",
    signedIn: false,
    open: async (page) => {
      await page.goto("/sign-in");
      await ready(page);
    },
  },
  {
    id: "WF-02",
    name: "sign-up",
    signedIn: false,
    open: async (page) => {
      await page.goto("/sign-up");
      await ready(page);
    },
  },
  {
    id: "WF-03",
    name: "confirm-email",
    signedIn: false,
    open: async (page) => {
      await page.goto("/sign-up");
      await page.locator('input[type="email"]').fill(`nepotrjen.${Date.now()}@example.si`);
      await page.locator('input[type="password"]').fill(PASSWORD);
      await page.locator('input[type="password"]').press("Enter");
      await expect(page).toHaveURL(/\/verify-email/);
      await ready(page);
    },
  },
  {
    id: "WF-04",
    name: "create-group",
    signedIn: true,
    open: as(ana, () => "/groups/new"),
  },
  {
    id: "WF-05",
    name: "no-groups",
    signedIn: true,
    open: as(
      (fx) => fx.newcomer,
      () => "/",
    ),
  },
  {
    id: "WF-06",
    name: "contact-list",
    signedIn: true,
    open: as(petra, (fx) => `${group(fx)}/contacts`),
  },
  { id: "WF-07", name: "contact-details", signedIn: true, open: as(petra, janez) },
  {
    id: "WF-08",
    name: "contact-form",
    signedIn: true,
    open: as(marko, (fx) => `${janez(fx)}/edit`),
  },
  {
    id: "WF-09",
    name: "history",
    signedIn: true,
    open: as(petra, (fx) => `${janez(fx)}/history`),
  },
  {
    id: "WF-10",
    name: "version",
    signedIn: true,
    open: as(petra, (fx) => `${janez(fx)}/history/1`),
  },
  {
    id: "WF-11",
    name: "recycle-bin",
    signedIn: true,
    open: as(marko, (fx) => `${group(fx)}/recycle-bin`),
  },
  {
    id: "WF-13",
    name: "members",
    signedIn: true,
    open: as(ana, (fx) => `${group(fx)}/members`),
  },
  {
    id: "WF-14",
    name: "group-settings",
    signedIn: true,
    open: as(ana, (fx) => `${group(fx)}/settings`),
  },
  {
    id: "WF-15",
    name: "deleted-groups",
    signedIn: true,
    open: as(ana, () => "/deleted-groups"),
  },
  {
    id: "WF-16",
    name: "invitation",
    signedIn: true,
    open: as(
      (fx) => fx.newcomer,
      (fx) => `/invitations/${fx.invitationToken}`,
    ),
  },
  {
    id: "WF-17",
    name: "possible-duplicates",
    signedIn: true,
    open: as(marko, (fx) => `${group(fx)}/duplicates`),
  },
  {
    id: "WF-18",
    name: "merge",
    signedIn: true,
    open: as(marko, (fx) => `${group(fx)}/merge?first=${fx.janez}&second=${fx.jNovak}`),
  },
  { id: "WF-19", name: "account", signedIn: true, open: as(petra, () => "/account") },
  {
    id: "WF-20",
    name: "not-found",
    signedIn: true,
    open: as(petra, () => "/no-such-page"),
  },
];

/** Sets the viewport to one of the three widths and lets the layout settle. */
export async function atWidth(page: Page, w: (typeof WIDTHS)[number]): Promise<void> {
  await page.setViewportSize({ width: w.width, height: w.height });
  await page.waitForTimeout(300);
}
