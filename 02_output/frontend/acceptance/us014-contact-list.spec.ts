// US-014 Contact list and search: WF-06, WF-07 (API part in the backend).
import { expect, test } from "@playwright/test";
import {
  createContact,
  listPath,
  moveContactToRecycleBin,
  resetStack,
  signIn,
  team,
} from "./support/stack";

test.beforeEach(resetStack);

type Team = Awaited<ReturnType<typeof team>>;

async function fourContacts(t: Team): Promise<string> {
  const janez = await createContact(t.ana, t.groupId, {
    fields: { firstName: "Janez", lastName: "Novak", company: "Podjetje d.o.o." },
    tags: ["udelezenec2025"],
    attributes: [{ key: "kompetenca", value: "spletnoProgramiranje" }],
  });
  await createContact(t.ana, t.groupId, { fields: { firstName: "Maja", lastName: "Zupan" } });
  await createContact(t.ana, t.groupId, { fields: { firstName: "Žiga", lastName: "Čuk" } });
  const bizjak = await createContact(t.ana, t.groupId, {
    fields: { firstName: "Ana", lastName: "Bizjak" },
  });
  await moveContactToRecycleBin(t.ana, t.groupId, bizjak);
  return janez;
}

test("AC-014-01 3 Contacts in the order Čuk, Novak, Zupan; the one in the recycle bin is not shown", async ({
  page,
}) => {
  const t = await team();
  await fourContacts(t);
  await signIn(page, t.petra);

  await page.goto(listPath(t.groupId));

  await expect(page.getByText("3 Contacts")).toBeVisible();
  const table = await page.getByRole("table").innerText();
  expect(table.indexOf("Žiga Čuk")).toBeGreaterThanOrEqual(0);
  expect(table.indexOf("Žiga Čuk")).toBeLessThan(table.indexOf("Janez Novak"));
  expect(table.indexOf("Janez Novak")).toBeLessThan(table.indexOf("Maja Zupan"));
  expect(table).not.toContain("Bizjak");
  for (const header of ["Name", "Company", "E-mail", "Phone", "Tags"]) {
    await expect(page.getByRole("columnheader", { name: header })).toBeVisible();
  }
});

test("AC-014-04 a search without matches: No Contacts match. with Clear search", async ({
  page,
}) => {
  const t = await team();
  await fourContacts(t);
  await signIn(page, t.petra);

  await page.goto(listPath(t.groupId));
  await page.getByLabel("Search").fill("xyz");

  await expect(page.getByText("No Contacts match.")).toBeVisible();
  await page.getByRole("button", { name: "Clear search" }).click();
  await expect(page.getByText("3 Contacts")).toBeVisible();
});

test("AC-014-02 searching cuk shows only Žiga Čuk", async ({ page }) => {
  const t = await team();
  await fourContacts(t);
  await signIn(page, t.petra);

  await page.goto(listPath(t.groupId));
  await page.getByLabel("Search").fill("cuk");

  await expect(page.getByText("1 Contact", { exact: true })).toBeVisible();
  await expect(page.getByRole("link", { name: "Žiga Čuk" })).toBeVisible();
  await expect(page.getByRole("link", { name: "Janez Novak" })).toHaveCount(0);
});

test("EX-014-02 an empty Group: No Contacts yet. with Add a Contact", async ({ page }) => {
  const t = await team();
  await signIn(page, t.ana);

  await page.goto(listPath(t.groupId));

  await expect(page.getByText("No Contacts yet.")).toBeVisible();
  await expect(page.getByRole("button", { name: "Add a Contact" }).first()).toBeVisible();
});

test("R-014-06 opening a row shows the details with Attributes, Tags and a link to the History", async ({
  page,
}) => {
  const t = await team();
  const janez = await fourContacts(t);
  await signIn(page, t.petra);

  await page.goto(listPath(t.groupId));
  await page.getByRole("link", { name: "Janez Novak" }).click();

  await expect(page).toHaveURL((url) => url.pathname.includes(`/contacts/${janez}`));
  const main = page.getByRole("main");
  await expect(main.getByText("spletnoProgramiranje")).toBeVisible();
  await expect(main.getByText("udelezenec2025")).toBeVisible();
  await expect(
    main
      .getByRole("link", { name: "History" })
      .or(main.getByRole("button", { name: "History" }))
      .first(),
  ).toBeVisible();
});
