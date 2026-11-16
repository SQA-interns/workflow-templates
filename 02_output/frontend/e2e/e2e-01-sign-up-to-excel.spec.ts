// E2E-01 From sign-up to an Excel file (e2e-scenarios.md). The language switch comes right after
// the Slovenian check, because the Slovenian wording of the screens is not in the inputs (D-37).
import { expect, test } from "@playwright/test";
import {
  ANA,
  BASE_URL,
  PASSWORD,
  choose,
  resetStack,
  signIn,
  verifyEmail,
} from "../acceptance/support/stack";
import { readXlsx } from "../acceptance/support/xlsx";

test.beforeEach(resetStack);

test("E2E-01 from sign-up to an Excel file", async ({ page, browser }) => {
  // Create an account, confirm the e-mail, sign in.
  await page.goto("/sign-up");
  await page.getByRole("textbox").fill(ANA);
  await page.locator('input[type="password"]').fill(PASSWORD);
  await page.locator('input[type="password"]').press("Enter");
  await expect(page).toHaveURL(/\/verify-email/);
  await verifyEmail(ANA);
  const context = await browser.newContext({ baseURL: BASE_URL });
  const app = await context.newPage();
  await signIn(app, { email: ANA });

  // The UI is Slovenian; switch to English: the title reads "Contacts".
  await expect(app.getByText("Kontakti", { exact: true }).first()).toBeVisible();
  await app.goto("/account");
  await choose(app.getByRole("combobox").filter({ hasText: "Slovenščina" }), "English");
  await expect(app.getByText("Contacts", { exact: true }).first()).toBeVisible();

  // Create the Group "Konferenca 2025".
  await app.goto("/");
  await app.getByRole("button", { name: "Create a Group" }).click();
  await app.getByLabel("Group name").fill("Konferenca 2025");
  await app.getByRole("button", { name: "Create a Group" }).click();
  await expect(app.getByText("No Contacts yet.")).toBeVisible();
  await expect(app.getByRole("heading", { level: 1 })).toHaveText("Contacts");
  const list = new URL(app.url()).pathname;

  // Add Janez Novak with an Attribute and a Tag.
  await app.getByRole("button", { name: "Add a Contact" }).first().click();
  await app.getByLabel("First name").fill("Janez");
  await app.getByLabel("Last name").fill("Novak");
  await app.getByLabel("E-mail").fill("janez.novak@example.si");
  await app.getByRole("button", { name: "Add an Attribute" }).click();
  await app.getByLabel("Key").last().fill("kompetenca");
  await app.getByLabel("Value").last().fill("spletnoProgramiranje");
  await app.getByLabel("Add a Tag").fill("udelezenec2025");
  await app.getByLabel("Add a Tag").press("Enter");
  await app.getByRole("button", { name: "Save" }).click();
  await expect(
    app
      .getByRole("heading", { name: /Janez Novak/ })
      .or(app.getByRole("link", { name: "Janez Novak" }))
      .first(),
  ).toBeVisible();

  // Add Maja Zupan without Tags.
  await app.goto(list);
  await app.getByRole("button", { name: "Add a Contact" }).first().click();
  await app.getByLabel("First name").fill("Maja");
  await app.getByLabel("Last name").fill("Zupan");
  await app.getByRole("button", { name: "Save" }).click();
  await expect(
    app
      .getByRole("heading", { name: /Maja Zupan/ })
      .or(app.getByRole("link", { name: "Maja Zupan" }))
      .first(),
  ).toBeVisible();

  // Search "novak" shows only Janez.
  await app.goto(list);
  await expect(app.getByText("2 Contacts")).toBeVisible();
  await app.getByLabel("Search").fill("novak");
  await expect(app.getByRole("link", { name: "Janez Novak" })).toBeVisible();
  await expect(app.getByRole("link", { name: "Maja Zupan" })).toHaveCount(0);
  await app.getByLabel("Search").fill("");
  await expect(app.getByText("2 Contacts")).toBeVisible();

  // Filter "has Tags: udelezenec2025" shows "1 Contact".
  await app.getByRole("button", { name: "Filters" }).click();
  await choose(app.getByLabel("has Tags"), "udelezenec2025");
  await app.getByRole("button", { name: "Apply" }).click();
  await expect(app.getByText("1 Contact", { exact: true })).toBeVisible();

  // Export: one data row Janez, Novak, kompetenca, Tags.
  const download = app.waitForEvent("download");
  await app.getByRole("button", { name: "Export", exact: true }).click();
  const file = await download;
  expect(file.suggestedFilename()).toMatch(/^kontakti-Konferenca 2025-\d{4}-\d{2}-\d{2}\.xlsx$/);
  const chunks: Buffer[] = [];
  for await (const chunk of await file.createReadStream()) chunks.push(chunk as Buffer);
  const [header = [], ...data] = readXlsx(Buffer.concat(chunks));
  expect(data).toHaveLength(1);
  const cell = (name: string) => data[0]?.at(header.indexOf(name)) ?? "";
  expect(cell("First name")).toBe("Janez");
  expect(cell("Last name")).toBe("Novak");
  expect(cell("kompetenca")).toBe("spletnoProgramiranje");
  expect(cell("Tags")).toBe("udelezenec2025");

  // The title still reads "Contacts".
  await expect(app.getByRole("heading", { level: 1 })).toHaveText("Contacts");
  await context.close();
});
