import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
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
      <UserMenu />
    </QueryClientProvider>,
  );
  return client;
}

beforeEach(async () => {
  vi.mocked(apiRequest).mockResolvedValue({
    id: "u1",
    email: "ana.kovac@example.si",
    displayName: "Ana Kovač",
    language: "sl",
    emailVerified: true,
  });
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
