// US-002 Create and switch Groups: WF-04, WF-05, WF-06 header (API part in the backend).
import { expect, test } from "@playwright/test";
import {
  ANA,
  api,
  createContact,
  createGroup,
  listPath,
  resetStack,
  selectGroup,
  signIn,
  signOut,
  user,
} from "./support/stack";

test.beforeEach(resetStack);

test("AC-002-01 the new Group is current, Ana is Owner, No Contacts yet.", async ({ page }) => {
  const ana = await user(ANA, "Ana Kovač");
  await signIn(page, ana);

  await page.getByRole("button", { name: "Create a Group" }).click();
  await page.getByLabel("Group name").fill("Konferenca 2025");
  await page.getByRole("button", { name: "Create a Group" }).click();

  await expect(page).toHaveURL(/\/groups\/[0-9a-f-]{36}\/contacts/);
  await expect(page.getByLabel("Group switcher")).toContainText("Konferenca 2025");
  await expect(page.getByText("No Contacts yet.")).toBeVisible();
  await expect(page.getByRole("button", { name: "Add a Contact" }).first()).toBeVisible();
  const groups = await api(ana, "GET", "/groups");
  expect(groups.json.map((g: { role: string }) => g.role)).toEqual(["OWNER"]);
});

test("AC-002-02 selecting a Group in the switcher shows its Contacts only", async ({ page }) => {
  const ana = await user(ANA, "Ana Kovač");
  const konferenca = await createGroup(ana, "Konferenca 2025");
  const alfa = await createGroup(ana, "Podjetje Alfa");
  await createContact(ana, konferenca, { fields: { firstName: "Janez", lastName: "Novak" } });
  await createContact(ana, alfa, { fields: { firstName: "Maja", lastName: "Zupan" } });
  await signIn(page, ana);

  await selectGroup(page, "Podjetje Alfa");

  await expect(page).toHaveURL((url) => url.pathname === listPath(alfa));
  await expect(page.getByRole("link", { name: "Maja Zupan" })).toBeVisible();
  await expect(page.getByText("Janez Novak")).toHaveCount(0);
});

test("AC-002-04 a blank Group name: Enter a name of 1 to 100 characters.", async ({ page }) => {
  const ana = await user(ANA, "Ana Kovač");
  await signIn(page, ana);

  await page.goto("/groups/new");
  await page.getByLabel("Group name").fill("   ");
  await page.getByRole("button", { name: "Create a Group" }).click();

  await expect(page.getByText("Enter a name of 1 to 100 characters.")).toBeVisible();
  expect((await api(ana, "GET", "/groups")).json).toEqual([]);
});

test("AC-002-05 after signing in again the Group selected last is current", async ({ page }) => {
  const ana = await user(ANA, "Ana Kovač");
  await createGroup(ana, "Konferenca 2025");
  const alfa = await createGroup(ana, "Podjetje Alfa");
  await signIn(page, ana);
  await selectGroup(page, "Podjetje Alfa");
  await expect(page).toHaveURL((url) => url.pathname === listPath(alfa));

  await signOut(page, "Ana Kovač");
  await signIn(page, ana);

  await expect(page).toHaveURL((url) => url.pathname === listPath(alfa));
  await expect(page.getByLabel("Group switcher")).toContainText("Podjetje Alfa");
});

test("EX-002-06 the Group selected last is in the recycle bin: first Group, else no Groups", async ({
  page,
}) => {
  const ana = await user(ANA, "Ana Kovač");
  const konferenca = await createGroup(ana, "Konferenca 2025");
  const alfa = await createGroup(ana, "Podjetje Alfa");
  await signIn(page, ana);
  await selectGroup(page, "Podjetje Alfa");
  await expect(page).toHaveURL((url) => url.pathname === listPath(alfa));
  await signOut(page, "Ana Kovač");
  expect((await api(ana, "POST", `/groups/${alfa}/move-to-recycle-bin`)).status).toBe(204);

  await signIn(page, ana);
  await expect(page).toHaveURL((url) => url.pathname === listPath(konferenca));

  await signOut(page, "Ana Kovač");
  expect((await api(ana, "POST", `/groups/${konferenca}/move-to-recycle-bin`)).status).toBe(204);
  await signIn(page, ana);
  await expect(page.getByText("You have no Groups yet.")).toBeVisible();
});
