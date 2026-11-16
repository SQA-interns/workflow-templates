// US-006 Add a Contact: WF-08 (API part in the backend).
import { expect, test } from "@playwright/test";
import { api, choose, createContact, listPath, resetStack, signIn, team } from "./support/stack";

test.beforeEach(resetStack);

async function total(t: Awaited<ReturnType<typeof team>>): Promise<number> {
  return (await api(t.ana, "GET", `/groups/${t.groupId}/contacts`)).json.total as number;
}

test("AC-006-01 an Editor adds a Contact; it is listed; History shows created by him", async ({
  page,
}) => {
  const t = await team();
  await signIn(page, t.marko);

  await page.goto(`/groups/${t.groupId}/contacts/new`);
  await page.getByLabel("First name").fill("Janez");
  await page.getByLabel("Last name").fill("Novak");
  await page.getByLabel("E-mail").fill("janez.novak@example.si");
  await page.getByLabel("Phone").fill("+386 41 123 456");
  await page.getByLabel("Company").fill("Podjetje d.o.o.");
  await page.getByLabel("Job title").fill("Direktor");
  await page.getByLabel("Street").fill("Slovenska cesta 1");
  await page.getByLabel("Postal code").fill("1000");
  await page.getByLabel("City").fill("Ljubljana");
  await choose(page.getByLabel("Country"), "Slovenia");
  await page.getByLabel("Note").fill("Govornik");
  await page.getByRole("button", { name: "Save" }).click();

  await expect.poll(() => total(t)).toBe(1);
  const created = (await api(t.ana, "GET", `/groups/${t.groupId}/contacts`)).json.items[0];
  await page.goto(listPath(t.groupId));
  await expect(page.getByRole("link", { name: "Janez Novak" })).toBeVisible();
  await page.goto(`/groups/${t.groupId}/contacts/${created.id}/history`);
  await expect(page.getByRole("main").getByText("created", { exact: true })).toBeVisible();
  await expect(page.getByRole("main").getByText("Marko Kranjc")).toBeVisible();
  const contact = (await api(t.ana, "GET", `/groups/${t.groupId}/contacts/${created.id}`)).json;
  expect(contact.fields.country).toBe("SI");
});

test("AC-006-03 only an e-mail: Enter a first name, last name or company.", async ({ page }) => {
  const t = await team();
  await signIn(page, t.marko);

  await page.goto(`/groups/${t.groupId}/contacts/new`);
  await page.getByLabel("E-mail").fill("info@example.si");
  await page.getByRole("button", { name: "Save" }).click();

  await expect(page.getByText("Enter a first name, last name or company.")).toBeVisible();
  expect(await total(t)).toBe(0);
});

test("AC-006-04 an invalid e-mail: Enter a valid e-mail address.", async ({ page }) => {
  const t = await team();
  await signIn(page, t.marko);

  await page.goto(`/groups/${t.groupId}/contacts/new`);
  await page.getByLabel("First name").fill("Janez");
  await page.getByLabel("E-mail").fill("janez@");
  await page.getByRole("button", { name: "Save" }).click();

  await expect(page.getByText("Enter a valid e-mail address.")).toBeVisible();
  expect(await total(t)).toBe(0);
});

test("AC-006-05 an e-mail that exists is reported and saving still works", async ({ page }) => {
  const t = await team();
  await createContact(t.ana, t.groupId, {
    fields: { firstName: "Janez", lastName: "Novak", email: "janez.novak@example.si" },
  });
  await signIn(page, t.marko);

  await page.goto(`/groups/${t.groupId}/contacts/new`);
  await page.getByLabel("First name").fill("J.");
  await page.getByLabel("Last name").fill("Novak");
  await page.getByLabel("E-mail").fill("Janez.Novak@example.si");
  await page.getByLabel("E-mail").press("Tab");

  await expect(
    page.getByText("A Contact with this e-mail already exists: Janez Novak."),
  ).toBeVisible();
  await page.getByRole("button", { name: "Save" }).click();
  await expect.poll(() => total(t)).toBe(2);
});

test("EX-006-08 a name with diacritics is shown as entered", async ({ page }) => {
  const t = await team();
  await createContact(t.ana, t.groupId, { fields: { firstName: "Žiga", lastName: "Čuk" } });
  await signIn(page, t.marko);

  await page.goto(listPath(t.groupId));

  await expect(page.getByRole("link", { name: "Žiga Čuk" })).toBeVisible();
});
