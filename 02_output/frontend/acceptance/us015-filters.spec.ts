// US-015 Filter the Contact list: WF-06 filter panel and chips (API part in the backend).
import { expect, test, type Page } from "@playwright/test";
import { choose, createContact, listPath, resetStack, signIn, team } from "./support/stack";

test.beforeEach(resetStack);

type Team = Awaited<ReturnType<typeof team>>;

/** Janez (udelezenec2025, SI, kompetenca), Maja (udelezenec2025, neObvescaj, AT), Žiga (no Tags). */
async function threeContacts(t: Team): Promise<void> {
  await createContact(t.ana, t.groupId, {
    fields: { firstName: "Janez", lastName: "Novak", country: "SI" },
    tags: ["udelezenec2025"],
    attributes: [{ key: "kompetenca", value: "spletnoProgramiranje" }],
  });
  await createContact(t.ana, t.groupId, {
    fields: { firstName: "Maja", lastName: "Zupan", country: "AT" },
    tags: ["udelezenec2025", "neObvescaj"],
  });
  await createContact(t.ana, t.groupId, { fields: { firstName: "Žiga", lastName: "Čuk" } });
}

async function openList(page: Page, t: Team): Promise<void> {
  await signIn(page, t.petra);
  await page.goto(listPath(t.groupId));
  await expect(page.getByText("3 Contacts")).toBeVisible();
}

async function filter(page: Page, set: (page: Page) => Promise<void>): Promise<void> {
  await page.getByRole("button", { name: "Filters" }).click();
  await set(page);
  await page.getByRole("button", { name: "Apply" }).click();
}

test("AC-015-01 has Tags: udelezenec2025 shows 2 Contacts", async ({ page }) => {
  const t = await team();
  await threeContacts(t);
  await openList(page, t);

  await filter(page, (p) => choose(p.getByLabel("has Tags"), "udelezenec2025"));

  await expect(page.getByText("2 Contacts")).toBeVisible();
  await expect(page.getByRole("link", { name: "Janez Novak" })).toBeVisible();
  await expect(page.getByRole("link", { name: "Maja Zupan" })).toBeVisible();
  await expect(page.getByRole("link", { name: "Žiga Čuk" })).toHaveCount(0);
  await expect(page.getByText(/has Tags: udelezenec2025/)).toBeVisible();
});

test("AC-015-02 adding without Tags: neObvescaj shows 1 Contact", async ({ page }) => {
  const t = await team();
  await threeContacts(t);
  await openList(page, t);

  await filter(page, async (p) => {
    await choose(p.getByLabel("has Tags"), "udelezenec2025");
    await choose(p.getByLabel("without Tags"), "neObvescaj");
  });

  await expect(page.getByText("1 Contact", { exact: true })).toBeVisible();
  await expect(page.getByRole("link", { name: "Janez Novak" })).toBeVisible();
});

test("AC-015-03 an Attribute filter matches case-insensitively", async ({ page }) => {
  const t = await team();
  await threeContacts(t);
  await openList(page, t);

  await filter(page, async (p) => {
    await p.getByLabel("Attribute key").fill("Kompetenca");
    await p.getByLabel("Attribute value").fill("SPLETNOPROGRAMIRANJE");
  });

  await expect(page.getByText("1 Contact", { exact: true })).toBeVisible();
  await expect(page.getByRole("link", { name: "Janez Novak" })).toBeVisible();
});

test("AC-015-04 Clear filters shows all 3 Contacts again", async ({ page }) => {
  const t = await team();
  await threeContacts(t);
  await openList(page, t);
  await filter(page, async (p) => {
    await choose(p.getByLabel("has Tags"), "udelezenec2025");
    await choose(p.getByLabel("without Tags"), "neObvescaj");
  });
  await expect(page.getByText("1 Contact", { exact: true })).toBeVisible();

  await page
    .getByRole("link", { name: "Clear filters" })
    .or(page.getByRole("button", { name: "Clear filters" }))
    .first()
    .click();

  await expect(page.getByText("3 Contacts")).toBeVisible();
});

test("AC-015-05 country Slovenia and search maja: No Contacts match.", async ({ page }) => {
  const t = await team();
  await threeContacts(t);
  await openList(page, t);

  await filter(page, (p) => choose(p.getByLabel("Country"), "Slovenia"));
  await expect(page.getByText("1 Contact", { exact: true })).toBeVisible();
  await page.getByLabel("Search").fill("maja");

  await expect(page.getByText("No Contacts match.")).toBeVisible();
});
