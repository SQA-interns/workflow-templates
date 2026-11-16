// E2E-05 Recycle bin round trip (e2e-scenarios.md).
import { expect, test } from "@playwright/test";
import {
  api,
  confirmDialog,
  createContact,
  listPath,
  ljubljana,
  resetStack,
  signIn,
  team,
} from "../acceptance/support/stack";

test.beforeEach(resetStack);

test("E2E-05 recycle bin round trip", async ({ page }) => {
  const t = await team();
  const janez = await createContact(t.ana, t.groupId, {
    fields: { firstName: "Janez", lastName: "Novak" },
    tags: ["udelezenec2025"],
    attributes: [{ key: "kompetenca", value: "spletnoProgramiranje" }],
  });

  // Marko moves "Janez Novak" to the recycle bin: gone from the list.
  await signIn(page, t.marko);
  await page.goto(`/groups/${t.groupId}/contacts/${janez}`);
  await page.getByRole("button", { name: "Move to recycle bin" }).click();
  await confirmDialog(page, "Move to recycle bin");
  await expect
    .poll(async () => (await api(t.marko, "GET", `/groups/${t.groupId}/recycle-bin`)).json.length)
    .toBe(1);
  await page.goto(listPath(t.groupId));
  await expect(page.getByRole("link", { name: "Janez Novak" })).toHaveCount(0);

  // The recycle bin shows it with its removal date.
  const entry = (await api(t.marko, "GET", `/groups/${t.groupId}/recycle-bin`)).json[0];
  await page.goto(`/groups/${t.groupId}/recycle-bin`);
  const row = page.getByRole("row", { name: /Janez Novak/ });
  await expect(row).toContainText(ljubljana(entry.removalAt));

  // Marko restores it: in the list with its Tags and Attributes.
  await row.getByRole("button", { name: "Restore" }).click();
  await expect
    .poll(async () => (await api(t.marko, "GET", `/groups/${t.groupId}/recycle-bin`)).json.length)
    .toBe(0);
  await page.goto(listPath(t.groupId));
  await expect(page.getByRole("row", { name: /Janez Novak/ })).toContainText("udelezenec2025");
  await page.getByRole("link", { name: "Janez Novak" }).click();
  await expect(page.getByRole("main").getByText("spletnoProgramiranje")).toBeVisible();
});
