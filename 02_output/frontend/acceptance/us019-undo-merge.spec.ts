// US-019 Undo a Merge: WF-07 (API part in the backend).
import { expect, test } from "@playwright/test";
import {
  api,
  cancelDialog,
  confirmDialog,
  createContact,
  editContact,
  getContact,
  listPath,
  resetStack,
  signIn,
  team,
} from "./support/stack";

test.beforeEach(resetStack);

type Team = Awaited<ReturnType<typeof team>>;

/** "J. Novak" merged into "Janez Novak" (phone of J. Novak kept), as AC-019-01. */
async function merged(t: Team): Promise<[string, string]> {
  const janez = await createContact(t.ana, t.groupId, {
    fields: { firstName: "Janez", lastName: "Novak", email: "janez.novak@example.si" },
    tags: ["udelezenec2025"],
  });
  const other = await createContact(t.ana, t.groupId, {
    fields: {
      firstName: "J.",
      lastName: "Novak",
      email: "janez.novak@example.si",
      phone: "+386 41 123 456",
    },
    tags: ["vip"],
  });
  const res = await api(t.marko, "POST", `/groups/${t.groupId}/merges`, {
    primaryId: janez,
    otherId: other,
    primaryVersionNumber: 1,
    otherVersionNumber: 1,
    fieldChoices: { phone: "other" },
  });
  expect(res.status, JSON.stringify(res.json)).toBe(201);
  return [janez, other];
}

test("AC-019-01 Undo merge: J. Novak is in the list again", async ({ page }) => {
  const t = await team();
  const [janez, other] = await merged(t);
  await signIn(page, t.marko);

  await page.goto(`/groups/${t.groupId}/contacts/${janez}`);
  await page.getByRole("button", { name: "Undo merge" }).click();
  await confirmDialog(page, "Undo merge");

  await expect.poll(async () => (await getContact(t.ana, t.groupId, other)).state).toBe("active");
  await page.goto(listPath(t.groupId));
  await expect(page.getByRole("link", { name: "J. Novak" })).toBeVisible();
  expect(
    (await getContact(t.ana, t.groupId, janez)).tags.map((x: { name: string }) => x.name),
  ).toEqual(["udelezenec2025"]);
});

test("AC-019-02 after a later change: The Contact changed after the Merge. Restore the Versions you need instead.", async ({
  page,
}) => {
  const t = await team();
  const [janez] = await merged(t);
  await editContact(t.ana, t.groupId, janez, (u) => (u.fields.jobTitle = "CEO"));
  const before = await getContact(t.ana, t.groupId, janez);
  await signIn(page, t.marko);

  await page.goto(`/groups/${t.groupId}/contacts/${janez}`);
  await page.getByRole("button", { name: "Undo merge" }).click();
  await confirmDialog(page, "Undo merge");

  await expect(
    page.getByText("The Contact changed after the Merge. Restore the Versions you need instead."),
  ).toBeVisible();
  expect(await getContact(t.ana, t.groupId, janez)).toEqual(before);
});

test("EX-019-03 cancelling Undo merge changes nothing", async ({ page }) => {
  const t = await team();
  const [janez, other] = await merged(t);
  await signIn(page, t.marko);

  await page.goto(`/groups/${t.groupId}/contacts/${janez}`);
  await page.getByRole("button", { name: "Undo merge" }).click();
  await cancelDialog(page);

  expect((await getContact(t.ana, t.groupId, other)).state).toBe("merged");
  expect((await getContact(t.ana, t.groupId, janez)).versionNumber).toBe(2);
});
