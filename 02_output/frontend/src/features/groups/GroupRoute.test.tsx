import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import { createMemoryRouter, RouterProvider } from "react-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "../../api/apiClient";
import i18n from "../../i18n";
import { rememberGroup } from "./currentGroup";
import { GroupRoute } from "./GroupRoute";
import { GroupStart } from "./GroupStart";

vi.mock("../../api/apiClient", () => ({ apiRequest: vi.fn() }));

const marko = { id: "u-marko", displayName: "Marko Horvat", language: "en" };
const alfa = { id: "g2", name: "Podjetje Alfa", role: "VIEWER" };

function renderAt(path: string, groups: unknown[]) {
  vi.mocked(apiRequest).mockImplementation(async (_method: string, p: string) =>
    p === "/groups" ? groups : marko,
  );
  const router = createMemoryRouter(
    [
      { path: "/", element: <GroupStart /> },
      {
        path: "/groups/:groupId",
        element: <GroupRoute />,
        children: [{ path: "contacts", element: <p>contact list</p> }],
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

describe("Group routes (R-002-05, BR-02)", () => {
  it("EX-002-06 the Group selected last is gone and no Group is left: You have no Groups yet.", async () => {
    rememberGroup(marko.id, "g1");
    const router = renderAt("/groups/g1/contacts", []);

    expect(
      await screen.findByRole("heading", { level: 1, name: "You have no Groups yet." }),
    ).toBeTruthy();
    expect(router.state.location.pathname).toBe("/");
  });

  it("EX-002-06 the Group selected last is gone: the first Group of the switcher opens", async () => {
    rememberGroup(marko.id, "g1");
    const router = renderAt("/groups/g1/contacts", [alfa]);

    await waitFor(() => expect(router.state.location.pathname).toBe("/groups/g2/contacts"));
    expect(screen.getByText("contact list")).toBeTruthy();
  });

  it("BR-02 a Group the User never selected and is not a Member of is not found", async () => {
    rememberGroup(marko.id, "g2");
    const router = renderAt("/groups/g9/contacts", [alfa]);

    expect(await screen.findByText("This page cannot be found.")).toBeTruthy();
    expect(router.state.location.pathname).toBe("/groups/g9/contacts");
  });
});
