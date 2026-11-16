// US-007 Edit a Contact: WF-08, WF-09 (API part in the backend).
import { expect, test } from "@playwright/test";
import {
  api,
  createContact,
  editContact,
  getContact,
  resetStack,
  signIn,
  team,
} from "./support/stack";

test.beforeEach(resetStack);

async function janez(t: Awaited<ReturnType<typeof team>>): Promise<string> {
  return createContact(t.ana, t.groupId, {
    fields: { firstName: "Janez", lastName: "Novak", email: "janez.novak@example.si" },
  });
}

test("AC-007-01 the changed e-mail is shown; History lists E-mail: old → new", async ({ page }) => {
  const t = await team();
  const id = await janez(t);
  await signIn(page, t.marko);

  await page.goto(`/groups/${t.groupId}/contacts/${id}/edit`);
  await page.getByLabel("E-mail").fill("janez@novak.si");
  await page.getByRole("button", { name: "Save" }).click();

  await expect.poll(async () => (await getContact(t.ana, t.groupId, id)).versionNumber).toBe(2);
  await page.goto(`/groups/${t.groupId}/contacts/${id}`);
  await expect(page.getByText("janez@novak.si")).toBeVisible();
  await page.goto(`/groups/${t.groupId}/contacts/${id}/history`);
  await expect(page.getByText("E-mail: janez.novak@example.si → janez@novak.si")).toBeVisible();
  await expect(page.getByRole("main").getByText("Marko Kranjc")).toBeVisible();
});

test("AC-007-02 a save on a stale Version: Contact was changed by Marko Kranjc, reload", async ({
  page,
}) => {
  const t = await team();
  const id = await janez(t);
  await editContact(t.ana, t.groupId, id, (u) => (u.fields.phone = "+386 41 123 456"));
  await signIn(page, t.ana);
  await page.goto(`/groups/${t.groupId}/contacts/${id}/edit`);
  await expect(page.getByLabel("Phone")).toHaveValue("+386 41 123 456");

  await editContact(t.marko, t.groupId, id, (u) => (u.fields.city = "Maribor"));
  await page.getByLabel("Job title").fill("CEO");
  await page.getByRole("button", { name: "Save" }).click();

  await expect(page.getByText(/Contact was changed by Marko Kranjc, reload/)).toBeVisible();
  const now = await getContact(t.ana, t.groupId, id);
  expect(now.versionNumber).toBe(3);
  expect(now.fields.jobTitle ?? "").toBe("");
});

test("AC-007-03 clearing every name part: Enter a first name, last name or company.", async ({
  page,
}) => {
  const t = await team();
  const id = await janez(t);
  const before = await getContact(t.ana, t.groupId, id);
  await signIn(page, t.marko);

  await page.goto(`/groups/${t.groupId}/contacts/${id}/edit`);
  await page.getByLabel("First name").fill("");
  await page.getByLabel("Last name").fill("");
  await page.getByRole("button", { name: "Save" }).click();

  await expect(page.getByText("Enter a first name, last name or company.")).toBeVisible();
  expect(await getContact(t.ana, t.groupId, id)).toEqual(before);
});

test("AC-007-04 saving without changes: No changes to save.", async ({ page }) => {
  const t = await team();
  const id = await janez(t);
  await editContact(t.marko, t.groupId, id, (u) => (u.fields.city = "Maribor"));
  await signIn(page, t.marko);

  await page.goto(`/groups/${t.groupId}/contacts/${id}/edit`);
  await expect(page.getByLabel("City")).toHaveValue("Maribor");
  await page.getByRole("button", { name: "Save" }).click();

  await expect(page.getByText("No changes to save.")).toBeVisible();
  const history = await api(t.ana, "GET", `/groups/${t.groupId}/contacts/${id}/versions`);
  expect(history.json[0].number).toBe(2);
});
