import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createMemoryRouter, RouterProvider } from "react-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import i18n from "../../i18n";
import { type AuthState, useAuth } from "../../auth/AuthProvider";
import { signOutAccount } from "../../auth/firebase";
import { VerifyEmailPage } from "./VerifyEmailPage";

vi.mock("../../auth/AuthProvider", () => ({ useAuth: vi.fn() }));
vi.mock("../../auth/firebase", () => ({ signOutAccount: vi.fn(async () => undefined) }));

const refresh = vi.fn(async () => undefined);

function renderAs(state: AuthState) {
  vi.mocked(useAuth).mockReturnValue({ state, refresh });
  const router = createMemoryRouter(
    [
      { path: "/verify-email", element: <VerifyEmailPage /> },
      { path: "/sign-in", element: <p>sign-in page</p> },
      { path: "/", element: <p>home page</p> },
    ],
    { initialEntries: ["/verify-email"] },
  );
  render(<RouterProvider router={router} />);
  return router;
}

const unverified: AuthState = {
  status: "signedIn",
  account: { email: "ana.kovac@example.si", mustVerifyEmail: true },
};

beforeEach(async () => {
  await i18n.changeLanguage("en");
});
afterEach(async () => {
  refresh.mockClear();
  await i18n.changeLanguage("sl");
});

describe("WF-03 Confirm your e-mail (R-001-05)", () => {
  it("AC-001-05 shows the notice and where the link was sent", () => {
    renderAs(unverified);

    expect(screen.getByRole("heading", { level: 1, name: "Confirm your e-mail" })).toBeTruthy();
    expect(screen.getByText("Confirm your e-mail address: we sent you a link.")).toBeTruthy();
    expect(screen.getByText(/ana\.kovac@example\.si/)).toBeTruthy();
  });

  it("R-001-05 I confirmed my e-mail reads the account again", async () => {
    renderAs(unverified);

    await userEvent.click(screen.getByRole("button", { name: "I confirmed my e-mail" }));

    expect(refresh).toHaveBeenCalled();
  });

  it("R-001-06 Sign out", async () => {
    renderAs(unverified);

    await userEvent.click(screen.getByRole("button", { name: "Sign out" }));

    expect(signOutAccount).toHaveBeenCalled();
  });

  it("R-001-05 a confirmed account goes on to the product", () => {
    const router = renderAs({
      status: "signedIn",
      account: { email: "ana.kovac@example.si", mustVerifyEmail: false },
    });

    expect(router.state.location.pathname).toBe("/");
  });

  it("R-001-06 without an account it shows the sign-in screen", () => {
    const router = renderAs({ status: "signedOut" });

    expect(router.state.location.pathname).toBe("/sign-in");
  });
  it("UI-14 while the account is read it shows Loading …", () => {
    const router = renderAs({ status: "loading" });

    expect(screen.getByRole("status").textContent).toBe("Loading …");
    expect(router.state.location.pathname).toBe("/verify-email");
  });

  it("UI-12 I confirmed my e-mail waits while the account is read, then is available again", async () => {
    let finish: () => void = () => undefined;
    refresh.mockReturnValueOnce(
      new Promise<undefined>((resolve) => {
        finish = () => resolve(undefined);
      }),
    );
    renderAs(unverified);
    const button = screen.getByRole<HTMLButtonElement>("button", { name: "I confirmed my e-mail" });

    await userEvent.click(button);

    expect(button.disabled).toBe(true);
    finish();
    await waitFor(() => expect(button.disabled).toBe(false));
  });
});
