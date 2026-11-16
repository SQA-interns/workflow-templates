// E2E-04 Duplicates: find, merge, undo (increment 2) (e2e-scenarios.md).
import { expect, test } from "@playwright/test";
import {
  choose,
  confirmDialog,
  createContact,
  getContact,
  listPath,
  resetStack,
  signIn,
  team,
} from "../acceptance/support/stack";

test.beforeEach(resetStack);

test("E2E-04 duplicates: find, merge, undo", async ({ page }) => {
  const t = await team();
  const janez = await createContact(t.ana, t.groupId, {
    fields: { firstName: "Janez", lastName: "Novak", email: "janez.novak@example.si" },
  });
  const other = await createContact(t.ana, t.groupId, {
    fields: { firstName: "J.", lastName: "Novak", email: "janez.novak@example.si" },
  });

  // "Possible duplicates" lists the pair with "Same e-mail".
  await signIn(page, t.marko);
  await page.goto(`/groups/${t.groupId}/duplicates`);
  const main = page.getByRole("main");
  await expect(main.getByRole("link", { name: "Janez Novak" })).toBeVisible();
  await expect(main.getByRole("link", { name: "J. Novak" })).toBeVisible();
  await expect(main.getByText("Same e-mail")).toBeVisible();

  // Marko merges with "Janez Novak" as Primary contact.
  await page.getByRole("button", { name: "Merge" }).click();
  await expect(page.getByRole("heading", { name: "Merge", level: 1 })).toBeVisible();
  const asPrimary = page.getByRole("radio", { name: "Janez Novak" });
  if ((await asPrimary.count()) > 0) {
    await asPrimary.check();
  } else {
    await choose(page.getByLabel("Primary contact"), "Janez Novak");
  }
  await page.getByRole("button", { name: "Merge", exact: true }).click();
  await expect.poll(async () => (await getContact(t.ana, t.groupId, other)).state).toBe("merged");

  // "J. Novak" is gone from the list; "Janez Novak" shows "Merged from J. Novak".
  await page.goto(listPath(t.groupId));
  await expect(page.getByRole("link", { name: "Janez Novak" })).toBeVisible();
  await expect(page.getByRole("link", { name: "J. Novak" })).toHaveCount(0);
  await page.goto(`/groups/${t.groupId}/contacts/${janez}`);
  await expect(page.getByText(/Merged from/)).toBeVisible();
  await expect(page.getByRole("link", { name: "J. Novak" })).toBeVisible();

  // Undo merge: both Contacts are in the list again.
  await page.getByRole("button", { name: "Undo merge" }).click();
  await confirmDialog(page, "Undo merge");
  await expect.poll(async () => (await getContact(t.ana, t.groupId, other)).state).toBe("active");
  await page.goto(listPath(t.groupId));
  await expect(page.getByRole("link", { name: "Janez Novak" })).toBeVisible();
  await expect(page.getByRole("link", { name: "J. Novak" })).toBeVisible();
});
