import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createMemoryRouter, RouterProvider } from "react-router";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "../api/apiClient";
import { type AuthState, useAuth } from "../auth/AuthProvider";
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
    <QueryClientProvider client={new QueryClient()}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  );
  return router;
}

beforeEach(() => {
  vi.mocked(apiRequest).mockResolvedValue({ displayName: "Ana Kovač" });
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
  it("R-001-07 a signed-in User without Groups sees WF-05", () => {
    renderAt("/");
    expect(
      screen.getByRole("heading", { level: 1, name: "Nimate še nobene skupine." }),
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
