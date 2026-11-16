// E2E-07 Permanent delete (e2e-scenarios.md).
import { expect, test } from "@playwright/test";
import {
  api,
  confirmDialog,
  createContact,
  listPath,
  resetStack,
  signIn,
  team,
} from "../acceptance/support/stack";

test.beforeEach(resetStack);

test("E2E-07 permanent delete", async ({ page }) => {
  const t = await team();
  const maja = await createContact(t.ana, t.groupId, {
    fields: { firstName: "Maja", lastName: "Zupan" },
  });

  // Ana moves "Maja Zupan" to the recycle bin.
  await signIn(page, t.ana);
  await page.goto(`/groups/${t.groupId}/contacts/${maja}`);
  await page.getByRole("button", { name: "Move to recycle bin" }).click();
  await confirmDialog(page, "Move to recycle bin");
  await expect
    .poll(async () => (await api(t.ana, "GET", `/groups/${t.groupId}/recycle-bin`)).json.length)
    .toBe(1);

  // She chooses "Delete permanently" and confirms.
  await page.goto(`/groups/${t.groupId}/recycle-bin`);
  await page
    .getByRole("row", { name: /Maja Zupan/ })
    .getByRole("button", { name: "Delete permanently" })
    .click();
  await confirmDialog(page, "Delete permanently");

  // "Maja Zupan" is in neither the list nor the recycle bin; her old URL shows "not found".
  await expect(page.getByText("The recycle bin is empty.")).toBeVisible();
  await page.goto(listPath(t.groupId));
  await expect(page.getByRole("link", { name: "Maja Zupan" })).toHaveCount(0);
  await page.goto(`/groups/${t.groupId}/contacts/${maja}`);
  await expect(page.getByText("This page cannot be found.")).toBeVisible();
});
