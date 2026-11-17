import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, within } from "@testing-library/react";
import { createMemoryRouter, RouterProvider } from "react-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "../../api/apiClient";
import i18n from "../../i18n";
import { DuplicatesPage } from "./DuplicatesPage";

vi.mock("../../api/apiClient", () => ({ apiRequest: vi.fn() }));

const NOVAK = {
  first: { id: "c1", name: "Janez Novak" },
  second: { id: "c2", name: "J. Novak" },
  reasons: ["sameEmail", "samePhone"],
};
const NOVACK = {
  first: { id: "c3", name: "Janez Novack" },
  second: { id: "c4", name: "Janez Novak" },
  reasons: ["similarNameSameCompany"],
};

function renderPage() {
  const router = createMemoryRouter(
    [{ path: "/groups/:groupId/duplicates", element: <DuplicatesPage /> }],
    { initialEntries: ["/groups/g1/duplicates"] },
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
  await i18n.changeLanguage("en");
});
afterEach(async () => {
  await i18n.changeLanguage("sl");
});

describe("WF-17 Possible duplicates (US-017)", () => {
  it("R-017-01 lists each pair with links to both Contacts and every reason", async () => {
    vi.mocked(apiRequest).mockResolvedValue([NOVAK, NOVACK]);

    renderPage();

    expect(
      await screen.findByRole("heading", { level: 1, name: "Possible duplicates" }),
    ).toBeTruthy();
    expect(apiRequest).toHaveBeenCalledWith("GET", "/groups/g1/duplicates");
    const [novak, novack] = await screen.findAllByRole("listitem");
    expect(screen.getAllByRole("listitem")).toHaveLength(2);
    if (!novak || !novack) throw new Error("two pairs expected");
    const first = within(novak);
    expect(first.getByRole("link", { name: "Janez Novak" }).getAttribute("href")).toBe(
      "/groups/g1/contacts/c1",
    );
    expect(first.getByRole("link", { name: "J. Novak" }).getAttribute("href")).toBe(
      "/groups/g1/contacts/c2",
    );
    expect(first.getByText("Same e-mail")).toBeTruthy();
    expect(first.getByText("Same phone")).toBeTruthy();
    expect(within(novack).getByText("Similar name, same company")).toBeTruthy();
  });

  it("EX-017-07 without pairs: No possible duplicates found.", async () => {
    vi.mocked(apiRequest).mockResolvedValue([]);

    renderPage();

    expect(await screen.findByText("No possible duplicates found.")).toBeTruthy();
  });

  it("UI-14 shows loading, then an error with Try again", async () => {
    vi.mocked(apiRequest).mockRejectedValue(new Error("down"));

    renderPage();

    expect(screen.getByRole("status").textContent).toBeTruthy();
    expect(await screen.findByRole("alert")).toBeTruthy();
    expect(screen.getByRole("button", { name: "Try again" })).toBeTruthy();
  });
});
