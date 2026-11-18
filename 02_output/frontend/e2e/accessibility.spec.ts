// NFR-07, NFR-06 (specification §8): every screen at the three widths (UI-22) has no Critical or
// Serious axe violation, in every configured browser; and E2E-01 can be done with the keyboard
// alone. The walk-through is recorded as a list of steps with the Tab presses each needed
// (keyboard-walk-through.txt in the test's output folder) and, after sign-up, as a video.
import AxeBuilder from "@axe-core/playwright";
import { writeFileSync } from "node:fs";
import { expect, test, type Locator, type Page } from "@playwright/test";
import { ANA, BASE_URL, PASSWORD, resetStack, verifyEmail } from "../acceptance/support/stack";
import { SCREENS, WIDTHS, atWidth, makeFixture, type Fixture } from "./support/screens";

test.describe("NFR-07 axe on every screen", () => {
  test.describe.configure({ mode: "serial" });
  let fx: Fixture;

  test.beforeAll(async () => {
    fx = await makeFixture();
  });

  for (const screen of SCREENS) {
    test(`NFR-07 ${screen.id} ${screen.name}: no Critical or Serious violation at the three widths`, async ({
      page,
    }) => {
      await screen.open(page, fx);
      for (const width of WIDTHS) {
        await atWidth(page, width);
        const result = await axe(page).analyze();
        const serious = result.violations
          .filter((v) => v.impact === "critical" || v.impact === "serious")
          .map((v) => `${v.impact} ${v.id}: ${v.nodes.map((n) => n.target.join(" ")).join(", ")}`);
        expect(serious, `${screen.id} ${width.label}`).toEqual([]);
      }
    });
  }
});

test.describe("NFR-07 keyboard walk-through of E2E-01", () => {
  test.beforeEach(resetStack);

  test("NFR-07 E2E-01 from sign-up to an Excel file with the keyboard only", async ({
    page,
    browser,
  }, testInfo) => {
    const steps: string[] = [];
    const walk = new KeyboardWalk(page, steps);

    // Create an account and confirm the e-mail (the link in the e-mail is followed by the helper).
    await page.goto("/sign-up");
    await walk.typeInto(page.locator('input[type="email"]'), ANA, "sign-up e-mail");
    await walk.typeInto(page.locator('input[type="password"]'), PASSWORD, "sign-up password");
    await walk.press("Enter", "create the account");
    await expect(page).toHaveURL(/\/verify-email/);
    await verifyEmail(ANA);

    const context = await browser.newContext({
      baseURL: BASE_URL,
      recordVideo: { dir: testInfo.outputPath("video") },
    });
    const app = await context.newPage();
    const keys = new KeyboardWalk(app, steps);
    await app.goto("/sign-in");
    await keys.typeInto(app.locator('input[type="email"]'), ANA, "sign-in e-mail");
    await keys.typeInto(app.locator('input[type="password"]'), PASSWORD, "sign-in password");
    await keys.press("Enter", "sign in");
    await expect(app).not.toHaveURL(/\/sign-in/);

    // Switch to English on the Account screen.
    await app.goto("/account");
    await keys.select(app.getByRole("combobox").filter({ hasText: "Slovenščina" }), "English");
    await expect(app.getByText("Contacts", { exact: true }).first()).toBeVisible();

    // Create the Group "Konferenca 2025".
    await app.goto("/");
    await keys.activate(app.getByRole("button", { name: "Create a Group" }), "Create a Group");
    await keys.typeInto(app.getByLabel("Group name"), "Konferenca 2025", "Group name");
    await keys.press("Enter", "create the Group");
    await expect(app.getByText("No Contacts yet.")).toBeVisible();

    // Add Janez Novak with an Attribute and a Tag.
    await keys.activate(
      app.getByRole("button", { name: "Add a Contact" }).first(),
      "Add a Contact",
    );
    await keys.typeInto(app.getByLabel("First name"), "Janez", "First name");
    await keys.typeInto(app.getByLabel("Last name"), "Novak", "Last name");
    await keys.typeInto(app.getByLabel("E-mail"), "janez.novak@example.si", "E-mail");
    await keys.activate(app.getByRole("button", { name: "Add an Attribute" }), "Add an Attribute");
    await keys.typeInto(app.getByLabel("Key").last(), "kompetenca", "Attribute key");
    await keys.typeInto(app.getByLabel("Value").last(), "spletnoProgramiranje", "Attribute value");
    await keys.typeInto(app.getByLabel("Add a Tag"), "udelezenec2025", "Tag");
    await keys.press("Enter", "add the Tag");
    await keys.activate(app.getByRole("button", { name: "Save" }), "Save");
    await expect(app.getByRole("link", { name: "Janez Novak" })).toBeVisible();

    // Search "novak" on the list, Export.
    await keys.typeInto(app.getByLabel("Search"), "novak", "Search");
    await expect(app.getByRole("link", { name: "Janez Novak" })).toBeVisible();
    const download = app.waitForEvent("download");
    await keys.activate(app.getByRole("button", { name: "Export", exact: true }), "Export");
    expect((await download).suggestedFilename()).toMatch(/^kontakti-Konferenca 2025-/);

    await context.close();
    const record = testInfo.outputPath("keyboard-walk-through.txt");
    // eslint-disable-next-line security/detect-non-literal-fs-filename -- the test's own output folder
    writeFileSync(record, `${steps.join("\n")}\n`);
    await testInfo.attach("keyboard-walk-through", { path: record, contentType: "text/plain" });
  });
});

/**
 * axe for the page. @axe-core/playwright brings its own playwright-core (1.65.1) whose Page type
 * differs from that of @playwright/test (1.56.1); the object is the same at runtime.
 */
function axe(page: Page): AxeBuilder {
  return new AxeBuilder({
    page: page as unknown as ConstructorParameters<typeof AxeBuilder>[0]["page"],
  });
}

/** Moves the focus with Tab only and records each step (NFR-07). */
class KeyboardWalk {
  constructor(
    private readonly page: Page,
    private readonly steps: string[],
  ) {}

  /** Presses Tab until the element has the focus; fails after 60 presses. */
  async focus(target: Locator, step: string): Promise<void> {
    for (let presses = 0; presses <= 60; presses++) {
      if (await target.evaluate((el) => el === document.activeElement).catch(() => false)) {
        this.steps.push(`${step}: focused after ${presses} Tab`);
        return;
      }
      await this.page.keyboard.press("Tab");
    }
    throw new Error(`${step}: not reachable with Tab`);
  }

  async typeInto(field: Locator, text: string, step: string): Promise<void> {
    await this.focus(field, step);
    await this.page.keyboard.type(text);
  }

  /** Focuses a button, link or select and presses Enter. */
  async activate(target: Locator, step: string): Promise<void> {
    await this.focus(target, step);
    await this.page.keyboard.press("Enter");
  }

  /** Focuses a native select and moves its choice with the arrow keys to the option. */
  async select(control: Locator, option: string): Promise<void> {
    await this.focus(control, `select ${option}`);
    for (let presses = 0; presses <= 20; presses++) {
      const chosen = await control.evaluate(
        (el) => (el as HTMLSelectElement).selectedOptions[0]?.text,
      );
      if (chosen === option) {
        this.steps.push(`${option}: chosen after ${presses} ArrowDown`);
        return;
      }
      await this.page.keyboard.press("ArrowDown");
    }
    throw new Error(`${option}: not reachable with the arrow keys`);
  }

  async press(key: string, step: string): Promise<void> {
    await this.page.keyboard.press(key);
    this.steps.push(`${step}: ${key}`);
  }
}
