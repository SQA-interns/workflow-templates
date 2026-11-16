// E2E-03 Isolation between Groups (e2e-scenarios.md).
import { expect, test } from "@playwright/test";
import {
  ANA,
  PETRA,
  api,
  createContact,
  createGroup,
  listPath,
  resetStack,
  signIn,
  user,
} from "../acceptance/support/stack";

test.beforeEach(resetStack);

test("E2E-03 isolation between Groups", async ({ page }) => {
  const ana = await user(ANA, "Ana Kovač");
  const konferenca = await createGroup(ana, "Konferenca 2025");
  const janez = await createContact(ana, konferenca, {
    fields: { firstName: "Janez", lastName: "Novak" },
  });
  const petra = await user(PETRA, "Petra Horvat");
  const beta = await createGroup(petra, "Podjetje Beta");
  await createContact(petra, beta, { fields: { firstName: "Peter", lastName: "Kos" } });

  // Petra's Group switcher does not show "Konferenca 2025".
  await signIn(page, petra);
  await page.goto(listPath(beta));
  await page.getByLabel("Group switcher").click();
  await expect(page.getByText("Podjetje Beta").first()).toBeVisible();
  await expect(page.getByText("Konferenca 2025")).toHaveCount(0);
  await page.keyboard.press("Escape");

  // Her list and search "novak" find nothing.
  await expect(page.getByRole("link", { name: "Janez Novak" })).toHaveCount(0);
  await page.getByLabel("Search").fill("novak");
  await expect(page.getByText("No Contacts match.")).toBeVisible();

  // Ana's Contact URL in Petra's browser: "not found" page; the API answers 404.
  await page.goto(`/groups/${konferenca}/contacts/${janez}`);
  await expect(page.getByText("This page cannot be found.")).toBeVisible();
  await expect(page.getByText("Janez")).toHaveCount(0);
  const answer = await api(petra, "GET", `/groups/${konferenca}/contacts/${janez}`);
  expect(answer.status).toBe(404);
  expect(JSON.stringify(answer.json ?? "")).not.toContain("Novak");
});
