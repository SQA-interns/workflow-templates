// US-012 Recycle bin for Contacts: WF-07 (Move to recycle bin), WF-11 (API part in the backend).
import { expect, test } from "@playwright/test";
import {
  api,
  cancelDialog,
  confirmDialog,
  createContact,
  getContact,
  listPath,
  moveContactToRecycleBin,
  resetStack,
  signIn,
  stubGet,
  team,
} from "./support/stack";

test.beforeEach(resetStack);

type Team = Awaited<ReturnType<typeof team>>;

async function janez(t: Team): Promise<string> {
  return createContact(t.ana, t.groupId, {
    fields: { firstName: "Janez", lastName: "Novak" },
    tags: ["udelezenec2025"],
    attributes: [{ key: "kompetenca", value: "spletnoProgramiranje" }],
  });
}

test("AC-012-01 Move to recycle bin: the Contact is no longer in the list", async ({ page }) => {
  const t = await team();
  const id = await janez(t);
  await signIn(page, t.marko);

  await page.goto(`/groups/${t.groupId}/contacts/${id}`);
  await page.getByRole("button", { name: "Move to recycle bin" }).click();
  await confirmDialog(page, "Move to recycle bin");

  await expect
    .poll(async () => (await api(t.marko, "GET", `/groups/${t.groupId}/recycle-bin`)).json.length)
    .toBe(1);
  await page.goto(listPath(t.groupId));
  await expect(page.getByText("No Contacts yet.")).toBeVisible();
  await expect(page.getByRole("link", { name: "Janez Novak" })).toHaveCount(0);
});

test("AC-012-01 the recycle bin shows deleted by, deleted at and removal date", async ({
  page,
}) => {
  const t = await team();
  const id = await janez(t);
  await stubGet(page, `/groups/${t.groupId}/recycle-bin`, [
    {
      contact: { id, name: "Janez Novak" },
      versionNumber: 4,
      deletedBy: "Marko Kranjc",
      deletedAt: "2026-03-05T10:00:00Z",
      removalAt: "2026-04-04T10:00:00Z",
    },
  ]);
  await signIn(page, t.marko);

  await page.goto(`/groups/${t.groupId}/recycle-bin`);

  const row = page.getByRole("row", { name: /Janez Novak/ });
  await expect(row).toContainText("Marko Kranjc");
  await expect(row).toContainText("05.03.2026 11:00");
  await expect(row).toContainText("04.04.2026 12:00");
});

test("AC-012-02 Restore from the recycle bin: back in the list with Tags", async ({ page }) => {
  const t = await team();
  const id = await janez(t);
  await moveContactToRecycleBin(t.marko, t.groupId, id);
  await signIn(page, t.ana);

  await page.goto(`/groups/${t.groupId}/recycle-bin`);
  await page
    .getByRole("row", { name: /Janez Novak/ })
    .getByRole("button", { name: "Restore" })
    .click();

  await expect.poll(async () => (await getContact(t.ana, t.groupId, id)).state).toBe("active");
  await page.goto(listPath(t.groupId));
  const row = page.getByRole("row", { name: /Janez Novak/ });
  await expect(row).toContainText("udelezenec2025");
  expect((await getContact(t.ana, t.groupId, id)).attributes[0].value).toBe("spletnoProgramiranje");
});

test("EX-012-03 cancelling Move to recycle bin changes nothing", async ({ page }) => {
  const t = await team();
  const id = await janez(t);
  await signIn(page, t.marko);

  await page.goto(`/groups/${t.groupId}/contacts/${id}`);
  await page.getByRole("button", { name: "Move to recycle bin" }).click();
  await cancelDialog(page);

  const contact = await getContact(t.ana, t.groupId, id);
  expect(contact.state).toBe("active");
  expect(contact.versionNumber).toBe(1);
  expect((await api(t.ana, "GET", `/groups/${t.groupId}/recycle-bin`)).json).toEqual([]);
});
