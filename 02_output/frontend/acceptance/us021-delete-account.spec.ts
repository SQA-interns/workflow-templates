// US-021 Delete my account: WF-19 (API part in the backend).
import { expect, test } from "@playwright/test";
import {
  ANA,
  MARKO,
  PASSWORD,
  api,
  createContact,
  editContact,
  resetStack,
  signIn,
  team,
} from "./support/stack";

test.beforeEach(resetStack);

test("AC-021-01 Delete my account: signed out, cannot sign in, Versions show Deleted user", async ({
  page,
  browser,
}) => {
  const t = await team();
  const id = await createContact(t.ana, t.groupId, {
    fields: { firstName: "Janez", lastName: "Novak" },
  });
  await editContact(t.marko, t.groupId, id, (u) => (u.fields.city = "Maribor"));
  await signIn(page, t.marko);

  await page.goto("/account");
  await page.getByLabel("Type your e-mail address to confirm").fill(MARKO);
  await page.getByRole("button", { name: "Delete", exact: true }).click();

  await expect(page).toHaveURL(/\/sign-in/);
  await page.locator('input[type="email"]').fill(MARKO);
  await page.locator('input[type="password"]').fill(PASSWORD);
  await page.locator('input[type="password"]').press("Enter");
  await expect(page.getByRole("alert")).toBeVisible();
  await expect(page).toHaveURL(/\/sign-in/);
  const members = await api(t.ana, "GET", `/groups/${t.groupId}/members`);
  expect(members.json.map((m: { email: string }) => m.email)).not.toContain(MARKO);

  const context = await browser.newContext({ baseURL: new URL(page.url()).origin });
  const anaPage = await context.newPage();
  await signIn(anaPage, t.ana);
  await anaPage.goto(`/groups/${t.groupId}/contacts/${id}/history`);
  await expect(anaPage.getByRole("main").getByText("Deleted user")).toBeVisible();
  await context.close();
});

test("AC-021-02 an Owner: Delete your Groups first: Konferenca 2025.", async ({ page }) => {
  const t = await team();
  await signIn(page, t.ana);

  await page.goto("/account");
  await page.getByLabel("Type your e-mail address to confirm").fill(ANA);
  await page.getByRole("button", { name: "Delete", exact: true }).click();

  await expect(page.getByText("Delete your Groups first: Konferenca 2025.")).toBeVisible();
  expect((await api(t.ana, "GET", "/me")).status).toBe(200);
});

test("EX-021-01 a typed e-mail that does not match keeps Delete disabled", async ({ page }) => {
  const t = await team();
  await signIn(page, t.marko);

  await page.goto("/account");
  await page.getByLabel("Type your e-mail address to confirm").fill("marko@example.si");

  await expect(page.getByRole("button", { name: "Delete", exact: true })).toBeDisabled();
  expect((await api(t.marko, "GET", "/me")).status).toBe(200);
});
