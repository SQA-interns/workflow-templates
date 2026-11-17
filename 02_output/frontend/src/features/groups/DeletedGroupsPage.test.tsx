import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createMemoryRouter, RouterProvider } from "react-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "../../api/apiClient";
import { ApiError } from "../../api/problem";
import i18n from "../../i18n";
import { DeletedGroupsPage } from "./DeletedGroupsPage";

vi.mock("../../api/apiClient", () => ({ apiRequest: vi.fn() }));

const KONFERENCA = {
  id: "g1",
  name: "Konferenca 2025",
  deletedAt: "2026-03-05T10:00:00Z",
  removalAt: "2026-04-04T10:00:00Z",
};

/** GET answers the given deleted Groups (and the Groups); POST the restored Group. */
function answer(deleted: unknown[]) {
  vi.mocked(apiRequest).mockImplementation((method: string, path: string) => {
    if (method === "GET") return Promise.resolve(path === "/deleted-groups" ? deleted : []);
    return Promise.resolve({ id: "g1", name: "Konferenca 2025", role: "OWNER" });
  });
}

function renderPage() {
  const router = createMemoryRouter(
    [
      { path: "/deleted-groups", element: <DeletedGroupsPage /> },
      { path: "/groups/:groupId/contacts", element: <p>contact list</p> },
    ],
    { initialEntries: ["/deleted-groups"] },
  );
  render(
    <QueryClientProvider
      client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}
    >
      <RouterProvider router={router} />
    </QueryClientProvider>,
  );
  return router;
}

beforeEach(async () => {
  vi.mocked(apiRequest).mockReset();
  answer([KONFERENCA]);
  await i18n.changeLanguage("en");
});
afterEach(async () => {
  await i18n.changeLanguage("sl");
});

describe("WF-15 Deleted Groups (US-003)", () => {
  it("AC-003-03 lists the name and the removal date in Europe/Ljubljana", async () => {
    renderPage();

    expect(await screen.findByRole("heading", { level: 1, name: "Deleted Groups" })).toBeTruthy();
    expect(apiRequest).toHaveBeenCalledWith("GET", "/deleted-groups");
    const row = await screen.findByRole("row", { name: /Konferenca 2025/ });
    expect(row.textContent).toContain("04.04.2026 12:00");
  });

  it("AC-003-04 Restore restores the Group and opens its Contact list", async () => {
    const router = renderPage();
    const row = await screen.findByRole("row", { name: /Konferenca 2025/ });

    await userEvent.click(within(row).getByRole("button", { name: "Restore" }));

    expect(apiRequest).toHaveBeenCalledWith("POST", "/deleted-groups/g1/restore");
    await waitFor(() => expect(router.state.location.pathname).toBe("/groups/g1/contacts"));
  });

  it("R-003-06 a refused restore: This Group can no longer be restored.", async () => {
    renderPage();
    const row = await screen.findByRole("row", { name: /Konferenca 2025/ });
    vi.mocked(apiRequest).mockImplementation((method: string) =>
      method === "POST"
        ? Promise.reject(new ApiError(409, { code: "group.noLongerRestorable" }))
        : Promise.resolve([KONFERENCA]),
    );

    await userEvent.click(within(row).getByRole("button", { name: "Restore" }));

    expect((await screen.findByRole("alert")).textContent).toBe(
      "This Group can no longer be restored.",
    );
  });

  it("UI-14 no deleted Groups: No deleted Groups.", async () => {
    answer([]);
    renderPage();

    expect(await screen.findByText("No deleted Groups.")).toBeTruthy();
  });
});
