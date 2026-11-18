import { render, screen } from "@testing-library/react";
import { createMemoryRouter, RouterProvider } from "react-router";
import { describe, expect, it, vi } from "vitest";
import { AuthProvider } from "./AuthProvider";
import { onAccountChange } from "./firebase";
import { GuestOnly, RequireAuth } from "./RequireAuth";

// Firebase cannot start in this file: /config.json is not readable.
vi.mock("./runtimeConfig", () => ({
  loadRuntimeConfig: vi.fn(async () => {
    throw new Error("/config.json answered 404");
  }),
}));
vi.mock("./firebase", () => ({ startAuth: vi.fn(), onAccountChange: vi.fn() }));

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
    ],
    { initialEntries: [path] },
  );
  render(
    <AuthProvider>
      <RouterProvider router={router} />
    </AuthProvider>,
  );
}

describe("Firebase cannot start (UI-14, ADR-05)", () => {
  it("UI-14 a protected page shows the error state instead of loading for ever", async () => {
    renderAt("/");

    expect((await screen.findByRole("alert")).textContent).toBe("Nekaj je šlo narobe.");
    expect(screen.queryByRole("status")).toBeNull();
    expect(screen.queryByText("signed-in page")).toBeNull();
    expect(onAccountChange).not.toHaveBeenCalled();
  });

  it("UI-14 the sign-in screen shows the error state too", async () => {
    renderAt("/sign-in");

    expect((await screen.findByRole("alert")).textContent).toBe("Nekaj je šlo narobe.");
    expect(screen.queryByText("sign-in page")).toBeNull();
  });
});
