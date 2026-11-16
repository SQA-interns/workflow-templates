// US-016 Export to Excel: WF-06 (Export, Export checked). File content is checked in the backend.
import { expect, test } from "@playwright/test";
import {
  api,
  bulkContacts,
  choose,
  createContact,
  listPath,
  resetStack,
  signIn,
  team,
} from "./support/stack";

test.beforeEach(resetStack);

type Team = Awaited<ReturnType<typeof team>>;

/** Today's date in Europe/Ljubljana as yyyy-mm-dd (R-016-06). */
function today(): string {
  return new Intl.DateTimeFormat("en-CA", { timeZone: "Europe/Ljubljana" }).format(new Date());
}

async function threeContacts(t: Team): Promise<void> {
  await createContact(t.ana, t.groupId, {
    fields: { firstName: "Janez", lastName: "Novak" },
    tags: ["udelezenec2025", "vip"],
    attributes: [{ key: "kompetenca", value: "spletnoProgramiranje" }],
  });
  await createContact(t.ana, t.groupId, {
    fields: { firstName: "Maja", lastName: "Zupan" },
    tags: ["udelezenec2025"],
    attributes: [{ key: "jezik", value: "nemščina" }],
  });
  await createContact(t.ana, t.groupId, { fields: { firstName: "Žiga", lastName: "Čuk" } });
}

test("AC-016-01 Export of the filtered list downloads kontakti-<Group>-<date>.xlsx", async ({
  page,
}) => {
  const t = await team();
  await threeContacts(t);
  await signIn(page, t.petra);
  await page.goto(listPath(t.groupId));
  await page.getByRole("button", { name: "Filters" }).click();
  await choose(page.getByLabel("has Tags"), "udelezenec2025");
  await page.getByRole("button", { name: "Apply" }).click();
  await expect(page.getByText("2 Contacts")).toBeVisible();

  const download = page.waitForEvent("download");
  await page.getByRole("button", { name: "Export", exact: true }).click();

  expect((await download).suggestedFilename()).toBe(`kontakti-Konferenca 2025-${today()}.xlsx`);
});

test("AC-016-02 Export checked with one checked Contact", async ({ page }) => {
  const t = await team();
  await threeContacts(t);
  await signIn(page, t.petra);
  await page.goto(listPath(t.groupId));

  await page
    .getByRole("row", { name: /Maja Zupan/ })
    .getByRole("checkbox")
    .check();
  await expect(page.getByText("1 checked")).toBeVisible();
  const download = page.waitForEvent("download");
  await page.getByRole("button", { name: /Export checked/ }).click();

  expect((await download).suggestedFilename()).toMatch(
    /^kontakti-Konferenca 2025-\d{4}-\d{2}-\d{2}\.xlsx$/,
  );
});

test("AC-016-03 more than 10,000 Contacts: Narrow the list to at most 10,000 Contacts to export.", async ({
  page,
}) => {
  const t = await team();
  const anaId = (await api(t.ana, "GET", "/me")).json.id as string;
  bulkContacts(t.groupId, anaId, 10_001);
  await signIn(page, t.petra);
  await page.goto(listPath(t.groupId));

  let downloaded = false;
  page.on("download", () => (downloaded = true));
  const exportButton = page.getByRole("button", { name: "Export", exact: true });
  if (await exportButton.isEnabled()) {
    await exportButton.click();
  }

  await expect(
    page.getByText("Narrow the list to at most 10,000 Contacts to export."),
  ).toBeVisible();
  expect(downloaded).toBe(false);
});

test("EX-016-02 an empty Export scope: Export disabled, Nothing to export.", async ({ page }) => {
  const t = await team();
  await threeContacts(t);
  await signIn(page, t.petra);
  await page.goto(listPath(t.groupId));

  await page.getByLabel("Search").fill("xyz");

  await expect(page.getByText("No Contacts match.")).toBeVisible();
  await expect(page.getByRole("button", { name: "Export", exact: true })).toBeDisabled();
  await expect(page.getByText("Nothing to export.")).toBeVisible();
});
