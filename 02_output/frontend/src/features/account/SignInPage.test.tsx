import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createMemoryRouter, RouterProvider } from "react-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import i18n from "../../i18n";
import { signInWithEmail, signInWithGoogle } from "../../auth/firebase";
import { SignInPage } from "./SignInPage";
import { SignUpPage } from "./SignUpPage";

vi.mock("../../auth/firebase", async (original) => ({
  ...(await original<typeof import("../../auth/firebase")>()),
  signInWithEmail: vi.fn(async () => undefined),
  signInWithGoogle: vi.fn(async () => undefined),
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
  await userEvent.click(screen.getByRole("button", { name: "Sign in" }));
}

beforeEach(async () => {
  await i18n.changeLanguage("en");
});
afterEach(async () => {
  vi.mocked(signInWithEmail).mockReset();
  vi.mocked(signInWithGoogle).mockReset();
  await i18n.changeLanguage("sl");
});

describe("WF-01 Sign in (US-001)", () => {
  it("R-001-01 signs in with e-mail and password", async () => {
    renderAt("/sign-in");
    expect(screen.getByRole("heading", { level: 1, name: "Sign in" })).toBeTruthy();

    await submit("ana.kovac@example.si", "Kontakti#2026");

    expect(signInWithEmail).toHaveBeenCalledWith("ana.kovac@example.si", "Kontakti#2026");
    expect(screen.queryByRole("alert")).toBeNull();
  });

  it("EX-001-01 wrong password: Wrong e-mail or password.", async () => {
    vi.mocked(signInWithEmail).mockRejectedValue({ code: "auth/invalid-credential" });
    renderAt("/sign-in");

    await submit("ana.kovac@example.si", "Napacno#2026");

    expect((await screen.findByRole("alert")).textContent).toBe("Wrong e-mail or password.");
  });

  it("R-001-01 Sign in with Google", async () => {
    renderAt("/sign-in");

    await userEvent.click(screen.getByRole("button", { name: "Sign in with Google" }));

    expect(signInWithGoogle).toHaveBeenCalled();
  });

  it("R-001-01 closing the Google window shows no error", async () => {
    vi.mocked(signInWithGoogle).mockRejectedValue({ code: "auth/popup-closed-by-user" });
    renderAt("/sign-in");

    await userEvent.click(screen.getByRole("button", { name: "Sign in with Google" }));

    expect(screen.queryByRole("alert")).toBeNull();
  });

  it("R-001-01 links to Create an account", async () => {
    const router = renderAt("/sign-in");

    await userEvent.click(screen.getByRole("link", { name: "Create an account" }));

    expect(router.state.location.pathname).toBe("/sign-up");
  });

  it("D-37 the page has a single textbox, the e-mail", () => {
    renderAt("/sign-in");
    expect(screen.getAllByRole("textbox")).toHaveLength(1);
  });
  it("WF-01 names the e-mail form and asks who has no account yet", () => {
    renderAt("/sign-in");

    expect(screen.getByRole("heading", { level: 2, name: "Sign in with e-mail" })).toBeTruthy();
    expect(screen.getByRole("link", { name: "Create an account" }).parentElement?.textContent).toBe(
      "No account yet? Create an account",
    );
  });

  it("UI-12 both sign-in buttons wait while signing in, then are available again", async () => {
    let finish: () => void = () => undefined;
    vi.mocked(signInWithEmail).mockReturnValue(
      new Promise<void>((resolve) => {
        finish = resolve;
      }),
    );
    renderAt("/sign-in");

    await submit("ana.kovac@example.si", "Kontakti#2026");

    const submitButton = screen.getByRole<HTMLButtonElement>("button", { name: "Sign in" });
    const google = screen.getByRole<HTMLButtonElement>("button", { name: "Sign in with Google" });
    expect(submitButton.disabled).toBe(true);
    expect(google.disabled).toBe(true);
    finish();
    await waitFor(() => expect(submitButton.disabled).toBe(false));
    expect(google.disabled).toBe(false);
  });
});
