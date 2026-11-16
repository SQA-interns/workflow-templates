// UI helpers for browser tests (D-37): elements are found by role, label or text of the
// wireframes. The stack and API helpers are in ./api and re-exported here.
import { expect, type Locator, type Page } from "@playwright/test";
import { PASSWORD } from "./api";

export * from "./api";

// ------------------------------------------------------------------------------- UI

/**
 * Signs in through the sign-in screen (WF-01). The screen is Slovenian before sign-in, so the
 * fields are found without text: the only textbox (e-mail), the password field, Enter (D-37).
 */
export async function signIn(page: Page, u: { email: string }, password = PASSWORD): Promise<void> {
  await page.goto("/sign-in");
  await page.locator('input[type="email"]').fill(u.email);
  await page.locator('input[type="password"]').fill(password);
  await page.locator('input[type="password"]').press("Enter");
  await expect(page).not.toHaveURL(/\/sign-in/);
}

/** Opens the Group switcher (header) and selects the Group. */
export async function selectGroup(page: Page, name: string): Promise<void> {
  await page.getByLabel("Group switcher").click();
  await page
    .getByRole("menuitem", { name, exact: true })
    .or(page.getByRole("menuitemradio", { name, exact: true }))
    .or(page.getByRole("option", { name, exact: true }))
    .or(page.getByRole("link", { name, exact: true }))
    .first()
    .click();
}

/** Opens the user menu (header button with the User's display name). */
export async function openUserMenu(page: Page, displayName: string): Promise<void> {
  await page.getByRole("button", { name: displayName }).click();
}

/** Chooses an option of a select-like control (native select, combobox or listbox). */
export async function choose(control: Locator, option: string): Promise<void> {
  const tag = await control.evaluate((el) => el.tagName.toLowerCase());
  if (tag === "select") {
    await control.selectOption({ label: option });
    return;
  }
  await control.click();
  const page = control.page();
  await page
    .getByRole("option", { name: option, exact: true })
    .or(page.getByRole("menuitemcheckbox", { name: option, exact: true }))
    .or(page.getByRole("checkbox", { name: option, exact: true }))
    .first()
    .click();
}

/** Clicks the confirming button of an open confirmation dialog (UI-15). */
export async function confirmDialog(page: Page, button: string): Promise<void> {
  await page
    .getByRole("alertdialog")
    .or(page.getByRole("dialog"))
    .getByRole("button", { name: button })
    .click();
}

/** Clicks Cancel in an open confirmation dialog. */
export async function cancelDialog(page: Page): Promise<void> {
  await page
    .getByRole("alertdialog")
    .or(page.getByRole("dialog"))
    .getByRole("button", { name: "Cancel" })
    .click();
}

/** Signs out through the user menu (R-001-06). */
export async function signOut(page: Page, displayName: string): Promise<void> {
  await openUserMenu(page, displayName);
  await page
    .getByRole("menuitem", { name: "Sign out" })
    .or(page.getByRole("link", { name: "Sign out" }))
    .or(page.getByRole("button", { name: "Sign out" }))
    .first()
    .click();
  await expect(page).toHaveURL(/\/sign-in/);
}

/** Asserts the shown choice of a select-like control (native select or combobox). */
export async function expectSelected(control: Locator, text: string): Promise<void> {
  const tag = await control.evaluate((el) => el.tagName.toLowerCase());
  if (tag === "select") {
    await expect
      .poll(() =>
        control.evaluate((el) => (el as HTMLSelectElement).selectedOptions[0]?.label ?? ""),
      )
      .toBe(text);
  } else {
    await expect(control).toContainText(text);
  }
}

/** Answers GET requests whose path ends with the suffix with a JSON body (time-bound examples). */
export async function stubGet(page: Page, pathSuffix: string, body: unknown): Promise<void> {
  await page.route(
    (url) => url.pathname.endsWith(pathSuffix),
    async (route) => {
      if (route.request().method() !== "GET") {
        await route.fallback();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(body),
      });
    },
  );
}

/** "Contacts" list page of a Group. */
export function listPath(groupId: string): string {
  return `/groups/${groupId}/contacts`;
}
