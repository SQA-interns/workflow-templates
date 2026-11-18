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
  vi.restoreAllMocks();
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

    const row = await screen.findByRole("row", { name: /marko\.kranjc@gmail\.com/ });
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
  it("R-004-01 Editor is the preselected Role", async () => {
    renderSection();

    expect(screen.getByRole<HTMLSelectElement>("combobox", { name: "Role" }).value).toBe("EDITOR");
    await userEvent.type(screen.getByRole("textbox", { name: "E-mail" }), MARKO);
    await userEvent.click(screen.getByRole("button", { name: "Create Invitation" }));

    expect(apiRequest).toHaveBeenCalledWith("POST", "/groups/g1/invitations", {
      email: MARKO,
      role: "EDITOR",
    });
  });

  it("AC-004-01 after the Invitation is created the e-mail field is empty and the link is titled", async () => {
    renderSection();

    await invite(MARKO);

    expect(await screen.findByText("Invitation link")).toBeTruthy();
    expect(screen.getByRole<HTMLInputElement>("textbox", { name: "E-mail" }).value).toBe("");
    expect(screen.getByRole("textbox", { name: "E-mail" }).getAttribute("aria-invalid")).toBe(
      "false",
    );
  });

  it("AC-004-01 a link that cannot be copied says so", async () => {
    const user = userEvent.setup();
    vi.spyOn(navigator.clipboard, "writeText").mockRejectedValue(new Error("denied"));
    renderSection();
    await invite(MARKO);

    await user.click(await screen.findByRole("button", { name: "Copy link" }));

    expect((await screen.findByRole("status")).textContent).toBe(
      "The link could not be copied. Select it and copy it.",
    );
  });

  it("UI-14 an Invitation that fails without a problem: Something went wrong.", async () => {
    vi.mocked(apiRequest).mockImplementation((method: string) =>
      method === "GET" ? Promise.resolve([]) : Promise.reject(new Error("offline")),
    );
    renderSection();

    await invite(MARKO);

    expect((await screen.findByRole("alert")).textContent).toBe("Something went wrong.");
    expect(screen.getByRole("textbox", { name: "E-mail" }).getAttribute("aria-invalid")).toBe(
      "false",
    );
  });

  it("R-004-08 the open Invitations have a title and named columns, and no error", async () => {
    answer([OPEN]);
    renderSection();

    const row = await screen.findByRole("row", { name: /marko/ });
    expect(screen.getByRole("heading", { name: "Open Invitations" })).toBeTruthy();
    expect(screen.getAllByRole("columnheader").map((header) => header.textContent)).toEqual([
      "E-mail",
      "Role",
      "Expires",
      "Actions",
    ]);
    expect(row.textContent).toContain("expires");
    expect(screen.queryByRole("alert")).toBeNull();
  });

  it("R-004-09 while one Invitation is revoked every Revoke waits, the revoked one is busy", async () => {
    const other = { ...OPEN, id: "i2", email: "ana.kovac@example.si" };
    vi.mocked(apiRequest).mockImplementation((method: string) =>
      method === "GET" ? Promise.resolve([OPEN, other]) : new Promise(() => undefined),
    );
    renderSection();

    const row = await screen.findByRole("row", { name: /marko/ });
    const otherRow = screen.getByRole("row", { name: /ana\.kovac/ });
    expect(within(row).getByRole("button", { name: "Revoke" }).getAttribute("aria-busy")).toBe(
      "false",
    );
    await userEvent.click(within(row).getByRole("button", { name: "Revoke" }));

    const busy = within(row).getByRole<HTMLButtonElement>("button", { name: "Revoke" });
    const waiting = within(otherRow).getByRole<HTMLButtonElement>("button", { name: "Revoke" });
    expect(busy.disabled).toBe(true);
    expect(busy.getAttribute("aria-busy")).toBe("true");
    expect(waiting.disabled).toBe(true);
    expect(waiting.getAttribute("aria-busy")).toBe("false");
  });

  it("R-004-09 a refused Revoke shows why", async () => {
    vi.mocked(apiRequest).mockImplementation((method: string) =>
      method === "GET"
        ? Promise.resolve([OPEN])
        : Promise.reject(new ApiError(410, { code: "invitation.noLongerValid" })),
    );
    renderSection();

    const row = await screen.findByRole("row", { name: /marko/ });
    await userEvent.click(within(row).getByRole("button", { name: "Revoke" }));

    expect((await screen.findByRole("alert")).textContent).toBe(
      "This Invitation is no longer valid.",
    );
  });
});
