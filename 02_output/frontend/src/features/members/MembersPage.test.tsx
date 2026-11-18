import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { createMemoryRouter, RouterProvider } from "react-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "../../api/apiClient";
import i18n from "../../i18n";
import { MembersPage } from "./MembersPage";

vi.mock("../../api/apiClient", () => ({ apiRequest: vi.fn() }));

/** GET /groups answers the given Groups; the Members and Invitations are empty. */
function answer(groups: Promise<unknown>) {
  vi.mocked(apiRequest).mockImplementation((_method: string, path: string) =>
    path === "/groups" ? groups : Promise.resolve([]),
  );
}

function renderPage() {
  const router = createMemoryRouter(
    [{ path: "/groups/:groupId/members", element: <MembersPage /> }],
    { initialEntries: ["/groups/g1/members"] },
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

describe("WF-13 Members and Invitations page (US-004, US-005)", () => {
  it("R-005-01 the Owner sees Members, Invite a Member and Open Invitations", async () => {
    answer(Promise.resolve([{ id: "g1", name: "Konferenca 2025", role: "OWNER" }]));
    renderPage();

    expect(await screen.findByRole("heading", { level: 1, name: "Members" })).toBeTruthy();
    expect(screen.getByRole("heading", { name: "Invite a Member" })).toBeTruthy();
    expect(apiRequest).toHaveBeenCalledWith("GET", "/groups/g1/members");
    expect(apiRequest).toHaveBeenCalledWith("GET", "/groups/g1/invitations");
  });

  it("BR-04 an Editor of this Group is refused, whatever Role they have elsewhere", async () => {
    answer(
      Promise.resolve([
        { id: "g0", name: "Lastna", role: "OWNER" },
        { id: "g1", name: "Konferenca 2025", role: "EDITOR" },
      ]),
    );
    renderPage();

    expect((await screen.findByRole("alert")).textContent).toBe("You are not allowed to do this.");
    expect(screen.queryByRole("heading", { name: "Members" })).toBeNull();
  });

  it("BR-04 a User who is not a Member of the Group is refused", async () => {
    answer(Promise.resolve([{ id: "g0", name: "Lastna", role: "OWNER" }]));
    renderPage();

    expect((await screen.findByRole("alert")).textContent).toBe("You are not allowed to do this.");
  });

  it("UI-14 while the Groups load nothing is refused yet", async () => {
    answer(new Promise(() => undefined));
    renderPage();

    expect((await screen.findAllByRole("status")).length).toBeGreaterThan(0);
    expect(screen.queryByRole("alert")).toBeNull();
  });
});
