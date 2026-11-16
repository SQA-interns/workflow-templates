// US-001 Sign up and sign in: screens WF-01, WF-02, WF-03, WF-05 (API part in the backend, D-37).
import { expect, test, type Page } from "@playwright/test";
import {
  ANA,
  BASE_URL,
  MARKO,
  PASSWORD,
  accountsWithEmail,
  api,
  createGroup,
  oobCodes,
  resetStack,
  signIn,
  signInToken,
  signOut,
  user,
  verifyEmail,
} from "./support/stack";

test.beforeEach(resetStack);

/** Fills WF-02 without relying on its (Slovenian) text: the e-mail textbox and the password field. */
async function signUpThroughUi(page: Page, email: string): Promise<void> {
  await page.goto("/sign-up");
  await page.locator('input[type="email"]').fill(email);
  await page.locator('input[type="password"]').fill(PASSWORD);
  await page.locator('input[type="password"]').press("Enter");
}

test("AC-001-01 sign up, confirm the e-mail, sign in: You have no Groups yet.", async ({
  page,
  browser,
}) => {
  await signUpThroughUi(page, ANA);
  await expect(page).toHaveURL(/\/verify-email/);
  await verifyEmail(ANA);
  const ana = { email: ANA, displayName: "Ana Kovač", uid: "", token: await signInToken(ANA) };
  const me = await api(ana, "GET", "/me");
  expect(me.status).toBe(200);
  expect(me.json.email).toBe(ANA);
  expect(me.json.language).toBe("sl");
  expect((await api(ana, "PATCH", "/me", { language: "en" })).status).toBe(200);

  const context = await browser.newContext({ baseURL: BASE_URL });
  const fresh = await context.newPage();
  await signIn(fresh, ana);

  await expect(fresh.getByText("You have no Groups yet.")).toBeVisible();
  await expect(fresh.getByRole("button", { name: "Create a Group" })).toBeVisible();
  await context.close();
});

test("AC-001-02 Sign in with Google: signed in, GET /me has the Google e-mail", async ({
  page,
}) => {
  await page.goto("/sign-in");
  const me = page.waitForResponse(
    (r) =>
      new URL(r.url()).pathname === "/api/v1/me" &&
      r.request().method() === "GET" &&
      r.status() === 200,
  );
  const popup = page.waitForEvent("popup", { timeout: 10_000 }).catch(() => null);
  await page.getByRole("button", { name: /Google/ }).click();
  const idp = (await popup) ?? page;
  await idp.waitForURL(/\/emulator\/auth\/handler/);
  const add = idp.locator("#add-account-button");
  await add
    .waitFor({ state: "visible", timeout: 5_000 })
    .then(() => add.click())
    .catch(() => undefined);
  await idp.locator("#email-input").fill(MARKO);
  await idp.locator("#display-name-input").fill("Marko Kranjc");
  await idp.locator("#sign-in").click();

  expect((await (await me).json()).email).toBe(MARKO);
  await expect(page).not.toHaveURL(/\/sign-in/);
});

test("AC-001-05 unverified e-mail: confirm screen, link sent, API 403", async ({ page }) => {
  await signUpThroughUi(page, ANA);

  await expect(page).toHaveURL(/\/verify-email/);
  await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
  await expect
    .poll(async () => (await oobCodes(ANA)).map((c) => c.requestType))
    .toContain("VERIFY_EMAIL");
  const unverified = { email: ANA, displayName: "", uid: "", token: await signInToken(ANA) };
  expect((await api(unverified, "GET", "/groups")).status).toBe(403);
});

test("AC-001-06 Sign out shows the sign-in screen and keeps no token", async ({ page }) => {
  const ana = await user(ANA, "Ana Kovač");
  await createGroup(ana, "Konferenca 2025");
  await signIn(page, ana);

  await signOut(page, "Ana Kovač");

  await page.reload();
  await expect(page).toHaveURL(/\/sign-in/);
  await expect(page.locator('input[type="password"]')).toBeVisible();
  expect((await api(null, "GET", "/groups")).status).toBe(401);
});

test("EX-001-01 wrong password: same message as for an unknown e-mail, not signed in", async ({
  page,
}) => {
  await user(ANA, "Ana Kovač");

  await page.goto("/sign-in");
  await page.locator('input[type="email"]').fill(ANA);
  await page.locator('input[type="password"]').fill("Napacno#2026");
  await page.locator('input[type="password"]').press("Enter");
  await expect(page.getByRole("alert")).toBeVisible();
  const wrongPassword = (await page.getByRole("alert").textContent())?.trim();
  await expect(page).toHaveURL(/\/sign-in/);

  await page.reload();
  await page.locator('input[type="email"]').fill("nihce@example.si");
  await page.locator('input[type="password"]').fill(PASSWORD);
  await page.locator('input[type="password"]').press("Enter");
  await expect(page.getByRole("alert")).toBeVisible();
  const unknownEmail = (await page.getByRole("alert").textContent())?.trim();

  expect(wrongPassword).toBeTruthy();
  expect(wrongPassword).toBe(unknownEmail);
  await expect(page).toHaveURL(/\/sign-in/);
});

test("EX-001-05 an account with this e-mail already exists: message, no second account", async ({
  page,
}) => {
  await user(ANA, "Ana Kovač");

  await signUpThroughUi(page, ANA);

  await expect(page.getByRole("alert")).toBeVisible();
  await expect(page).toHaveURL(/\/sign-up/);
  expect(await accountsWithEmail(ANA)).toBe(1);
});
