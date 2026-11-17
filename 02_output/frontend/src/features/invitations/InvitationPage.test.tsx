import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createMemoryRouter, RouterProvider } from "react-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "../../api/apiClient";
import { ApiError } from "../../api/problem";
import { routes } from "../../app/routes";
import { type AuthState, useAuth } from "../../auth/AuthProvider";
import i18n from "../../i18n";

vi.mock("../../auth/AuthProvider", () => ({ useAuth: vi.fn() }));
vi.mock("../../api/apiClient", () => ({ apiRequest: vi.fn() }));

const TOKEN = "C2o8HooKTFWdfioRPE1ebwAAAAAAAAAAAAAAAAAAAAAA";
const PATH = `/invitations/${TOKEN}`;
const marko: AuthState = {
  status: "signedIn",
  account: { email: "marko.kranjc@gmail.com", mustVerifyEmail: false },
};
const KONFERENCA = { id: "g1", name: "Konferenca 2025", role: "EDITOR" };

/** GET /me, /groups and the Invitation; POST accept answers the Group. */
function answer(invitation: () => Promise<unknown>) {
  vi.mocked(apiRequest).mockImplementation((method: string, path: string) => {
    if (path === `/invitations/${TOKEN}`) return invitation();
    if (method === "POST") return Promise.resolve(KONFERENCA);
    if (path === "/groups") return Promise.resolve([]);
    return Promise.resolve({ id: "u-marko", displayName: "Marko Kranjc", language: "en" });
  });
}

const open = () =>
  Promise.resolve({
    groupName: "Konferenca 2025",
    role: "EDITOR",
    expiresAt: "2026-03-12T09:00:00Z",
  });
const refused = (status: number, code: string) => () =>
  Promise.reject(new ApiError(status, { code }));

function renderAt(path: string, state: AuthState = marko) {
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

beforeEach(async () => {
  vi.mocked(apiRequest).mockReset();
  answer(open);
  await i18n.changeLanguage("en");
});
afterEach(async () => {
  sessionStorage.clear();
  await i18n.changeLanguage("sl");
});

describe("WF-16 Invitation (US-004)", () => {
  it("AC-004-02 shows the Group, the Role and the expiry in Europe/Ljubljana", async () => {
    renderAt(PATH);

    expect(
      await screen.findByRole("heading", { level: 1, name: "Invitation to Konferenca 2025" }),
    ).toBeTruthy();
    expect(screen.getByText("You are invited as Editor.")).toBeTruthy();
    expect(screen.getByText("Expires 12.03.2026 10:00")).toBeTruthy();
  });

  it("AC-004-02 Accept makes the User a Member and opens the Group's Contact list", async () => {
    const router = renderAt(PATH);

    await userEvent.click(await screen.findByRole("button", { name: "Accept" }));

    expect(apiRequest).toHaveBeenCalledWith("POST", `/invitations/${TOKEN}/accept`);
    await waitFor(() => expect(router.state.location.pathname).toBe("/groups/g1/contacts"));
  });

  it("AC-004-03 another e-mail: This Invitation is for another e-mail address.", async () => {
    answer(refused(403, "invitation.otherEmail"));
    renderAt(PATH);

    expect((await screen.findByRole("alert")).textContent).toBe(
      "This Invitation is for another e-mail address.",
    );
    expect(screen.queryByRole("button", { name: "Accept" })).toBeNull();
  });

  it("AC-004-04 R-004-06 an Invitation no longer valid: This Invitation is no longer valid.", async () => {
    answer(refused(409, "invitation.noLongerValid"));
    renderAt(PATH);

    expect((await screen.findByRole("alert")).textContent).toBe(
      "This Invitation is no longer valid.",
    );
  });

  it("R-004-06 a refused Accept shows its message", async () => {
    vi.mocked(apiRequest).mockImplementation((method: string, path: string) => {
      if (method === "POST") return refused(409, "invitation.noLongerValid")();
      if (path === `/invitations/${TOKEN}`) return open();
      return Promise.resolve([]);
    });
    renderAt(PATH);

    await userEvent.click(await screen.findByRole("button", { name: "Accept" }));

    expect((await screen.findByRole("alert")).textContent).toBe(
      "This Invitation is no longer valid.",
    );
  });

  it("UI-14 the Invitation cannot be read: Something went wrong. with Try again", async () => {
    answer(() => Promise.reject(new Error("offline")));
    renderAt(PATH);

    expect((await screen.findByRole("alert")).textContent).toContain("Something went wrong.");
    expect(screen.getByRole("button", { name: "Try again" })).toBeTruthy();
  });

  it("R-004-07 not signed in: asks to sign in or create an account", async () => {
    renderAt(PATH, { status: "signedOut" });

    expect(screen.getByText("Sign in or create an account to see this Invitation.")).toBeTruthy();
    expect(screen.getByRole("link", { name: "Sign in" }).getAttribute("href")).toBe("/sign-in");
    expect(screen.getByRole("link", { name: "Create an account" }).getAttribute("href")).toBe(
      "/sign-up",
    );
    expect(apiRequest).not.toHaveBeenCalled();
  });

  it("R-004-07 after signing in the User comes back to the Invitation", async () => {
    renderAt(PATH, { status: "signedOut" });
    cleanup();

    const router = renderAt("/sign-in");

    expect(router.state.location.pathname).toBe(PATH);
  });

  it("R-004-07 after confirming the e-mail the User comes back to the Invitation", async () => {
    renderAt(PATH, { status: "signedOut" });
    cleanup();

    const router = renderAt("/verify-email");

    expect(router.state.location.pathname).toBe(PATH);
  });

  it("R-004-07 an unconfirmed e-mail is sent to confirm it first", () => {
    const router = renderAt(PATH, {
      status: "signedIn",
      account: { email: "marko.kranjc@gmail.com", mustVerifyEmail: true },
    });

    expect(router.state.location.pathname).toBe("/verify-email");
  });

  it("R-004-07 without a remembered Invitation, sign-in leads to the start", () => {
    const router = renderAt("/sign-in");

    expect(router.state.location.pathname).toBe("/");
  });
});
