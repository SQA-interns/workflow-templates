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

/** GET answers the recycle bin with the given entries; POST answers the restored Contact. */
function answer(entries: unknown[]) {
  vi.mocked(apiRequest).mockImplementation((method: string) =>
    Promise.resolve(method === "GET" ? entries : { id: "c1", state: "active" }),
  );
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
    vi.mocked(apiRequest).mockRejectedValueOnce(new Error("down"));
    renderPage();

    await userEvent.click(await screen.findByRole("button", { name: "Try again" }));

    expect(await screen.findByRole("row", { name: /Janez Novak/ })).toBeTruthy();
  });
});
