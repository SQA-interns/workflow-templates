// US-020 Language: header, user menu, WF-06, WF-09 (API part in the backend).
import { expect, test } from "@playwright/test";
import {
  ANA,
  api,
  createContact,
  createGroup,
  listPath,
  openUserMenu,
  resetStack,
  signIn,
  signOut,
  stubGet,
  user,
} from "./support/stack";

test.beforeEach(resetStack);

test("AC-020-01 a new User sees the Contact list titled Kontakti", async ({ page }) => {
  const ana = await user(ANA, "Ana Kovač", "sl");
  const groupId = await createGroup(ana, "Konferenca 2025");
  await signIn(page, ana);

  await page.goto(listPath(groupId));

  await expect(page.getByRole("heading", { level: 1 })).toHaveText("Kontakti");
});

test("AC-020-02 choosing English switches at once and is kept after signing in again", async ({
  page,
}) => {
  const ana = await user(ANA, "Ana Kovač", "sl");
  const groupId = await createGroup(ana, "Konferenca 2025");
  await signIn(page, ana);
  await page.goto(listPath(groupId));
  await expect(page.getByRole("heading", { level: 1 })).toHaveText("Kontakti");
  let loads = 0;
  page.on("load", () => (loads += 1));

  await openUserMenu(page, "Ana Kovač");
  await page
    .getByRole("menuitemradio", { name: "English" })
    .or(page.getByRole("menuitem", { name: "English" }))
    .or(page.getByRole("button", { name: "English" }))
    .or(page.getByRole("link", { name: "English" }))
    .first()
    .click();

  await expect(page.getByRole("heading", { level: 1 })).toHaveText("Contacts");
  expect(loads).toBe(0);
  await signOut(page, "Ana Kovač");
  await signIn(page, ana);
  await page.goto(listPath(groupId));
  await expect(page.getByRole("heading", { level: 1 })).toHaveText("Contacts");
});

test("AC-020-03 in English the time of a Version reads 05.03.2026 10:00", async ({ page }) => {
  const ana = await user(ANA, "Ana Kovač");
  const groupId = await createGroup(ana, "Konferenca 2025");
  const id = await createContact(ana, groupId, {
    fields: { firstName: "Janez", lastName: "Novak" },
  });
  await stubGet(page, `/contacts/${id}/versions`, [
    {
      number: 1,
      kind: "created",
      changedBy: "Ana Kovač",
      changedAt: "2026-03-05T09:00:00Z",
      restoredFrom: null,
      changes: [],
    },
  ]);
  await signIn(page, ana);

  await page.goto(`/groups/${groupId}/contacts/${id}/history`);

  await expect(page.getByRole("main").getByText("05.03.2026 10:00")).toBeVisible();
});

test("EX-020-02 the count 10000 reads 10.000 in Slovenian and 10,000 in English", async ({
  page,
}) => {
  const ana = await user(ANA, "Ana Kovač", "sl");
  const groupId = await createGroup(ana, "Konferenca 2025");
  await stubGet(page, `/groups/${groupId}/contacts`, {
    items: [
      {
        id: "00000000-0000-4000-8000-000000000001",
        firstName: "Janez",
        lastName: "Novak",
        company: null,
        email: null,
        phone: null,
        tags: [],
        updatedAt: "2026-03-05T09:00:00Z",
      },
    ],
    total: 10000,
    page: 1,
    pageSize: 50,
  });
  await signIn(page, ana);

  await page.goto(listPath(groupId));
  await expect(
    page
      .getByRole("main")
      .getByText(/10\.000/)
      .first(),
  ).toBeVisible();

  expect((await api(ana, "PATCH", "/me", { language: "en" })).status).toBe(200);
  await page.reload();
  await expect(
    page
      .getByRole("main")
      .getByText(/10,000 Contacts/)
      .first(),
  ).toBeVisible();
});
