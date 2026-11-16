// E2E-06 Owner manages Members (e2e-scenarios.md).
import { expect, test } from "@playwright/test";
import {
  BASE_URL,
  api,
  choose,
  confirmDialog,
  createContact,
  getContact,
  resetStack,
  signIn,
  team,
} from "../acceptance/support/stack";

test.beforeEach(resetStack);

test("E2E-06 Owner manages Members", async ({ page, browser }) => {
  const t = await team();
  const janez = await createContact(t.ana, t.groupId, {
    fields: { firstName: "Janez", lastName: "Novak" },
  });
  const markoContext = await browser.newContext({ baseURL: BASE_URL });
  const markoPage = await markoContext.newPage();
  await signIn(markoPage, t.marko);
  await markoPage.goto(`/groups/${t.groupId}/contacts/${janez}/edit`);
  await expect(markoPage.getByLabel("First name")).toHaveValue("Janez");

  // Ana opens Members (Marko Editor, Petra Viewer) and changes Marko to Viewer.
  await signIn(page, t.ana);
  await page.goto(`/groups/${t.groupId}/members`);
  await expect(page.getByRole("row", { name: /Petra Horvat/ })).toBeVisible();
  await choose(page.getByRole("row", { name: /Marko Kranjc/ }).getByRole("combobox"), "Viewer");
  await expect
    .poll(async () => {
      const members = await api(t.ana, "GET", `/groups/${t.groupId}/members`);
      return members.json.find((m: { displayName: string }) => m.displayName === "Marko Kranjc")
        ?.role;
    })
    .toBe("VIEWER");

  // Marko's next save of a Contact is refused as "not allowed"; the Contact is unchanged.
  const before = await getContact(t.ana, t.groupId, janez);
  await markoPage.getByLabel("City").fill("Maribor");
  await markoPage.getByRole("button", { name: "Save" }).click();
  await expect(markoPage.getByText("You are not allowed to do this.")).toBeVisible();
  expect(await getContact(t.ana, t.groupId, janez)).toEqual(before);

  // Ana removes Petra: Petra no longer sees "Konferenca 2025".
  await page
    .getByRole("row", { name: /Petra Horvat/ })
    .getByRole("button", { name: "Remove from Group" })
    .click();
  await confirmDialog(page, "Remove from Group");
  await expect(page.getByRole("row", { name: /Petra Horvat/ })).toHaveCount(0);
  const petraContext = await browser.newContext({ baseURL: BASE_URL });
  const petraPage = await petraContext.newPage();
  await signIn(petraPage, t.petra);
  await expect(petraPage.getByText("You have no Groups yet.")).toBeVisible();
  await expect(petraPage.getByText("Konferenca 2025")).toHaveCount(0);
  await markoContext.close();
  await petraContext.close();
});
