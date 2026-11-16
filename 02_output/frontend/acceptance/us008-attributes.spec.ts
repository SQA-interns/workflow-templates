// US-008 Attributes of a Contact: WF-08 (Attributes), WF-09 (API part in the backend).
import { expect, test } from "@playwright/test";
import { createContact, editContact, getContact, resetStack, signIn, team } from "./support/stack";

test.beforeEach(resetStack);

type Team = Awaited<ReturnType<typeof team>>;

async function janez(t: Team, attributes: { key: string; value: string }[] = []): Promise<string> {
  return createContact(t.ana, t.groupId, {
    fields: { firstName: "Janez", lastName: "Novak" },
    attributes,
  });
}

function history(t: Team, id: string): string {
  return `/groups/${t.groupId}/contacts/${id}/history`;
}

test("AC-008-01 an added Attribute is shown; Version 2 lists kompetenca: (empty) → value", async ({
  page,
}) => {
  const t = await team();
  const id = await janez(t);
  await signIn(page, t.marko);

  await page.goto(`/groups/${t.groupId}/contacts/${id}/edit`);
  await page.getByRole("button", { name: "Add an Attribute" }).click();
  await page.getByLabel("Key").last().fill("kompetenca");
  await page.getByLabel("Value").last().fill("spletnoProgramiranje");
  await page.getByRole("button", { name: "Save" }).click();

  await expect.poll(async () => (await getContact(t.ana, t.groupId, id)).versionNumber).toBe(2);
  await page.goto(`/groups/${t.groupId}/contacts/${id}`);
  await expect(page.getByText("spletnoProgramiranje")).toBeVisible();
  await page.goto(history(t, id));
  await expect(page.getByText("kompetenca: (empty) → spletnoProgramiranje")).toBeVisible();
});

test("AC-008-02 a changed value: kompetenca: old → new", async ({ page }) => {
  const t = await team();
  const id = await janez(t, [{ key: "kompetenca", value: "spletnoProgramiranje" }]);
  await editContact(
    t.marko,
    t.groupId,
    id,
    (u) => (u.attributes = [{ key: "kompetenca", value: "mobilnoProgramiranje" }]),
  );
  await signIn(page, t.marko);

  await page.goto(history(t, id));

  await expect(
    page.getByText("kompetenca: spletnoProgramiranje → mobilnoProgramiranje"),
  ).toBeVisible();
});

test("AC-008-03 the same key twice: The key Kompetenca is used twice.", async ({ page }) => {
  const t = await team();
  const id = await janez(t, [{ key: "kompetenca", value: "mobilnoProgramiranje" }]);
  const before = await getContact(t.ana, t.groupId, id);
  await signIn(page, t.marko);

  await page.goto(`/groups/${t.groupId}/contacts/${id}/edit`);
  await page.getByRole("button", { name: "Add an Attribute" }).click();
  await page.getByLabel("Key").last().fill("Kompetenca");
  await page.getByLabel("Value").last().fill("java");
  await page.getByRole("button", { name: "Save" }).click();

  await expect(page.getByText("The key Kompetenca is used twice.")).toBeVisible();
  expect(await getContact(t.ana, t.groupId, id)).toEqual(before);
});

test("AC-008-04 a removed Attribute: kompetenca: value → (removed)", async ({ page }) => {
  const t = await team();
  const id = await janez(t, [{ key: "kompetenca", value: "mobilnoProgramiranje" }]);
  await signIn(page, t.marko);

  await page.goto(`/groups/${t.groupId}/contacts/${id}/edit`);
  await page.getByRole("button", { name: "Remove", exact: true }).click();
  await page.getByRole("button", { name: "Save" }).click();

  await expect.poll(async () => (await getContact(t.ana, t.groupId, id)).attributes.length).toBe(0);
  await page.goto(history(t, id));
  await expect(page.getByText("kompetenca: mobilnoProgramiranje → (removed)")).toBeVisible();
});
