import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createMemoryRouter, RouterProvider } from "react-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "../../api/apiClient";
import { ApiError } from "../../api/problem";
import { signOutAccount } from "../../auth/firebase";
import i18n from "../../i18n";
import { AccountPage } from "./AccountPage";

vi.mock("../../api/apiClient", () => ({ apiRequest: vi.fn() }));
vi.mock("../../auth/firebase", () => ({ signOutAccount: vi.fn(async () => undefined) }));

const MARKO = {
  id: "u2",
  email: "marko.kranjc@gmail.com",
  displayName: "Marko Kranjc",
  language: "en",
  emailVerified: true,
};

/** Answers GET /me with Marko, PATCH with the change and DELETE with the given result. */
function answer(deleted: unknown = undefined) {
  vi.mocked(apiRequest).mockImplementation(
    async (method: string, _path: string, body?: unknown) => {
      if (method === "PATCH") return { ...MARKO, ...(body as object) };
      if (method === "DELETE") {
        if (deleted instanceof Error) throw deleted;
        return deleted;
      }
      return MARKO;
    },
  );
}

function renderPage() {
  const router = createMemoryRouter(
    [
      { path: "/account", element: <AccountPage /> },
      { path: "/sign-in", element: <p>Sign-in screen</p> },
    ],
    { initialEntries: ["/account"] },
  );
  render(
    <QueryClientProvider
      client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}
    >
      <RouterProvider router={router} />
    </QueryClientProvider>,
  );
}

const confirmLabel = "Type your e-mail address to confirm";

beforeEach(async () => {
  vi.mocked(apiRequest).mockReset();
  vi.mocked(signOutAccount).mockClear();
  answer();
  await i18n.changeLanguage("en");
});
afterEach(async () => {
  await i18n.changeLanguage("sl");
});

describe("WF-19 Account (US-020, US-021)", () => {
  it("EX-021-01 Delete stays disabled until the typed e-mail matches, ignoring case", async () => {
    const user = userEvent.setup();
    renderPage();
    const typed = await screen.findByLabelText(confirmLabel, { exact: false });
    const deleteButton = screen.getByRole("button", { name: "Delete", exact: true });

    expect((deleteButton as HTMLButtonElement).disabled).toBe(true);
    await user.type(typed, "marko@example.si");
    expect((deleteButton as HTMLButtonElement).disabled).toBe(true);
    await user.clear(typed);
    await user.type(typed, "Marko.Kranjc@gmail.com ");
    expect((deleteButton as HTMLButtonElement).disabled).toBe(false);
    expect(apiRequest).not.toHaveBeenCalledWith("DELETE", "/me", expect.anything());
  });

  it("AC-021-01 Delete deletes the account with the typed e-mail and signs out", async () => {
    const user = userEvent.setup();
    renderPage();
    await user.type(
      await screen.findByLabelText(confirmLabel, { exact: false }),
      "marko.kranjc@gmail.com",
    );

    await user.click(screen.getByRole("button", { name: "Delete", exact: true }));

    expect(await screen.findByText("Sign-in screen")).toBeTruthy();
    expect(apiRequest).toHaveBeenCalledWith("DELETE", "/me", {
      confirmEmail: "marko.kranjc@gmail.com",
    });
    expect(signOutAccount).toHaveBeenCalled();
  });

  it("AC-021-02 an Owner is told to delete the Groups first and stays signed in", async () => {
    answer(new ApiError(409, { code: "account.ownsGroups", params: { names: "Konferenca 2025" } }));
    const user = userEvent.setup();
    renderPage();
    await user.type(
      await screen.findByLabelText(confirmLabel, { exact: false }),
      "marko.kranjc@gmail.com",
    );

    await user.click(screen.getByRole("button", { name: "Delete", exact: true }));

    expect((await screen.findByRole("alert")).textContent).toBe(
      "Delete your Groups first: Konferenca 2025.",
    );
    expect(signOutAccount).not.toHaveBeenCalled();
  });

  it("R-020-02 the language is chosen in the Language section", async () => {
    const user = userEvent.setup();
    renderPage();
    const language = await screen.findByRole("combobox", { name: "Language" });

    await user.selectOptions(language, "Slovenščina");

    await waitFor(() =>
      expect(apiRequest).toHaveBeenCalledWith("PATCH", "/me", { language: "sl" }),
    );
  });

  it("UI-14 shows the error state with Try again when the User cannot be read", async () => {
    vi.mocked(apiRequest).mockRejectedValue(new ApiError(500, {}));
    renderPage();

    expect(await screen.findByRole("button", { name: "Try again" })).toBeTruthy();
  });
});
