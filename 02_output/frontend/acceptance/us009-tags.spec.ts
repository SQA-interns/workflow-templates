// US-009 Tags of a Contact: WF-08 (Tags), WF-09 (API part in the backend).
import { expect, test } from "@playwright/test";
import { api, createContact, getContact, resetStack, signIn, team } from "./support/stack";

test.beforeEach(resetStack);

type Team = Awaited<ReturnType<typeof team>>;

async function person(
  t: Team,
  firstName: string,
  lastName: string,
  tags: string[] = [],
): Promise<string> {
  return createContact(t.ana, t.groupId, { fields: { firstName, lastName }, tags });
}

function editPath(t: Team, id: string): string {
  return `/groups/${t.groupId}/contacts/${id}/edit`;
}

async function addTag(page: import("@playwright/test").Page, name: string): Promise<void> {
  await page.getByLabel("Add a Tag").fill(name);
  await page.getByLabel("Add a Tag").press("Enter");
}

test("AC-009-01 an added Tag is shown; Version 2 lists tag:udelezenec2025 added", async ({
  page,
}) => {
  const t = await team();
  const id = await person(t, "Janez", "Novak");
  await signIn(page, t.marko);

  await page.goto(editPath(t, id));
  await addTag(page, "udelezenec2025");
  await page.getByRole("button", { name: "Save" }).click();

  await expect.poll(async () => (await getContact(t.ana, t.groupId, id)).versionNumber).toBe(2);
  await page.goto(`/groups/${t.groupId}/contacts/${id}`);
  await expect(page.getByRole("main").getByText("udelezenec2025", { exact: true })).toBeVisible();
  await page.goto(`/groups/${t.groupId}/contacts/${id}/history`);
  await expect(page.getByText("tag:udelezenec2025 added")).toBeVisible();
});

test("AC-009-02 typing udel suggests udelezenec2025", async ({ page }) => {
  const t = await team();
  await person(t, "Janez", "Novak", ["udelezenec2025"]);
  const maja = await person(t, "Maja", "Zupan");
  await signIn(page, t.marko);

  await page.goto(editPath(t, maja));
  await page.getByLabel("Add a Tag").fill("udel");

  await expect(
    page
      .getByRole("option", { name: "udelezenec2025" })
      .or(page.getByText("udelezenec2025", { exact: true }))
      .first(),
  ).toBeVisible();
});

test("AC-009-03 a Tag in other case takes the existing spelling", async ({ page }) => {
  const t = await team();
  await person(t, "Janez", "Novak", ["udelezenec2025"]);
  const maja = await person(t, "Maja", "Zupan");
  await signIn(page, t.marko);

  await page.goto(editPath(t, maja));
  await addTag(page, "Udelezenec2025");
  await page.getByRole("button", { name: "Save" }).click();

  await expect.poll(async () => (await getContact(t.ana, t.groupId, maja)).versionNumber).toBe(2);
  await page.goto(`/groups/${t.groupId}/contacts/${maja}`);
  await expect(page.getByRole("main").getByText("udelezenec2025", { exact: true })).toBeVisible();
  const tags = await api(t.ana, "GET", `/groups/${t.groupId}/tags?q=udelezenec2025`);
  expect(tags.json.map((tag: { name: string }) => tag.name)).toEqual(["udelezenec2025"]);
});

test("AC-009-04 a removed Tag: tag:neObvescaj removed", async ({ page }) => {
  const t = await team();
  const id = await person(t, "Janez", "Novak", ["udelezenec2025", "neObvescaj"]);
  await signIn(page, t.marko);

  await page.goto(editPath(t, id));
  await page.getByRole("button", { name: /neObvescaj/ }).click();
  await page.getByRole("button", { name: "Save" }).click();

  await expect.poll(async () => (await getContact(t.ana, t.groupId, id)).tags.length).toBe(1);
  await page.goto(`/groups/${t.groupId}/contacts/${id}/history`);
  await expect(page.getByText("tag:neObvescaj removed")).toBeVisible();
});

test("AC-009-05 a Tag with a comma: A Tag must have 1 to 50 characters and no comma.", async ({
  page,
}) => {
  const t = await team();
  const id = await person(t, "Janez", "Novak");
  const before = await getContact(t.ana, t.groupId, id);
  await signIn(page, t.marko);

  await page.goto(editPath(t, id));
  await addTag(page, "vip, 2025");
  const message = page.getByText("A Tag must have 1 to 50 characters and no comma.").first();
  const shownAtOnce = await message
    .waitFor({ state: "visible", timeout: 3_000 })
    .then(() => true)
    .catch(() => false);
  if (!shownAtOnce) {
    await page.getByRole("button", { name: "Save" }).click();
  }

  await expect(message).toBeVisible();
  expect(await getContact(t.ana, t.groupId, id)).toEqual(before);
});
