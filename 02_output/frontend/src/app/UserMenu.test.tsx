import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import i18n from "../i18n";
import { apiRequest } from "../api/apiClient";
import { signOutAccount } from "../auth/firebase";
import { UserMenu } from "./UserMenu";

vi.mock("../api/apiClient", () => ({ apiRequest: vi.fn() }));
vi.mock("../auth/firebase", () => ({ signOutAccount: vi.fn(async () => undefined) }));

function renderMenu(client = new QueryClient()) {
  render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <UserMenu />
      </MemoryRouter>
    </QueryClientProvider>,
  );
  return client;
}

const ana = {
  id: "u1",
  email: "ana.kovac@example.si",
  displayName: "Ana Kovač",
  language: "en",
  emailVerified: true,
};

beforeEach(async () => {
  vi.mocked(apiRequest).mockReset();
  vi.mocked(apiRequest).mockImplementation(async (method: string, _path: string, body?: unknown) =>
    method === "PATCH" ? { ...ana, ...(body as object) } : ana,
  );
  await i18n.changeLanguage("en");
});
afterEach(async () => {
  await i18n.changeLanguage("sl");
});

describe("user menu (R-001-04, R-001-06)", () => {
  it("R-001-04 the menu button shows the display name of GET /me", async () => {
    renderMenu();

    expect(await screen.findByRole("button", { name: "Ana Kovač" })).toBeTruthy();
    expect(apiRequest).toHaveBeenCalledWith("GET", "/me");
  });

  it("R-001-06 Sign out signs out of Firebase and forgets the User's data", async () => {
    const client = renderMenu();
    await userEvent.click(await screen.findByRole("button", { name: "Ana Kovač" }));

    await userEvent.click(screen.getByRole("button", { name: "Sign out" }));

    expect(signOutAccount).toHaveBeenCalled();
    expect(client.getQueryData(["me"])).toBeUndefined();
  });

  it("UI-23 the menu opens and closes with its button and Escape", async () => {
    renderMenu();
    const button = await screen.findByRole("button", { name: "Ana Kovač" });
    expect(button.getAttribute("aria-expanded")).toBe("false");

    await userEvent.click(button);
    expect(button.getAttribute("aria-expanded")).toBe("true");
    await userEvent.keyboard("{Escape}");

    expect(button.getAttribute("aria-expanded")).toBe("false");
    expect(screen.queryByRole("button", { name: "Sign out" })).toBeNull();
  });
});

describe("language (R-020-01, R-020-02)", () => {
  it("R-020-01 the texts follow the language of GET /me, Slovenian for a new User", async () => {
    vi.mocked(apiRequest).mockResolvedValue({ ...ana, language: "sl" });

    renderMenu();

    await waitFor(() => expect(i18n.language).toBe("sl"));
    expect(document.documentElement.lang).toBe("sl");
  });

  it("R-020-02 choosing Slovenščina saves it and switches every text at once", async () => {
    renderMenu();
    await userEvent.click(await screen.findByRole("button", { name: "Ana Kovač" }));

    await userEvent.click(screen.getByRole("button", { name: "Slovenščina" }));

    expect(apiRequest).toHaveBeenCalledWith("PATCH", "/me", { language: "sl" });
    await waitFor(() => expect(i18n.language).toBe("sl"));
    await userEvent.click(screen.getByRole("button", { name: "Ana Kovač" }));
    expect(screen.getByRole("button", { name: "Slovenščina" }).getAttribute("aria-pressed")).toBe(
      "true",
    );
    expect(screen.getByRole("button", { name: "English" }).getAttribute("aria-pressed")).toBe(
      "false",
    );
  });

  it("D-48 choosing a language closes the menu once it is saved", async () => {
    renderMenu();
    const menu = await screen.findByRole("button", { name: "Ana Kovač" });
    await userEvent.click(menu);

    await userEvent.click(screen.getByRole("button", { name: "Slovenščina" }));

    await waitFor(() => expect(menu.getAttribute("aria-expanded")).toBe("false"));
    expect(screen.queryByRole("button", { name: "Odjava" })).toBeNull();
  });

  it("R-020-02 when saving fails the language stays and the menu says so", async () => {
    vi.mocked(apiRequest).mockImplementation(async (method: string) => {
      if (method === "PATCH") throw new Error("offline");
      return ana;
    });
    renderMenu();
    await userEvent.click(await screen.findByRole("button", { name: "Ana Kovač" }));

    await userEvent.click(screen.getByRole("button", { name: "Slovenščina" }));

    expect((await screen.findByRole("alert")).textContent).toBe("Something went wrong.");
    expect(i18n.language).toBe("en");
  });
});
