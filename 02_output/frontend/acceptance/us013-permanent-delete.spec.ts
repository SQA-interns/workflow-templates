// US-013 Permanently delete a Contact: WF-11, WF-20 (API part in the backend).
import { expect, test } from "@playwright/test";
import {
  api,
  cancelDialog,
  confirmDialog,
  createContact,
  moveContactToRecycleBin,
  resetStack,
  signIn,
  team,
} from "./support/stack";

test.beforeEach(resetStack);

type Team = Awaited<ReturnType<typeof team>>;

async function janezInRecycleBin(t: Team): Promise<string> {
  const id = await createContact(t.ana, t.groupId, {
    fields: { firstName: "Janez", lastName: "Novak" },
  });
  await moveContactToRecycleBin(t.marko, t.groupId, id);
  return id;
}

test("AC-013-01 Delete permanently: gone from the recycle bin, its address is not found", async ({
  page,
}) => {
  const t = await team();
  const id = await janezInRecycleBin(t);
  await signIn(page, t.ana);

  await page.goto(`/groups/${t.groupId}/recycle-bin`);
  await page
    .getByRole("row", { name: /Janez Novak/ })
    .getByRole("button", { name: "Delete permanently" })
    .click();
  await confirmDialog(page, "Delete permanently");

  await expect(page.getByText("The recycle bin is empty.")).toBeVisible();
  expect((await api(t.ana, "GET", `/groups/${t.groupId}/contacts/${id}`)).status).toBe(404);
  await page.goto(`/groups/${t.groupId}/contacts/${id}`);
  await expect(page.getByText("This page cannot be found.")).toBeVisible();
});

test("EX-013-01 cancelling Delete permanently changes nothing", async ({ page }) => {
  const t = await team();
  await janezInRecycleBin(t);
  await signIn(page, t.ana);

  await page.goto(`/groups/${t.groupId}/recycle-bin`);
  await page
    .getByRole("row", { name: /Janez Novak/ })
    .getByRole("button", { name: "Delete permanently" })
    .click();
  await cancelDialog(page);

  await expect(page.getByRole("row", { name: /Janez Novak/ })).toBeVisible();
  const bin = await api(t.ana, "GET", `/groups/${t.groupId}/recycle-bin`);
  expect(bin.json.map((e: { contact: { name: string } }) => e.contact.name)).toEqual([
    "Janez Novak",
  ]);
});
