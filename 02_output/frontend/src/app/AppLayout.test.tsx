import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createMemoryRouter, RouterProvider } from "react-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "../api/apiClient";
import { type AuthState, useAuth } from "../auth/AuthProvider";
import { rememberGroup } from "../features/groups/currentGroup";
import { routes } from "./routes";

vi.mock("../auth/AuthProvider", () => ({ useAuth: vi.fn() }));
vi.mock("../api/apiClient", () => ({ apiRequest: vi.fn() }));

const signedIn: AuthState = {
  status: "signedIn",
  account: { email: "ana.kovac@example.si", mustVerifyEmail: false },
};

function renderAt(path: string, state: AuthState = signedIn) {
  vi.mocked(useAuth).mockReturnValue({ state, refresh: async () => undefined });
  const router = createMemoryRouter(routes, { initialEntries: [path] });
  render(
    <QueryClientProvider
      client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}
    >
      <RouterProvider router={router} />
    </QueryClientProvider>,
  );
  return router;
}

const ana = { id: "u-ana", displayName: "Ana Kovač", language: "sl" };
const konferenca = { id: "g1", name: "Konferenca 2025", role: "OWNER" };
const alfa = { id: "g2", name: "Podjetje Alfa", role: "OWNER" };

/** GET /me answers Ana, GET /groups the given Groups. */
function answer(groups: unknown[]) {
  vi.mocked(apiRequest).mockImplementation(async (_method: string, path: string) =>
    path === "/groups" ? groups : ana,
  );
}

beforeEach(() => {
  answer([]);
});
afterEach(() => {
  localStorage.clear();
});

describe("base layout (UI-10, R-020-01)", () => {
  it("R-020-01 shows the Slovenian product name by default", () => {
    renderAt("/");
    expect(screen.getByRole("banner").textContent).toContain("Kontakti");
    expect(screen.getByRole("main")).toBeTruthy();
    expect(screen.getByRole("contentinfo").textContent).toBe("Kontakti");
  });

  it("UI-23 the menu button toggles the navigation", async () => {
    renderAt("/");
    const button = screen.getByRole("button", { name: "Odpri meni" });
    expect(button.getAttribute("aria-expanded")).toBe("false");
    await userEvent.click(button);
    expect(button.getAttribute("aria-expanded")).toBe("true");
  });

  it("BR-02 an unknown route shows the not found page", () => {
    renderAt("/no-such-page");
    expect(screen.getByRole("heading", { level: 1, name: "Ni najdeno" })).toBeTruthy();
  });
});

describe("routes of US-001", () => {
  it("R-001-07 a signed-in User without Groups sees WF-05", async () => {
    renderAt("/");
    expect(
      await screen.findByRole("heading", { level: 1, name: "Nimate še nobene skupine." }),
    ).toBeTruthy();
  });

  it("R-001-03 a signed-out visitor sees the sign-in screen", () => {
    const router = renderAt("/", { status: "signedOut" });
    expect(router.state.location.pathname).toBe("/sign-in");
    expect(screen.getByRole("heading", { level: 1, name: "Prijava" })).toBeTruthy();
    expect(screen.getByRole("banner").textContent).toContain("Kontakti");
  });

  it("R-001-01 /sign-up shows Create an account", () => {
    renderAt("/sign-up", { status: "signedOut" });
    expect(screen.getByRole("heading", { level: 1, name: "Ustvari račun" })).toBeTruthy();
  });

  it("R-001-05 /verify-email shows Confirm your e-mail", () => {
    renderAt("/verify-email", {
      status: "signedIn",
      account: { email: "ana.kovac@example.si", mustVerifyEmail: true },
    });
    expect(
      screen.getByRole("heading", { level: 1, name: "Potrdite svoj e-poštni naslov" }),
    ).toBeTruthy();
  });

  it("R-001-04 the user menu is named by the display name", async () => {
    renderAt("/");
    expect(await screen.findByRole("button", { name: "Ana Kovač" })).toBeTruthy();
  });
});

describe("routes of US-002", () => {
  it("R-002-01 /groups/new shows WF-04 Create a Group", () => {
    renderAt("/groups/new");
    expect(screen.getByRole("heading", { level: 1, name: "Ustvari skupino" })).toBeTruthy();
  });

  it("AC-002-05 signing in opens the Group selected last", async () => {
    answer([konferenca, alfa]);
    rememberGroup("u-ana", "g2");

    const router = renderAt("/");

    await waitFor(() => expect(router.state.location.pathname).toBe("/groups/g2/contacts"));
  });

  it("EX-002-06 a Group no longer available: the first Group of the switcher opens", async () => {
    answer([konferenca, alfa]);
    rememberGroup("u-ana", "g-in-recycle-bin");

    const router = renderAt("/");

    await waitFor(() => expect(router.state.location.pathname).toBe("/groups/g1/contacts"));
  });

  it("R-002-04 the Contact list of a Group is remembered as the Group selected last", async () => {
    answer([konferenca, alfa]);

    renderAt("/groups/g2/contacts");

    await waitFor(() => expect(localStorage.getItem("contacts.lastGroup.u-ana")).toBe("g2"));
  });

  it("BR-02 a Group the User is no Member of shows the not found page", async () => {
    answer([konferenca]);

    renderAt("/groups/other/contacts");

    expect(await screen.findByRole("heading", { level: 1, name: "Ni najdeno" })).toBeTruthy();
    expect(localStorage.getItem("contacts.lastGroup.u-ana")).toBeNull();
  });

  it("UI-14 the Groups cannot be read: error with Try again", async () => {
    vi.mocked(apiRequest).mockImplementation(async (_method: string, path: string) => {
      if (path === "/groups") throw new Error("offline");
      return ana;
    });

    renderAt("/");

    expect((await screen.findByRole("alert")).textContent).toContain("Nekaj je šlo narobe.");
    expect(screen.getByRole("button", { name: "Poskusi znova" })).toBeTruthy();
  });
});

describe("header of US-002", () => {
  it("R-002-04 the header carries the Group switcher with the current Group", async () => {
    answer([konferenca, alfa]);

    renderAt("/groups/g2/contacts");

    const switcher = await screen.findByRole("button", { name: /Izbira skupine/ });
    expect(screen.getByRole("banner").contains(switcher)).toBe(true);
    expect(switcher.textContent).toContain("Podjetje Alfa");
  });
});

describe("routes and header of US-012", () => {
  it("R-012-02 an Owner or Editor gets Recycle bin in the navigation of the Group", async () => {
    answer([konferenca, { ...alfa, role: "EDITOR" }]);

    renderAt("/groups/g2/contacts");

    const navigation = screen.getByRole("navigation", { name: "Glavna navigacija" });
    const link = await within(navigation).findByRole("link", { name: "Koš" });
    expect(link.getAttribute("href")).toBe("/groups/g2/recycle-bin");
  });

  it("R-012-02 a Viewer gets no Recycle bin in the navigation", async () => {
    answer([{ ...konferenca, role: "VIEWER" }]);

    renderAt("/groups/g1/contacts");

    await screen.findByRole("button", { name: /Izbira skupine/ });
    expect(screen.queryByRole("link", { name: "Koš" })).toBeNull();
  });

  it("R-012-02 /groups/:groupId/recycle-bin shows WF-11", async () => {
    vi.mocked(apiRequest).mockImplementation(async (_method: string, path: string) => {
      if (path === "/groups") return [konferenca];
      return path === "/groups/g1/recycle-bin" ? [] : ana;
    });

    renderAt("/groups/g1/recycle-bin");

    expect(await screen.findByRole("heading", { level: 1, name: "Koš" })).toBeTruthy();
    expect(await screen.findByText("Koš je prazen.")).toBeTruthy();
  });
});

describe("routes and header of US-003", () => {
  it("BR-04 only the Owner gets Settings in the navigation of the Group", async () => {
    answer([konferenca, { ...alfa, role: "EDITOR" }]);

    renderAt("/groups/g1/contacts");

    const navigation = screen.getByRole("navigation", { name: "Glavna navigacija" });
    const link = await within(navigation).findByRole("link", { name: "Nastavitve" });
    expect(link.getAttribute("href")).toBe("/groups/g1/settings");
  });

  it("BR-04 an Editor gets no Settings in the navigation", async () => {
    answer([{ ...konferenca, role: "EDITOR" }]);

    renderAt("/groups/g1/contacts");

    await screen.findByRole("link", { name: "Koš" });
    expect(screen.queryByRole("link", { name: "Nastavitve" })).toBeNull();
  });

  it("R-003-01 /groups/:groupId/settings shows WF-14", async () => {
    answer([konferenca]);

    renderAt("/groups/g1/settings");

    expect(
      await screen.findByRole("heading", { level: 1, name: "Nastavitve skupine" }),
    ).toBeTruthy();
  });

  it("R-003-04 the user menu opens WF-15 Deleted Groups", async () => {
    vi.mocked(apiRequest).mockImplementation(async (_method: string, path: string) => {
      if (path === "/groups" || path === "/deleted-groups") return [];
      return ana;
    });
    renderAt("/");

    await userEvent.click(await screen.findByRole("button", { name: /Ana Kovač/ }));
    await userEvent.click(screen.getByRole("link", { name: "Izbrisane skupine" }));

    expect(
      await screen.findByRole("heading", { level: 1, name: "Izbrisane skupine" }),
    ).toBeTruthy();
    expect(await screen.findByText("Ni izbrisanih skupin.")).toBeTruthy();
  });
});
