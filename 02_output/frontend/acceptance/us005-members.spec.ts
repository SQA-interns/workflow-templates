// US-005 Manage Members and Roles: WF-13 (API part in the backend).
import { expect, test } from "@playwright/test";
import {
  ANA,
  MARKO,
  PETRA,
  api,
  cancelDialog,
  expectSelected,
  resetStack,
  signIn,
  team,
} from "./support/stack";

test.beforeEach(resetStack);

test("AC-005-01 the Members screen lists every Member with e-mail and Role", async ({ page }) => {
  const t = await team();
  await signIn(page, t.ana);

  await page.goto(`/groups/${t.groupId}/members`);

  const ana = page.getByRole("row", { name: /Ana Kovač/ });
  await expect(ana).toContainText(ANA);
  await expect(ana).toContainText("Owner");
  const marko = page.getByRole("row", { name: /Marko Kranjc/ });
  await expect(marko).toContainText(MARKO);
  await expectSelected(marko.getByRole("combobox"), "Editor");
  const petra = page.getByRole("row", { name: /Petra Horvat/ });
  await expect(petra).toContainText(PETRA);
  await expectSelected(petra.getByRole("combobox"), "Viewer");
});

test("EX-005-04 cancelling Remove from Group changes nothing", async ({ page }) => {
  const t = await team();
  await signIn(page, t.ana);

  await page.goto(`/groups/${t.groupId}/members`);
  await page
    .getByRole("row", { name: /Marko Kranjc/ })
    .getByRole("button", { name: "Remove from Group" })
    .click();
  await cancelDialog(page);

  await expect(page.getByRole("row", { name: /Marko Kranjc/ })).toBeVisible();
  const members = await api(t.ana, "GET", `/groups/${t.groupId}/members`);
  expect(members.json.map((m: { email: string }) => m.email)).toContain(MARKO);
});
