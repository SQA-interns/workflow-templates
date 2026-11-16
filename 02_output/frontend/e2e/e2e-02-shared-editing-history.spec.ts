// E2E-02 Shared editing with History and restore (e2e-scenarios.md).
import { expect, test } from "@playwright/test";
import {
  ANA,
  BASE_URL,
  MARKO,
  choose,
  confirmDialog,
  createContact,
  createGroup,
  getContact,
  resetStack,
  signIn,
  user,
} from "../acceptance/support/stack";

test.beforeEach(resetStack);

test("E2E-02 shared editing with History and restore", async ({ page, browser }) => {
  const ana = await user(ANA, "Ana Kovač");
  const marko = await user(MARKO, "Marko Kranjc");
  const groupId = await createGroup(ana, "Konferenca 2025");
  const janez = await createContact(ana, groupId, {
    fields: { firstName: "Janez", lastName: "Novak", email: "janez.novak@example.si" },
  });

  // Ana invites Marko as Editor and copies the link.
  await page
    .context()
    .grantPermissions(["clipboard-read", "clipboard-write"])
    .catch(() => undefined);
  await signIn(page, ana);
  await page.goto(`/groups/${groupId}/members`);
  await page.getByLabel("E-mail").fill(MARKO);
  await choose(page.getByLabel("Role"), "Editor");
  await page.getByRole("button", { name: "Create Invitation" }).click();
  const linkText = await page
    .getByText(/\/invitations\/[A-Za-z0-9_-]{22,128}/)
    .first()
    .textContent();
  await page.getByRole("button", { name: "Copy link" }).click();
  const link = /\/invitations\/[A-Za-z0-9_-]{22,128}/.exec(linkText ?? "")?.[0] ?? "";
  expect(link).not.toBe("");

  // Marko signs in, opens the link and accepts.
  const markoContext = await browser.newContext({ baseURL: BASE_URL });
  const markoPage = await markoContext.newPage();
  await signIn(markoPage, marko);
  await markoPage.goto(link);
  await markoPage.getByRole("button", { name: "Accept" }).click();
  await expect(markoPage.getByLabel("Group switcher")).toContainText("Konferenca 2025");

  // Marko changes Janez's e-mail.
  await markoPage.goto(`/groups/${groupId}/contacts/${janez}/edit`);
  await markoPage.getByLabel("E-mail").fill("janez@novak.si");
  await markoPage.getByRole("button", { name: "Save" }).click();
  await expect.poll(async () => (await getContact(ana, groupId, janez)).versionNumber).toBe(2);

  // History shows Version 2 by Marko Kranjc with the change.
  await page.goto(`/groups/${groupId}/contacts/${janez}/history`);
  const history = page.getByRole("main");
  await expect(history.getByText("Version 2", { exact: true })).toBeVisible();
  await expect(history.getByText("Marko Kranjc")).toBeVisible();
  await expect(history.getByText("E-mail: janez.novak@example.si → janez@novak.si")).toBeVisible();

  // Ana restores Version 1: the e-mail is back and Version 3 is restored.
  await page.goto(`/groups/${groupId}/contacts/${janez}/history/1`);
  await page.getByRole("button", { name: "Restore this Version" }).click();
  await confirmDialog(page, "Restore this Version");
  await expect.poll(async () => (await getContact(ana, groupId, janez)).versionNumber).toBe(3);
  await page.goto(`/groups/${groupId}/contacts/${janez}`);
  await expect(
    page.getByRole("main").getByText("janez.novak@example.si", { exact: true }),
  ).toBeVisible();
  await page.goto(`/groups/${groupId}/contacts/${janez}/history`);
  await expect(page.getByRole("main").getByText("Version 3", { exact: true })).toBeVisible();
  await expect(
    page
      .getByRole("main")
      .getByText(/restored/)
      .first(),
  ).toBeVisible();
  await markoContext.close();
});
