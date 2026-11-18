import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createMemoryRouter, RouterProvider } from "react-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import i18n from "../../i18n";
import { signUpWithEmail } from "../../auth/firebase";
import { SignInPage } from "./SignInPage";
import { SignUpPage } from "./SignUpPage";

vi.mock("../../auth/firebase", async (original) => ({
  ...(await original<typeof import("../../auth/firebase")>()),
  signUpWithEmail: vi.fn(async () => undefined),
}));

function renderAt(path: string) {
  const router = createMemoryRouter(
    [
      { path: "/sign-in", element: <SignInPage /> },
      { path: "/sign-up", element: <SignUpPage /> },
    ],
    { initialEntries: [path] },
  );
  render(<RouterProvider router={router} />);
  return router;
}

async function submit(email: string, password: string) {
  await userEvent.type(screen.getByLabelText("E-mail"), email);
  await userEvent.type(screen.getByLabelText("Password"), password);
  await userEvent.click(screen.getByRole("button", { name: "Create an account" }));
}

beforeEach(async () => {
  await i18n.changeLanguage("en");
});
afterEach(async () => {
  vi.mocked(signUpWithEmail).mockReset();
  await i18n.changeLanguage("sl");
});

describe("WF-02 Create an account (US-001)", () => {
  it("R-001-01 creates an account with e-mail and password", async () => {
    renderAt("/sign-up");
    expect(screen.getByRole("heading", { level: 1, name: "Create an account" })).toBeTruthy();

    await submit("ana.kovac@example.si", "Kontakti#2026");

    expect(signUpWithEmail).toHaveBeenCalledWith("ana.kovac@example.si", "Kontakti#2026");
  });

  it("EX-001-05 an e-mail that already has an account", async () => {
    vi.mocked(signUpWithEmail).mockRejectedValue({ code: "auth/email-already-in-use" });
    renderAt("/sign-up");

    await submit("ana.kovac@example.si", "Kontakti#2026");

    expect((await screen.findByRole("alert")).textContent).toBe(
      "An account with this e-mail already exists.",
    );
  });

  it("R-001-01 links back to Sign in", async () => {
    const router = renderAt("/sign-up");

    await userEvent.click(screen.getByRole("link", { name: "Sign in" }));

    expect(router.state.location.pathname).toBe("/sign-in");
  });
  it("WF-02 asks who already has an account and links to Sign in", () => {
    renderAt("/sign-up");

    expect(screen.getByRole("link", { name: "Sign in" }).parentElement?.textContent).toBe(
      "Already have an account? Sign in",
    );
  });
});
