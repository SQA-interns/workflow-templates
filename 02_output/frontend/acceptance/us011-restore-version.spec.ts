// US-011 Restore a Version: WF-10, WF-09 (API part in the backend).
import { expect, test } from "@playwright/test";
import {
  api,
  cancelDialog,
  confirmDialog,
  createContact,
  editContact,
  getContact,
  resetStack,
  signIn,
  team,
} from "./support/stack";

test.beforeEach(resetStack);

type Team = Awaited<ReturnType<typeof team>>;

async function janezAtVersion2(t: Team): Promise<string> {
  const id = await createContact(t.ana, t.groupId, {
    fields: { firstName: "Janez", lastName: "Novak", email: "janez.novak@example.si" },
  });
  await editContact(t.marko, t.groupId, id, (u) => (u.fields.email = "janez@novak.si"));
  return id;
}

async function versions(t: Team, id: string): Promise<number> {
  return (await api(t.ana, "GET", `/groups/${t.groupId}/contacts/${id}/versions`)).json
    .length as number;
}

test("AC-011-01 Restore this Version: e-mail back, Version 3 restored from Version 1", async ({
  page,
}) => {
  const t = await team();
  const id = await janezAtVersion2(t);
  await signIn(page, t.marko);

  await page.goto(`/groups/${t.groupId}/contacts/${id}/history/1`);
  await page.getByRole("button", { name: "Restore this Version" }).click();
  await confirmDialog(page, "Restore this Version");

  await expect.poll(() => versions(t, id)).toBe(3);
  expect((await getContact(t.ana, t.groupId, id)).fields.email).toBe("janez.novak@example.si");
  await page.goto(`/groups/${t.groupId}/contacts/${id}/history`);
  await expect(page.getByRole("main").getByText(/from Version 1/)).toBeVisible();
  await expect(page.getByRole("main").getByText("Version 2", { exact: true })).toBeVisible();
});

test("AC-011-04 restoring a Version equal to the current one: This Version is the same as the current one.", async ({
  page,
}) => {
  const t = await team();
  const id = await janezAtVersion2(t);
  const v2 = await getContact(t.marko, t.groupId, id);
  expect(
    (
      await api(t.marko, "POST", `/groups/${t.groupId}/contacts/${id}/versions/1/restore`, {
        versionNumber: v2.versionNumber,
      })
    ).status,
  ).toBe(200);
  await signIn(page, t.marko);

  await page.goto(`/groups/${t.groupId}/contacts/${id}/history/1`);
  await page.getByRole("button", { name: "Restore this Version" }).click();
  await confirmDialog(page, "Restore this Version");

  await expect(page.getByText("This Version is the same as the current one.")).toBeVisible();
  expect(await versions(t, id)).toBe(3);
});

test("EX-011-03 cancelling Restore this Version changes nothing", async ({ page }) => {
  const t = await team();
  const id = await janezAtVersion2(t);
  await signIn(page, t.marko);

  await page.goto(`/groups/${t.groupId}/contacts/${id}/history/1`);
  await page.getByRole("button", { name: "Restore this Version" }).click();
  await cancelDialog(page);

  expect(await versions(t, id)).toBe(2);
  expect((await getContact(t.ana, t.groupId, id)).fields.email).toBe("janez@novak.si");
});
