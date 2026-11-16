// US-004 Invite Members: WF-13 (Invitations), WF-16 (API part in the backend).
import { expect, test } from "@playwright/test";
import {
  ANA,
  BASE_URL,
  MARKO,
  PETRA,
  api,
  choose,
  createGroup,
  invite,
  ljubljana,
  resetStack,
  signIn,
  stubGet,
  user,
} from "./support/stack";

test.beforeEach(resetStack);

async function anaOwnsKonferenca() {
  const ana = await user(ANA, "Ana Kovač");
  const groupId = await createGroup(ana, "Konferenca 2025");
  return { ana, groupId };
}

test("AC-004-01 the Owner invites an Editor: link with Copy link, open Invitation listed", async ({
  page,
}) => {
  const { ana, groupId } = await anaOwnsKonferenca();
  await signIn(page, ana);

  await page.goto(`/groups/${groupId}/members`);
  await page.getByLabel("E-mail").fill(MARKO);
  await choose(page.getByLabel("Role"), "Editor");
  await page.getByRole("button", { name: "Create Invitation" }).click();

  await expect(page.getByRole("button", { name: "Copy link" })).toBeVisible();
  await expect(page.getByText(/\/invitations\/[A-Za-z0-9_-]{22,128}/)).toBeVisible();
  const open = await api(ana, "GET", `/groups/${groupId}/invitations`);
  const row = page.getByRole("row", { name: MARKO });
  await expect(row).toContainText("Editor");
  await expect(row).toContainText(ljubljana(open.json[0].expiresAt));
});

test("AC-004-01 the expiry is shown in Europe/Ljubljana: 12.03.2026 10:00", async ({ page }) => {
  const { ana, groupId } = await anaOwnsKonferenca();
  await stubGet(page, `/api/v1/groups/${groupId}/invitations`, [
    {
      id: "00000000-0000-4000-8000-000000000001",
      email: MARKO,
      role: "EDITOR",
      expiresAt: "2026-03-12T09:00:00Z",
    },
  ]);
  await signIn(page, ana);

  await page.goto(`/groups/${groupId}/members`);

  await expect(page.getByRole("row", { name: /marko\.kranjc@gmail\.com/ })).toContainText(
    "12.03.2026 10:00",
  );
});

test("AC-004-02 the invitee accepts and sees the Group in the switcher", async ({ page }) => {
  const { ana, groupId } = await anaOwnsKonferenca();
  const marko = await user(MARKO, "Marko Kranjc");
  const token = await invite(groupId, ana, MARKO, "EDITOR");
  await signIn(page, marko);

  await page.goto(`/invitations/${token}`);
  await expect(page.getByRole("heading", { level: 1 })).toContainText("Konferenca 2025");
  await expect(page.getByText("You are invited as Editor.")).toBeVisible();
  await page.getByRole("button", { name: "Accept" }).click();

  await expect(page.getByLabel("Group switcher")).toContainText("Konferenca 2025");
  expect((await api(marko, "GET", "/groups")).json[0].role).toBe("EDITOR");
  expect((await api(ana, "GET", `/groups/${groupId}/invitations`)).json).toEqual([]);
});

test("AC-004-03 another e-mail: This Invitation is for another e-mail address.", async ({
  page,
}) => {
  const { ana, groupId } = await anaOwnsKonferenca();
  const petra = await user(PETRA, "Petra Horvat");
  const token = await invite(groupId, ana, MARKO, "EDITOR");
  await signIn(page, petra);

  await page.goto(`/invitations/${token}`);

  await expect(page.getByText("This Invitation is for another e-mail address.")).toBeVisible();
  expect((await api(petra, "GET", "/groups")).json).toEqual([]);
});

test("AC-004-04 an accepted link opened again: This Invitation is no longer valid.", async ({
  page,
}) => {
  const { ana, groupId } = await anaOwnsKonferenca();
  const marko = await user(MARKO, "Marko Kranjc");
  const token = await invite(groupId, ana, MARKO, "EDITOR");
  expect((await api(marko, "POST", `/invitations/${token}/accept`)).status).toBe(200);
  await signIn(page, marko);

  await page.goto(`/invitations/${token}`);

  await expect(page.getByText("This Invitation is no longer valid.")).toBeVisible();
});

test("AC-004-06 the Owner revokes; the link is no longer valid", async ({ page, browser }) => {
  const { ana, groupId } = await anaOwnsKonferenca();
  const petra = await user(PETRA, "Petra Horvat");
  const token = await invite(groupId, ana, PETRA, "VIEWER");
  await signIn(page, ana);

  await page.goto(`/groups/${groupId}/members`);
  await page
    .getByRole("row", { name: /petra\.horvat@example\.si/ })
    .getByRole("button", { name: "Revoke" })
    .click();

  await expect(page.getByRole("row", { name: /petra\.horvat@example\.si/ })).toHaveCount(0);
  expect((await api(ana, "GET", `/groups/${groupId}/invitations`)).json).toEqual([]);
  const context = await browser.newContext({ baseURL: BASE_URL });
  const petraPage = await context.newPage();
  await signIn(petraPage, petra);
  await petraPage.goto(`/invitations/${token}`);
  await expect(petraPage.getByText("This Invitation is no longer valid.")).toBeVisible();
  await context.close();
});

test("EX-004-04 inviting a Member: This person is already a Member.", async ({ page }) => {
  const { ana, groupId } = await anaOwnsKonferenca();
  await signIn(page, ana);

  await page.goto(`/groups/${groupId}/members`);
  await page.getByLabel("E-mail").fill(ANA);
  await choose(page.getByLabel("Role"), "Editor");
  await page.getByRole("button", { name: "Create Invitation" }).click();

  await expect(page.getByText("This person is already a Member.")).toBeVisible();
  expect((await api(ana, "GET", `/groups/${groupId}/invitations`)).json).toEqual([]);
});

test("EX-004-06 an invalid e-mail: Enter a valid e-mail address.", async ({ page }) => {
  const { ana, groupId } = await anaOwnsKonferenca();
  await signIn(page, ana);

  await page.goto(`/groups/${groupId}/members`);
  await page.getByLabel("E-mail").fill("marko@");
  await page.getByRole("button", { name: "Create Invitation" }).click();

  await expect(page.getByText("Enter a valid e-mail address.")).toBeVisible();
  expect((await api(ana, "GET", `/groups/${groupId}/invitations`)).json).toEqual([]);
});
