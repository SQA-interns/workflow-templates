// US-018 Merge two Contacts: WF-17, WF-18, WF-07 (API part in the backend).
import { expect, test } from "@playwright/test";
import {
  api,
  createContact,
  editContact,
  getContact,
  resetStack,
  signIn,
  team,
} from "./support/stack";

test.beforeEach(resetStack);

type Team = Awaited<ReturnType<typeof team>>;

/** "Janez Novak" (Version 3, no phone, udelezenec2025) and "J. Novak" (phone, vip), same e-mail. */
async function pair(t: Team): Promise<[string, string]> {
  const janez = await createContact(t.ana, t.groupId, {
    fields: { firstName: "Janez", lastName: "Novak", email: "janez.novak@example.si" },
    tags: ["udelezenec2025"],
  });
  await editContact(t.marko, t.groupId, janez, (u) => (u.fields.city = "Ljubljana"));
  await editContact(t.marko, t.groupId, janez, (u) => (u.fields.city = "Maribor"));
  const other = await createContact(t.ana, t.groupId, {
    fields: {
      firstName: "J.",
      lastName: "Novak",
      email: "janez.novak@example.si",
      phone: "+386 41 123 456",
    },
    tags: ["vip"],
  });
  return [janez, other];
}

async function openMerge(page: import("@playwright/test").Page, t: Team): Promise<void> {
  await signIn(page, t.marko);
  await page.goto(`/groups/${t.groupId}/duplicates`);
  await page.getByRole("button", { name: "Merge" }).click();
  await expect(page.getByRole("heading", { name: "Merge", level: 1 })).toBeVisible();
}

test("AC-018-05 the Merge screen: Primary contact Janez Novak, phone of J. Novak preselected", async ({
  page,
}) => {
  const t = await team();
  await pair(t);

  await openMerge(page, t);

  await expect(page.getByRole("heading", { name: "Primary contact" })).toBeVisible();
  const text = await page.getByRole("main").innerText();
  const start = text.indexOf("Primary contact");
  expect(text.slice(start, start + 200)).toContain("Janez Novak");
  await expect(page.getByRole("radio", { name: "+386 41 123 456" })).toBeChecked();
});

test("AC-018-02 after the Merge the Primary contact shows Merged from J. Novak", async ({
  page,
}) => {
  const t = await team();
  const [janez, other] = await pair(t);

  await openMerge(page, t);
  await page.getByRole("button", { name: "Merge", exact: true }).click();

  await expect.poll(async () => (await getContact(t.ana, t.groupId, other)).state).toBe("merged");
  await page.goto(`/groups/${t.groupId}/contacts/${janez}`);
  await expect(page.getByText(/Merged from/)).toBeVisible();
  await page.getByRole("link", { name: "J. Novak" }).click();
  await expect(page).toHaveURL((url) => url.pathname.includes(`/contacts/${other}/history`));
  await expect(page.getByRole("main").getByText("Version 2", { exact: true })).toBeVisible();
  await expect(page.getByRole("main").getByText("Version 1", { exact: true })).toBeVisible();
});

test("AC-018-03 two values of kompetenca: Choose a value for kompetenca.", async ({ page }) => {
  const t = await team();
  const a = await createContact(t.ana, t.groupId, {
    fields: { firstName: "Janez", lastName: "Novak", email: "janez.novak@example.si" },
    attributes: [{ key: "kompetenca", value: "java" }],
  });
  const b = await createContact(t.ana, t.groupId, {
    fields: { firstName: "J.", lastName: "Novak", email: "janez.novak@example.si" },
    attributes: [{ key: "kompetenca", value: "python" }],
  });
  const before = [await getContact(t.ana, t.groupId, a), await getContact(t.ana, t.groupId, b)];

  await openMerge(page, t);
  await page.getByRole("button", { name: "Merge", exact: true }).click();

  await expect(page.getByText("Choose a value for kompetenca.")).toBeVisible();
  expect([await getContact(t.ana, t.groupId, a), await getContact(t.ana, t.groupId, b)]).toEqual(
    before,
  );
});

test("EX-018-06 Cancel on the Merge screen changes nothing", async ({ page }) => {
  const t = await team();
  const [janez, other] = await pair(t);
  const before = [
    await getContact(t.ana, t.groupId, janez),
    await getContact(t.ana, t.groupId, other),
  ];

  await openMerge(page, t);
  await page.getByRole("button", { name: "Cancel" }).click();

  expect([
    await getContact(t.ana, t.groupId, janez),
    await getContact(t.ana, t.groupId, other),
  ]).toEqual(before);
  expect((await api(t.ana, "GET", `/groups/${t.groupId}/contacts`)).json.total).toBe(2);
});
