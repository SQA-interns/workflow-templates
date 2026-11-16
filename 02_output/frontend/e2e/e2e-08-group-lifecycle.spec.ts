// E2E-08 Group lifecycle (e2e-scenarios.md).
import { expect, test } from "@playwright/test";
import {
  BASE_URL,
  confirmDialog,
  createContact,
  listPath,
  resetStack,
  signIn,
  team,
} from "../acceptance/support/stack";

test.beforeEach(resetStack);

test("E2E-08 Group lifecycle", async ({ page, browser }) => {
  const t = await team();
  await createContact(t.ana, t.groupId, { fields: { firstName: "Janez", lastName: "Novak" } });
  const markoContext = await browser.newContext({ baseURL: BASE_URL });
  const markoPage = await markoContext.newPage();
  await signIn(markoPage, t.marko);
  await expect(markoPage.getByLabel("Group switcher")).toContainText("Konferenca 2025");

  // Ana renames "Konferenca 2025" to "Konferenca 2026".
  await signIn(page, t.ana);
  await page.goto(`/groups/${t.groupId}/settings`);
  await page.getByLabel("Group name").fill("Konferenca 2026");
  await page.getByRole("button", { name: "Save" }).click();
  await expect(page.getByLabel("Group switcher")).toContainText("Konferenca 2026");

  // She moves it to the recycle bin: Marko no longer sees it.
  await page.getByRole("button", { name: "Move Group to recycle bin" }).click();
  await confirmDialog(page, "Move Group to recycle bin");
  await expect(page.getByText("You have no Groups yet.")).toBeVisible();
  await markoPage.reload();
  await expect(markoPage.getByText("You have no Groups yet.")).toBeVisible();

  // Ana restores it from "Deleted Groups": Marko sees "Konferenca 2026" with its Contacts.
  await page.goto("/deleted-groups");
  await page
    .getByRole("row", { name: /Konferenca 2026/ })
    .getByRole("button", { name: "Restore" })
    .click();
  await expect(page.getByLabel("Group switcher")).toContainText("Konferenca 2026");
  await markoPage.goto(listPath(t.groupId));
  await expect(markoPage.getByLabel("Group switcher")).toContainText("Konferenca 2026");
  await expect(markoPage.getByRole("link", { name: "Janez Novak" })).toBeVisible();
  await markoContext.close();
});
