import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "../../api/apiClient";
import { ApiError } from "../../api/problem";
import i18n from "../../i18n";
import { InvitationsSection } from "./InvitationsSection";

vi.mock("../../api/apiClient", () => ({ apiRequest: vi.fn() }));

const MARKO = "marko.kranjc@gmail.com";
const OPEN = { id: "i1", email: MARKO, role: "EDITOR", expiresAt: "2026-03-12T09:00:00Z" };
const LINK = "https://kontakti.example.si/invitations/C2o8HooKTFWdfioRPE1ebwAAAAAAAAAAAAAAAAAAAAAA";

/** GET answers the open Invitations, POST the new one with its link, DELETE nothing. */
function answer(open: unknown[]) {
  vi.mocked(apiRequest).mockImplementation((method: string) => {
    if (method === "GET") return Promise.resolve(open);
    if (method === "POST") return Promise.resolve({ invitation: OPEN, link: LINK });
    return Promise.resolve(undefined);
  });
}

function renderSection() {
  render(
    <QueryClientProvider
      client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}
    >
      <InvitationsSection groupId="g1" />
    </QueryClientProvider>,
  );
}

async function invite(email: string, role = "Editor") {
  await userEvent.type(screen.getByRole("textbox", { name: "E-mail" }), email);
  await userEvent.selectOptions(screen.getByRole("combobox", { name: "Role" }), role);
  await userEvent.click(screen.getByRole("button", { name: "Create Invitation" }));
}

beforeEach(async () => {
  vi.mocked(apiRequest).mockReset();
  answer([]);
  await i18n.changeLanguage("en");
});
afterEach(async () => {
  await i18n.changeLanguage("sl");
});

describe("WF-13 Invite a Member and Open Invitations (US-004)", () => {
  it("AC-004-01 creates the Invitation and shows its link once with Copy link", async () => {
    renderSection();

    await invite(`  ${MARKO} `);

    expect(apiRequest).toHaveBeenCalledWith("POST", "/groups/g1/invitations", {
      email: MARKO,
      role: "EDITOR",
    });
    expect(await screen.findByText(LINK)).toBeTruthy();
    expect(screen.getByRole("button", { name: "Copy link" })).toBeTruthy();
    expect(screen.getByText(/shown only once/)).toBeTruthy();
    await waitFor(() =>
      expect(vi.mocked(apiRequest).mock.calls.filter(([m]) => m === "GET")).toHaveLength(2),
    );
  });

  it("AC-004-01 Copy link copies the link and says so", async () => {
    const user = userEvent.setup();
    renderSection();
    await invite(MARKO);

    await user.click(await screen.findByRole("button", { name: "Copy link" }));

    expect(await navigator.clipboard.readText()).toBe(LINK);
    expect(screen.getByRole("status").textContent).toBe("Link copied.");
  });

  it("R-004-01 Viewer can be chosen; Owner cannot", async () => {
    renderSection();

    const options = within(screen.getByRole("combobox", { name: "Role" })).getAllByRole("option");
    expect(options.map((option) => option.textContent)).toEqual(["Editor", "Viewer"]);
    await invite(MARKO, "Viewer");
    expect(apiRequest).toHaveBeenCalledWith("POST", "/groups/g1/invitations", {
      email: MARKO,
      role: "VIEWER",
    });
  });

  it("EX-004-06 an invalid e-mail: Enter a valid e-mail address., nothing sent", async () => {
    renderSection();

    await invite("marko@");

    expect(screen.getByRole("alert").textContent).toBe("Enter a valid e-mail address.");
    expect(screen.getByRole("textbox", { name: "E-mail" }).getAttribute("aria-invalid")).toBe(
      "true",
    );
    expect(vi.mocked(apiRequest).mock.calls.filter(([m]) => m === "POST")).toHaveLength(0);
  });

  it("EX-004-04 a Member's e-mail: This person is already a Member.", async () => {
    vi.mocked(apiRequest).mockImplementation((method: string) =>
      method === "GET"
        ? Promise.resolve([])
        : Promise.reject(new ApiError(409, { code: "invitation.alreadyMember" })),
    );
    renderSection();

    await invite("ana.kovac@example.si");

    expect((await screen.findByRole("alert")).textContent).toBe("This person is already a Member.");
    expect(screen.queryByRole("button", { name: "Copy link" })).toBeNull();
  });

  it("R-004-08 lists the open Invitations with e-mail, Role and expiry in Europe/Ljubljana", async () => {
    answer([OPEN]);
    renderSection();

    const row = await screen.findByRole("row", { name: new RegExp(MARKO.replace(".", "\\.")) });
    expect(row.textContent).toContain("Editor");
    expect(row.textContent).toContain("12.03.2026 10:00");
    expect(apiRequest).toHaveBeenCalledWith("GET", "/groups/g1/invitations");
  });

  it("R-004-09 Revoke revokes the Invitation and reads the list again", async () => {
    answer([OPEN]);
    renderSection();

    const row = await screen.findByRole("row", { name: /marko/ });
    await userEvent.click(within(row).getByRole("button", { name: "Revoke" }));

    expect(apiRequest).toHaveBeenCalledWith("DELETE", "/groups/g1/invitations/i1");
    await waitFor(() =>
      expect(vi.mocked(apiRequest).mock.calls.filter(([m]) => m === "GET")).toHaveLength(2),
    );
  });

  it("UI-14 no open Invitations", async () => {
    renderSection();

    expect(await screen.findByText("No open Invitations.")).toBeTruthy();
  });

  it("UI-14 the list cannot be read: Something went wrong. with Try again", async () => {
    vi.mocked(apiRequest).mockRejectedValue(new Error("offline"));
    renderSection();

    const alert = await screen.findByRole("alert");
    expect(alert.textContent).toContain("Something went wrong.");
    expect(within(alert).getByRole("button", { name: "Try again" })).toBeTruthy();
  });
});
