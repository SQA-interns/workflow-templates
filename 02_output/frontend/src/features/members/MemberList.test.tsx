import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "../../api/apiClient";
import { ApiError } from "../../api/problem";
import i18n from "../../i18n";
import { MemberList } from "./MemberList";

vi.mock("../../api/apiClient", () => ({ apiRequest: vi.fn() }));

const ANA = {
  userId: "u1",
  displayName: "Ana Kovač",
  email: "ana.kovac@example.si",
  role: "OWNER",
};
const MARKO = {
  userId: "u2",
  displayName: "Marko Kranjc",
  email: "marko.kranjc@gmail.com",
  role: "EDITOR",
};
const PETRA = {
  userId: "u3",
  displayName: "Petra Horvat",
  email: "petra.horvat@example.si",
  role: "VIEWER",
};

/** GET answers the Members, PATCH the Member with the requested Role, DELETE nothing. */
function answer(members: unknown[]) {
  vi.mocked(apiRequest).mockImplementation((method: string, _path: string, body?: unknown) => {
    if (method === "GET") return Promise.resolve(members);
    if (method === "PATCH") return Promise.resolve({ ...MARKO, ...(body as object) });
    return Promise.resolve(undefined);
  });
}

function renderList() {
  render(
    <QueryClientProvider
      client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}
    >
      <MemberList groupId="g1" />
    </QueryClientProvider>,
  );
}

const row = (name: string) =>
  screen.findByRole("row", { name: (accessibleName) => accessibleName.includes(name) });
const getCalls = () => vi.mocked(apiRequest).mock.calls.filter(([m]) => m === "GET");

beforeEach(async () => {
  vi.mocked(apiRequest).mockReset();
  answer([ANA, MARKO, PETRA]);
  await i18n.changeLanguage("en");
});
afterEach(async () => {
  await i18n.changeLanguage("sl");
});

describe("WF-13 Members (US-005)", () => {
  it("AC-005-01 lists every Member with e-mail and Role; the Owner's Role cannot be changed", async () => {
    renderList();

    const ana = await row("Ana Kovač");
    expect(ana.textContent).toContain(ANA.email);
    expect(ana.textContent).toContain("Owner");
    expect(within(ana).queryByRole("combobox")).toBeNull();
    expect(within(ana).queryByRole("button", { name: "Remove from Group" })).toBeNull();
    const marko = await row("Marko Kranjc");
    expect(marko.textContent).toContain(MARKO.email);
    expect(within(marko).getByRole("combobox", { name: "Role of Marko Kranjc" })).toHaveProperty(
      "value",
      "EDITOR",
    );
    const petra = await row("Petra Horvat");
    expect(within(petra).getByRole("combobox")).toHaveProperty("value", "VIEWER");
    expect(apiRequest).toHaveBeenCalledWith("GET", "/groups/g1/members");
  });

  it("R-005-02 choosing another Role changes it at once", async () => {
    renderList();
    const marko = await row("Marko Kranjc");

    await userEvent.selectOptions(within(marko).getByRole("combobox"), "Viewer");

    expect(apiRequest).toHaveBeenCalledWith("PATCH", "/groups/g1/members/u2", { role: "VIEWER" });
    await waitFor(() =>
      expect(within(marko).getByRole("combobox")).toHaveProperty("value", "VIEWER"),
    );
  });

  it("R-005-05 a refused Role change shows its message", async () => {
    renderList();
    const marko = await row("Marko Kranjc");
    vi.mocked(apiRequest).mockRejectedValueOnce(
      new ApiError(409, { code: "member.ownerRequired" }),
    );

    await userEvent.selectOptions(within(marko).getByRole("combobox"), "Viewer");

    expect((await screen.findByRole("alert")).textContent).toBe("A Group always has one Owner.");
  });

  it("UI-14 the Members cannot be read: error with Try again", async () => {
    vi.mocked(apiRequest).mockRejectedValue(new Error("offline"));
    renderList();

    await userEvent.click(await screen.findByRole("button", { name: "Try again" }));

    expect(getCalls().length).toBeGreaterThan(1);
  });
});
