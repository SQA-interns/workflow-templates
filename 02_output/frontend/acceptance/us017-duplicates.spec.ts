// US-017 Find Possible duplicates: WF-17 (API part in the backend).
import { expect, test } from "@playwright/test";
import { createContact, resetStack, signIn, team } from "./support/stack";

test.beforeEach(resetStack);

type Team = Awaited<ReturnType<typeof team>>;

function duplicatesPath(t: Team): string {
  return `/groups/${t.groupId}/duplicates`;
}

test("AC-017-01 a pair with the same e-mail is listed with Same e-mail", async ({ page }) => {
  const t = await team();
  await createContact(t.ana, t.groupId, {
    fields: { firstName: "Janez", lastName: "Novak", email: "janez.novak@example.si" },
  });
  await createContact(t.ana, t.groupId, {
    fields: { firstName: "J.", lastName: "Novak", email: "Janez.Novak@example.si" },
  });
  await signIn(page, t.marko);

  await page.goto(duplicatesPath(t));

  const main = page.getByRole("main");
  await expect(main.getByRole("link", { name: "Janez Novak" })).toBeVisible();
  await expect(main.getByRole("link", { name: "J. Novak" })).toBeVisible();
  await expect(main.getByText("Same e-mail")).toBeVisible();
  await expect(main.getByText("Same phone")).toHaveCount(0);
});

test("AC-017-02 a pair with the same phone is listed with Same phone", async ({ page }) => {
  const t = await team();
  await createContact(t.ana, t.groupId, {
    fields: { firstName: "Janez", lastName: "Novak", phone: "+386 41 123 456" },
  });
  await createContact(t.ana, t.groupId, {
    fields: { firstName: "Janez", lastName: "N.", phone: "+386-41-123-456" },
  });
  await signIn(page, t.marko);

  await page.goto(duplicatesPath(t));

  await expect(page.getByRole("main").getByText("Same phone")).toBeVisible();
});

test("AC-017-03 similar names with the same company: Similar name, same company", async ({
  page,
}) => {
  const t = await team();
  await createContact(t.ana, t.groupId, {
    fields: { firstName: "Janez", lastName: "Novak", company: "Podjetje d.o.o." },
  });
  await createContact(t.ana, t.groupId, {
    fields: { firstName: "Janez", lastName: "Novack", company: "Podjetje d.o.o." },
  });
  await signIn(page, t.marko);

  await page.goto(duplicatesPath(t));

  await expect(page.getByRole("main").getByText("Similar name, same company")).toBeVisible();
});

test("EX-017-07 no pairs: No possible duplicates found.", async ({ page }) => {
  const t = await team();
  await createContact(t.ana, t.groupId, { fields: { firstName: "Janez", lastName: "Novak" } });
  await signIn(page, t.marko);

  await page.goto(duplicatesPath(t));

  await expect(page.getByText("No possible duplicates found.")).toBeVisible();
});
