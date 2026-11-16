import { act, render, screen } from "@testing-library/react";
import { createMemoryRouter, RouterProvider } from "react-router";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { Account } from "./firebase";
import { AuthProvider } from "./AuthProvider";
import { GuestOnly, RequireAuth } from "./RequireAuth";

let emit: (account: Account | null) => void = () => undefined;

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
    return () => undefined;
  }),
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
