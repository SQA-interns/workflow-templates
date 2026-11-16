// US-010 History of a Contact: WF-09, WF-10. Times are examples of the AC, so the History answer
// is stubbed with those instants (the stack's clock cannot be set); the rest is the real stack.
import { expect, test } from "@playwright/test";
import { createContact, editContact, resetStack, signIn, stubGet, team } from "./support/stack";

test.beforeEach(resetStack);

type Team = Awaited<ReturnType<typeof team>>;

async function janez(t: Team): Promise<string> {
  return createContact(t.ana, t.groupId, {
    fields: {
      firstName: "Janez",
      lastName: "Novak",
      email: "janez.novak@example.si",
      city: "Ljubljana",
    },
  });
}

function version(
  number: number,
  kind: string,
  changedBy: string,
  changedAt: string,
  changes: object[],
) {
  return { number, kind, changedBy, changedAt, restoredFrom: null, changes };
}

test("AC-010-01 the History lists Versions newest first with name and local time", async ({
  page,
}) => {
  const t = await team();
  const id = await janez(t);
  await stubGet(page, `/contacts/${id}/versions`, [
    version(2, "edited", "Marko Kranjc", "2026-07-01T08:00:00Z", [
      { item: "email", oldValue: "janez.novak@example.si", newValue: "janez@novak.si" },
    ]),
    version(1, "created", "Ana Kovač", "2026-03-05T09:00:00Z", []),
  ]);
  await signIn(page, t.petra);

  await page.goto(`/groups/${t.groupId}/contacts/${id}/history`);

  const main = page.getByRole("main");
  await expect(main.getByText("E-mail: janez.novak@example.si → janez@novak.si")).toBeVisible();
  await expect(main.getByText("Marko Kranjc")).toBeVisible();
  await expect(main.getByText("01.07.2026 10:00")).toBeVisible();
  await expect(main.getByText("Ana Kovač")).toBeVisible();
  await expect(main.getByText("05.03.2026 10:00")).toBeVisible();
  const text = await main.innerText();
  expect(text.indexOf("Version 2")).toBeGreaterThanOrEqual(0);
  expect(text.indexOf("Version 2")).toBeLessThan(text.indexOf("Version 1"));
});

test("AC-010-02 one Version shows the content as created, without edit controls", async ({
  page,
}) => {
  const t = await team();
  const id = await janez(t);
  await editContact(t.marko, t.groupId, id, (u) => (u.fields.email = "janez@novak.si"));
  await signIn(page, t.petra);

  await page.goto(`/groups/${t.groupId}/contacts/${id}/history/1`);

  const main = page.getByRole("main");
  await expect(main.getByText("janez.novak@example.si", { exact: true })).toBeVisible();
  await expect(main.getByText("Ljubljana", { exact: true })).toBeVisible();
  await expect(main.getByText("janez@novak.si")).toHaveCount(0);
  await expect(main.getByRole("textbox")).toHaveCount(0);
  await expect(main.getByRole("button", { name: "Save" })).toHaveCount(0);
  await expect(main.getByRole("button", { name: "Edit" })).toHaveCount(0);
});

test("EX-010-01 a Version made at 31.12.2026 23:30 UTC is shown as 01.01.2027 00:30", async ({
  page,
}) => {
  const t = await team();
  const id = await janez(t);
  await stubGet(page, `/contacts/${id}/versions`, [
    version(1, "created", "Ana Kovač", "2026-12-31T23:30:00Z", []),
  ]);
  await signIn(page, t.petra);

  await page.goto(`/groups/${t.groupId}/contacts/${id}/history`);

  await expect(page.getByRole("main").getByText("01.01.2027 00:30")).toBeVisible();
});
