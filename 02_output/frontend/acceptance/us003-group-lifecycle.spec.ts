// US-003 Rename, move to recycle bin and restore a Group: WF-14, WF-15, WF-16 (API part in the backend).
import { expect, test } from "@playwright/test";
import {
  api,
  cancelDialog,
  confirmDialog,
  invite,
  resetStack,
  signIn,
  stubGet,
  team,
  user,
} from "./support/stack";

test.beforeEach(resetStack);

test("AC-003-01 the Owner renames the Group; the switcher shows the new name", async ({ page }) => {
  const t = await team();
  await signIn(page, t.ana);

  await page.goto(`/groups/${t.groupId}/settings`);
  await page.getByLabel("Group name").fill("Konferenca 2026");
  await page.getByRole("button", { name: "Save" }).click();

  await expect(page.getByLabel("Group switcher")).toContainText("Konferenca 2026");
});

test("AC-003-03 Move Group to recycle bin: gone from the switcher", async ({ page }) => {
  const t = await team();
  await signIn(page, t.ana);

  await page.goto(`/groups/${t.groupId}/settings`);
  await page.getByRole("button", { name: "Move Group to recycle bin" }).click();
  await confirmDialog(page, "Move Group to recycle bin");

  await expect(page.getByText("You have no Groups yet.")).toBeVisible();
  expect((await api(t.marko, "GET", "/groups")).json).toEqual([]);
});

test("AC-003-03 Deleted Groups shows the removal date 04.04.2026 12:00 (DST in between)", async ({
  page,
}) => {
  const t = await team();
  await stubGet(page, "/api/v1/deleted-groups", [
    {
      id: t.groupId,
      name: "Konferenca 2025",
      deletedAt: "2026-03-05T10:00:00Z",
      removalAt: "2026-04-04T10:00:00Z",
    },
  ]);
  await signIn(page, t.ana);

  await page.goto("/deleted-groups");

  const row = page.getByRole("row", { name: /Konferenca 2025/ });
  await expect(row).toContainText("04.04.2026 12:00");
});

test("AC-003-04 the Owner restores the Group from Deleted Groups", async ({ page }) => {
  const t = await team();
  expect((await api(t.ana, "POST", `/groups/${t.groupId}/move-to-recycle-bin`)).status).toBe(204);
  await signIn(page, t.ana);

  await page.goto("/deleted-groups");
  await page
    .getByRole("row", { name: /Konferenca 2025/ })
    .getByRole("button", { name: "Restore" })
    .click();

  await expect(page.getByLabel("Group switcher")).toContainText("Konferenca 2025");
  expect((await api(t.marko, "GET", "/groups")).json.map((g: { name: string }) => g.name)).toEqual([
    "Konferenca 2025",
  ]);
});

test("EX-003-04 cancelling Move Group to recycle bin changes nothing", async ({ page }) => {
  const t = await team();
  await signIn(page, t.ana);

  await page.goto(`/groups/${t.groupId}/settings`);
  await page.getByRole("button", { name: "Move Group to recycle bin" }).click();
  await cancelDialog(page);

  await expect(page.getByLabel("Group switcher")).toContainText("Konferenca 2025");
  expect((await api(t.ana, "GET", `/groups/${t.groupId}`)).status).toBe(200);
});

test("EX-003-05 an Invitation of a Group in the recycle bin: This Invitation is no longer valid.", async ({
  page,
}) => {
  const t = await team();
  const token = await invite(t.groupId, t.ana, "nina.kos@example.si", "EDITOR");
  expect((await api(t.ana, "POST", `/groups/${t.groupId}/move-to-recycle-bin`)).status).toBe(204);
  const nina = await user("nina.kos@example.si", "Nina Kos");
  await signIn(page, nina);

  await page.goto(`/invitations/${token}`);

  await expect(page.getByText("This Invitation is no longer valid.")).toBeVisible();
  expect((await api(nina, "GET", "/groups")).json).toEqual([]);
});
