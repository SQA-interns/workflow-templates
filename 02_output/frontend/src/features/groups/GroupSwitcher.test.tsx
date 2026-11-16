import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createMemoryRouter, Outlet, RouterProvider } from "react-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "../../api/apiClient";
import i18n from "../../i18n";
import { rememberGroup } from "./currentGroup";
import { GroupSwitcher } from "./GroupSwitcher";

vi.mock("../../api/apiClient", () => ({ apiRequest: vi.fn() }));

const ana = { id: "u-ana", displayName: "Ana Kovač", language: "en" };
const konferenca = { id: "g1", name: "Konferenca 2025", role: "OWNER" };
const alfa = { id: "g2", name: "Podjetje Alfa", role: "VIEWER" };

function renderAt(path: string, groups: unknown[] = [konferenca, alfa]) {
  vi.mocked(apiRequest).mockImplementation(async (_method: string, p: string) =>
    p === "/groups" ? groups : ana,
  );
  const router = createMemoryRouter(
    [
      {
        element: (
          <>
            <GroupSwitcher />
            <Outlet />
          </>
        ),
        children: [
          { path: "/groups/new", element: <p>create</p> },
          { path: "/groups/:groupId/contacts", element: <p>contact list</p> },
        ],
      },
    ],
    { initialEntries: [path] },
  );
  render(
    <QueryClientProvider client={new QueryClient()}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  );
  return router;
}

beforeEach(async () => {
  await i18n.changeLanguage("en");
});
afterEach(async () => {
  localStorage.clear();
  await i18n.changeLanguage("sl");
});

describe("Group switcher (R-002-03, R-002-04)", () => {
  it("AC-002-01 shows the current Group", async () => {
    renderAt("/groups/g1/contacts");

    const switcher = await screen.findByRole("button", { name: /Group switcher/ });
    expect(switcher.textContent).toContain("Konferenca 2025");
    expect(switcher.getAttribute("aria-expanded")).toBe("false");
  });

  it("R-002-03 lists the User's Groups in the order of the API, the current one marked", async () => {
    renderAt("/groups/g1/contacts");

    await userEvent.click(await screen.findByRole("button", { name: /Group switcher/ }));

    const links = screen.getAllByRole("link").map((link) => link.textContent);
    expect(links).toEqual(["Konferenca 2025", "Podjetje Alfa", "Create a Group"]);
    expect(screen.getByRole("link", { name: "Konferenca 2025" }).getAttribute("aria-current")).toBe(
      "true",
    );
    expect(screen.getByRole("link", { name: "Podjetje Alfa" }).getAttribute("aria-current")).toBe(
      null,
    );
  });

  it("AC-002-02 selecting a Group opens its Contact list and closes the switcher", async () => {
    const router = renderAt("/groups/g1/contacts");
    await userEvent.click(await screen.findByRole("button", { name: /Group switcher/ }));

    await userEvent.click(screen.getByRole("link", { name: "Podjetje Alfa" }));

    expect(router.state.location.pathname).toBe("/groups/g2/contacts");
    await waitFor(() =>
      expect(screen.getByRole("button", { name: /Group switcher/ }).textContent).toContain(
        "Podjetje Alfa",
      ),
    );
    expect(screen.queryByRole("link", { name: "Konferenca 2025" })).toBeNull();
  });

  it("R-002-01 Create a Group opens WF-04", async () => {
    const router = renderAt("/groups/g1/contacts");
    await userEvent.click(await screen.findByRole("button", { name: /Group switcher/ }));

    await userEvent.click(screen.getByRole("link", { name: "Create a Group" }));

    expect(router.state.location.pathname).toBe("/groups/new");
  });

  it("R-002-05 outside a Group it shows the Group selected last", async () => {
    rememberGroup("u-ana", "g2");

    renderAt("/groups/new");

    await waitFor(() =>
      expect(screen.getByRole("button", { name: /Group switcher/ }).textContent).toContain(
        "Podjetje Alfa",
      ),
    );
  });

  it("R-001-07 a User without Groups has no switcher (WF-05)", async () => {
    renderAt("/groups/new", []);

    await waitFor(() => expect(apiRequest).toHaveBeenCalledWith("GET", "/groups"));
    expect(screen.queryByRole("button", { name: /Group switcher/ })).toBeNull();
  });

  it("UI-23 Escape closes the switcher", async () => {
    renderAt("/groups/g1/contacts");
    const switcher = await screen.findByRole("button", { name: /Group switcher/ });
    await userEvent.click(switcher);

    await userEvent.keyboard("{Escape}");

    expect(switcher.getAttribute("aria-expanded")).toBe("false");
    expect(screen.queryByRole("link", { name: "Podjetje Alfa" })).toBeNull();
  });
});
