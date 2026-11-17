import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createMemoryRouter, RouterProvider } from "react-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "../../api/apiClient";
import { ApiError } from "../../api/problem";
import i18n from "../../i18n";
import { RecycleBinPage } from "./RecycleBinPage";

vi.mock("../../api/apiClient", () => ({ apiRequest: vi.fn() }));

const JANEZ = {
  contact: { id: "c1", name: "Janez Novak" },
  versionNumber: 4,
  deletedBy: "Marko Kranjc",
  deletedAt: "2026-03-05T10:00:00Z",
  removalAt: "2026-04-04T10:00:00Z",
};

/**
 * GET answers the User's Groups with the Role, and the recycle bin with the given entries; POST
 * answers the restored Contact; DELETE answers nothing.
 */
function answer(entries: unknown[], role = "EDITOR") {
  vi.mocked(apiRequest).mockImplementation((method: string, path: string) => {
    if (method === "GET") {
      return Promise.resolve(path === "/groups" ? [{ id: "g1", name: "K", role }] : entries);
    }
    return Promise.resolve(method === "POST" ? { id: "c1", state: "active" } : undefined);
  });
}

function renderPage() {
  const router = createMemoryRouter(
    [{ path: "/groups/:groupId/recycle-bin", element: <RecycleBinPage /> }],
    { initialEntries: ["/groups/g1/recycle-bin"] },
  );
  render(
    <QueryClientProvider
      client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}
    >
      <RouterProvider router={router} />
    </QueryClientProvider>,
  );
}

beforeEach(async () => {
  vi.mocked(apiRequest).mockReset();
  answer([JANEZ]);
  await i18n.changeLanguage("en");
});
afterEach(async () => {
  await i18n.changeLanguage("sl");
});

describe("WF-11 Recycle bin (US-012)", () => {
  it("AC-012-01 lists name, deleted by, deleted at and removal date in Europe/Ljubljana", async () => {
    renderPage();

    expect(await screen.findByRole("heading", { level: 1, name: "Recycle bin" })).toBeTruthy();
    expect(apiRequest).toHaveBeenCalledWith("GET", "/groups/g1/recycle-bin");
    const row = await screen.findByRole("row", { name: /Janez Novak/ });
    expect(row.textContent).toContain("Marko Kranjc");
    expect(row.textContent).toContain("05.03.2026 11:00");
    expect(row.textContent).toContain("04.04.2026 12:00");
  });

  it("AC-012-02 Restore sends the Version read and reads the recycle bin again", async () => {
    renderPage();
    const row = await screen.findByRole("row", { name: /Janez Novak/ });
    answer([]);

    await userEvent.click(within(row).getByRole("button", { name: "Restore" }));

    expect(apiRequest).toHaveBeenCalledWith("POST", "/groups/g1/recycle-bin/c1/restore", {
      versionNumber: 4,
    });
    expect(await screen.findByText("The recycle bin is empty.")).toBeTruthy();
  });

  it("R-012-04 a refused restore shows This Contact can no longer be restored.", async () => {
    renderPage();
    const row = await screen.findByRole("row", { name: /Janez Novak/ });
    vi.mocked(apiRequest).mockRejectedValueOnce(
      new ApiError(409, { code: "contact.noLongerRestorable" }),
    );

    await userEvent.click(within(row).getByRole("button", { name: "Restore" }));

    expect((await screen.findByRole("alert")).textContent).toBe(
      "This Contact can no longer be restored.",
    );
  });

  it("UI-14 an empty recycle bin says so", async () => {
    answer([]);
    renderPage();

    expect(await screen.findByText("The recycle bin is empty.")).toBeTruthy();
    expect(screen.queryByRole("table")).toBeNull();
  });

  it("AC-012-05 a Viewer is told this is not allowed", async () => {
    vi.mocked(apiRequest).mockRejectedValue(new ApiError(403, { code: "auth.forbidden" }));
    renderPage();

    expect(await screen.findByText("You are not allowed to do this.")).toBeTruthy();
  });

  it("UI-14 an error shows Something went wrong. and Try again asks again", async () => {
    let calls = 0;
    vi.mocked(apiRequest).mockImplementation((_method: string, path: string) =>
      path === "/groups/g1/recycle-bin" && calls++ === 0
        ? Promise.reject(new Error("down"))
        : Promise.resolve(path === "/groups" ? [] : [JANEZ]),
    );
    renderPage();

    await userEvent.click(await screen.findByRole("button", { name: "Try again" }));

    expect(await screen.findByRole("row", { name: /Janez Novak/ })).toBeTruthy();
  });
});

describe("WF-11 Delete permanently (US-013)", () => {
  it("BR-04 only the Owner sees Delete permanently", async () => {
    renderPage();
    const row = await screen.findByRole("row", { name: /Janez Novak/ });

    expect(within(row).queryByRole("button", { name: "Delete permanently" })).toBeNull();
  });

  it("AC-013-01 the Owner confirms and the recycle bin is read again", async () => {
    answer([JANEZ], "OWNER");
    renderPage();
    const row = await screen.findByRole("row", { name: /Janez Novak/ });
    await userEvent.click(await within(row).findByRole("button", { name: "Delete permanently" }));
    const dialog = await screen.findByRole("alertdialog");
    expect(within(dialog).getByText("Delete Janez Novak permanently?")).toBeTruthy();
    expect(within(dialog).getByText("Delete permanently. This cannot be undone.")).toBeTruthy();
    answer([], "OWNER");

    await userEvent.click(within(dialog).getByRole("button", { name: "Delete permanently" }));

    expect(apiRequest).toHaveBeenCalledWith("DELETE", "/groups/g1/recycle-bin/c1");
    expect(await screen.findByText("The recycle bin is empty.")).toBeTruthy();
    expect(screen.queryByRole("alertdialog")).toBeNull();
  });

  it("EX-013-01 Cancel changes nothing", async () => {
    answer([JANEZ], "OWNER");
    renderPage();
    const row = await screen.findByRole("row", { name: /Janez Novak/ });
    await userEvent.click(await within(row).findByRole("button", { name: "Delete permanently" }));

    await userEvent.click(
      within(await screen.findByRole("alertdialog")).getByRole("button", { name: "Cancel" }),
    );

    expect(apiRequest).not.toHaveBeenCalledWith("DELETE", expect.anything());
    expect(screen.getByRole("row", { name: /Janez Novak/ })).toBeTruthy();
  });

  it("R-013-03 a refused delete shows its message in the confirmation", async () => {
    answer([JANEZ], "OWNER");
    renderPage();
    const row = await screen.findByRole("row", { name: /Janez Novak/ });
    await userEvent.click(await within(row).findByRole("button", { name: "Delete permanently" }));
    const dialog = await screen.findByRole("alertdialog");
    vi.mocked(apiRequest).mockRejectedValueOnce(
      new ApiError(409, { code: "contact.notInRecycleBin" }),
    );

    await userEvent.click(within(dialog).getByRole("button", { name: "Delete permanently" }));

    expect((await within(dialog).findByRole("alert")).textContent).toBe(
      "Move the Contact to the recycle bin first.",
    );
  });
});
