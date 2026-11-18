import { act, render, screen } from "@testing-library/react";
import { createMemoryRouter, RouterProvider } from "react-router";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { Account } from "./firebase";
import { onAccountChange, refreshAccount } from "./firebase";
import { AuthProvider, useAuth } from "./AuthProvider";
import { GuestOnly, RequireAuth } from "./RequireAuth";

let emit: (account: Account | null) => void = () => undefined;
const unsubscribe = vi.hoisted(() => vi.fn());

vi.mock("./runtimeConfig", () => ({
  loadRuntimeConfig: vi.fn(async () => ({
    firebaseApiKey: "k",
    firebaseAuthDomain: "d",
    firebaseProjectId: "p",
  })),
}));
vi.mock("./firebase", () => ({
  startAuth: vi.fn(),
  onAccountChange: vi.fn((callback: (account: Account | null) => void) => {
    emit = callback;
    return unsubscribe;
  }),
  refreshAccount: vi.fn(),
}));

function renderAt(path: string) {
  const router = createMemoryRouter(
    [
      {
        path: "/",
        element: (
          <RequireAuth>
            <p>signed-in page</p>
          </RequireAuth>
        ),
      },
      {
        path: "/sign-in",
        element: (
          <GuestOnly>
            <p>sign-in page</p>
          </GuestOnly>
        ),
      },
      { path: "/verify-email", element: <p>verify page</p> },
    ],
    { initialEntries: [path] },
  );
  render(
    <AuthProvider>
      <RouterProvider router={router} />
    </AuthProvider>,
  );
  return router;
}

async function signedInAs(account: Account | null) {
  await vi.waitFor(() => expect(emit).not.toBe(undefined));
  await act(async () => emit(account));
}

beforeEach(() => {
  emit = () => undefined;
});

describe("route guard (R-001-03, R-001-05, R-001-06)", () => {
  it("UI-14 shows the loading state until Firebase knows the account", () => {
    renderAt("/");
    expect(screen.getByRole("status").textContent).toBe("Nalaganje …");
  });

  it("R-001-06 a signed-out visitor is sent to the sign-in screen", async () => {
    const router = renderAt("/");
    await signedInAs(null);

    expect(await screen.findByText("sign-in page")).toBeTruthy();
    expect(router.state.location.pathname).toBe("/sign-in");
  });

  it("R-001-05 an unverified e-mail account is sent to the confirm screen", async () => {
    const router = renderAt("/");
    await signedInAs({ email: "ana.kovac@example.si", mustVerifyEmail: true });

    expect(await screen.findByText("verify page")).toBeTruthy();
    expect(router.state.location.pathname).toBe("/verify-email");
  });

  it("R-001-01 a signed-in User sees the page", async () => {
    renderAt("/");
    await signedInAs({ email: "ana.kovac@example.si", mustVerifyEmail: false });

    expect(await screen.findByText("signed-in page")).toBeTruthy();
  });

  it("R-001-01 a signed-in User who opens the sign-in screen goes to the product", async () => {
    const router = renderAt("/sign-in");
    await signedInAs({ email: "ana.kovac@example.si", mustVerifyEmail: false });

    expect(await screen.findByText("signed-in page")).toBeTruthy();
    expect(router.state.location.pathname).toBe("/");
  });

  it("R-001-05 an unverified account on the sign-in screen goes to the confirm screen", async () => {
    renderAt("/sign-in");
    await signedInAs({ email: "ana.kovac@example.si", mustVerifyEmail: true });

    expect(await screen.findByText("verify page")).toBeTruthy();
  });
});

/** Shows the account state and offers its refresh, like the confirm e-mail screen. */
function AccountState() {
  const { state, refresh } = useAuth();
  return (
    <>
      <p>
        {state.status}
        {state.status === "signedIn" ? ` ${state.account.email ?? ""}` : ""}
      </p>
      <button type="button" onClick={() => void refresh()}>
        refresh
      </button>
    </>
  );
}

describe("account state (R-001-05, R-001-06)", () => {
  it("R-001-06 stops listening to Firebase when the page is left", async () => {
    unsubscribe.mockClear();
    const { unmount } = render(
      <AuthProvider>
        <AccountState />
      </AuthProvider>,
    );
    await vi.waitFor(() => expect(onAccountChange).toHaveBeenCalled());

    unmount();

    expect(unsubscribe).toHaveBeenCalledTimes(1);
  });

  it("left before Firebase started: never listens to Firebase", async () => {
    vi.mocked(onAccountChange).mockClear();
    const { unmount } = render(
      <AuthProvider>
        <AccountState />
      </AuthProvider>,
    );

    expect(() => unmount()).not.toThrow();
    await new Promise((resolve) => setTimeout(resolve, 20));

    expect(onAccountChange).not.toHaveBeenCalled();
  });

  it("R-001-05 refresh reads the account again, e.g. after the e-mail was confirmed", async () => {
    vi.mocked(refreshAccount).mockResolvedValue({
      email: "ana.kovac@example.si",
      mustVerifyEmail: false,
    });
    render(
      <AuthProvider>
        <AccountState />
      </AuthProvider>,
    );
    await signedInAs({ email: "ana.kovac@example.si", mustVerifyEmail: true });

    await act(async () => screen.getByRole("button", { name: "refresh" }).click());

    expect(refreshAccount).toHaveBeenCalled();
    expect(screen.getByText("signedIn ana.kovac@example.si")).toBeTruthy();
  });

  it("R-001-06 refresh without an account signs out", async () => {
    vi.mocked(refreshAccount).mockResolvedValue(null);
    render(
      <AuthProvider>
        <AccountState />
      </AuthProvider>,
    );
    await signedInAs({ email: "ana.kovac@example.si", mustVerifyEmail: true });

    await act(async () => screen.getByRole("button", { name: "refresh" }).click());

    expect(screen.getByText("signedOut")).toBeTruthy();
  });

  it("useAuth outside AuthProvider is a programming error", () => {
    vi.spyOn(console, "error").mockImplementation(() => undefined);

    expect(() => render(<AccountState />)).toThrow("useAuth outside AuthProvider");

    vi.mocked(console.error).mockRestore();
  });
});
