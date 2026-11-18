import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createMemoryRouter, RouterProvider } from "react-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "../../api/apiClient";
import { ApiError } from "../../api/problem";
import i18n from "../../i18n";
import { GroupSettingsPage } from "./GroupSettingsPage";

vi.mock("../../api/apiClient", () => ({ apiRequest: vi.fn() }));

/** GET /groups answers the Group with the Role; PATCH the renamed Group; the move nothing. */
function answer(role = "OWNER", name = "Konferenca 2025") {
  vi.mocked(apiRequest).mockImplementation((method: string, _path: string, body?: unknown) => {
    if (method === "GET") return Promise.resolve([{ id: "g1", name, role }]);
    if (method === "PATCH") return Promise.resolve({ id: "g1", role, ...(body as object) });
    return Promise.resolve(undefined);
  });
}

function renderPage() {
  const router = createMemoryRouter(
    [
      { path: "/", element: <p>start</p> },
      { path: "/groups/:groupId/settings", element: <GroupSettingsPage /> },
    ],
    { initialEntries: ["/groups/g1/settings"] },
  );
  render(
    <QueryClientProvider
      client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}
    >
      <RouterProvider router={router} />
    </QueryClientProvider>,
  );
  return router;
}

async function nameField() {
  return screen.findByLabelText(/Group name/);
}

beforeEach(async () => {
  vi.mocked(apiRequest).mockReset();
  answer();
  await i18n.changeLanguage("en");
});
afterEach(async () => {
  await i18n.changeLanguage("sl");
});

describe("WF-14 Group settings (US-003)", () => {
  it("AC-003-01 saves the trimmed new name and reads the Groups again", async () => {
    renderPage();
    const field = await nameField();
    expect(field).toHaveProperty("value", "Konferenca 2025");

    await userEvent.clear(field);
    await userEvent.type(field, " Konferenca 2026 ");
    answer("OWNER", "Konferenca 2026");
    await userEvent.click(screen.getByRole("button", { name: "Save" }));

    expect(apiRequest).toHaveBeenCalledWith("PATCH", "/groups/g1", { name: "Konferenca 2026" });
    expect((await screen.findByRole("status")).textContent).toBe("The name was saved.");
    expect(vi.mocked(apiRequest).mock.calls.filter(([m]) => m === "GET").length).toBe(2);
  });

  it("EX-003-06 a 101-character name: Enter a name of 1 to 100 characters. and nothing is sent", async () => {
    renderPage();
    const field = await nameField();

    await userEvent.clear(field);
    await userEvent.click(field);
    await userEvent.paste("K".repeat(101));
    await userEvent.click(screen.getByRole("button", { name: "Save" }));

    expect((await screen.findByRole("alert")).textContent).toBe(
      "Enter a name of 1 to 100 characters.",
    );
    expect(field.getAttribute("aria-invalid")).toBe("true");
    expect(apiRequest).not.toHaveBeenCalledWith("PATCH", expect.anything(), expect.anything());
  });

  it("AC-003-03 Move Group to recycle bin asks first, then moves and opens the start", async () => {
    const router = renderPage();
    await userEvent.click(await screen.findByRole("button", { name: "Move Group to recycle bin" }));

    const dialog = await screen.findByRole("alertdialog");
    expect(
      within(dialog).getByRole("heading", {
        name: "Move Konferenca 2025 to the recycle bin?",
      }),
    ).toBeTruthy();
    answer();
    await userEvent.click(
      within(dialog).getByRole("button", { name: "Move Group to recycle bin" }),
    );

    expect(apiRequest).toHaveBeenCalledWith("POST", "/groups/g1/move-to-recycle-bin");
    await waitFor(() => expect(router.state.location.pathname).toBe("/"));
  });

  it("EX-003-04 Cancel in the confirmation changes nothing", async () => {
    const router = renderPage();
    await userEvent.click(await screen.findByRole("button", { name: "Move Group to recycle bin" }));

    await userEvent.click(
      within(await screen.findByRole("alertdialog")).getByRole("button", { name: "Cancel" }),
    );

    expect(apiRequest).not.toHaveBeenCalledWith("POST", expect.anything());
    expect(screen.queryByRole("alertdialog")).toBeNull();
    expect(router.state.location.pathname).toBe("/groups/g1/settings");
  });

  it("AC-003-02 an Editor sees You are not allowed to do this. and no form", async () => {
    answer("EDITOR");
    renderPage();

    expect((await screen.findByRole("alert")).textContent).toBe("You are not allowed to do this.");
    expect(screen.queryByLabelText(/Group name/)).toBeNull();
  });
  it("BR-04 an Editor of this Group is refused, whatever Role they have elsewhere", async () => {
    vi.mocked(apiRequest).mockResolvedValue([
      { id: "g0", name: "Lastna", role: "OWNER" },
      { id: "g1", name: "Konferenca 2025", role: "EDITOR" },
    ]);
    renderPage();

    expect((await screen.findByRole("alert")).textContent).toBe("You are not allowed to do this.");
  });

  it("BR-04 a User who is not a Member of the Group is refused", async () => {
    vi.mocked(apiRequest).mockResolvedValue([{ id: "g0", name: "Lastna", role: "OWNER" }]);
    renderPage();

    expect((await screen.findByRole("alert")).textContent).toBe("You are not allowed to do this.");
  });

  it("UI-14 while the Groups load nothing is refused yet", async () => {
    vi.mocked(apiRequest).mockReturnValue(new Promise(() => undefined));
    renderPage();

    expect((await screen.findAllByRole("status")).length).toBeGreaterThan(0);
    expect(screen.queryByRole("alert")).toBeNull();
  });

  it("WF-14 names both cards and describes the name rule", async () => {
    renderPage();
    const field = await nameField();

    expect(screen.getByRole("heading", { name: "Rename the Group" })).toBeTruthy();
    expect(
      screen.getByRole("heading", { level: 2, name: "Move Group to recycle bin" }),
    ).toBeTruthy();
    expect(screen.getByText(/The Group and its Contacts are hidden from all Members/)).toBeTruthy();
    expect(field.getAttribute("aria-invalid")).toBe("false");
    const hint = document.getElementById(field.getAttribute("aria-describedby") ?? "");
    expect(hint?.textContent).toBe("1 to 100 characters");
  });

  it("R-003-01 a name refused by the server shows the name rule", async () => {
    renderPage();
    const field = await nameField();
    vi.mocked(apiRequest).mockRejectedValueOnce(
      new ApiError(400, {
        code: "validation",
        errors: [{ field: "name", code: "group.nameLength" }],
      }),
    );

    await userEvent.click(screen.getByRole("button", { name: "Save" }));

    expect((await screen.findByRole("alert")).textContent).toBe(
      "Enter a name of 1 to 100 characters.",
    );
    expect(field.getAttribute("aria-invalid")).toBe("true");
  });

  it("UI-14 a rename that fails without a problem: Something went wrong.", async () => {
    renderPage();
    const field = await nameField();
    vi.mocked(apiRequest).mockRejectedValueOnce(new Error("offline"));

    await userEvent.click(screen.getByRole("button", { name: "Save" }));

    expect((await screen.findByRole("alert")).textContent).toBe("Something went wrong.");
    expect(field.getAttribute("aria-invalid")).toBe("false");
  });

  it("R-003-02 the confirmation explains the move and shows no error before it", async () => {
    renderPage();
    await userEvent.click(await screen.findByRole("button", { name: "Move Group to recycle bin" }));

    const dialog = await screen.findByRole("alertdialog");
    expect(
      within(dialog).getByText(/The Group and its Contacts are hidden from all Members/),
    ).toBeTruthy();
    expect(within(dialog).queryByRole("alert")).toBeNull();
  });

  it("R-003-02 a refused move shows its message in the confirmation", async () => {
    renderPage();
    await userEvent.click(await screen.findByRole("button", { name: "Move Group to recycle bin" }));
    const dialog = await screen.findByRole("alertdialog");
    vi.mocked(apiRequest).mockRejectedValueOnce(new ApiError(403, { code: "auth.forbidden" }));

    await userEvent.click(
      within(dialog).getByRole("button", { name: "Move Group to recycle bin" }),
    );

    expect((await within(dialog).findByRole("alert")).textContent).toBe(
      "You are not allowed to do this.",
    );
  });

  it("UI-14 a move that fails without a problem shows Something went wrong., forgotten on reopening", async () => {
    renderPage();
    await userEvent.click(await screen.findByRole("button", { name: "Move Group to recycle bin" }));
    const dialog = await screen.findByRole("alertdialog");
    vi.mocked(apiRequest).mockRejectedValueOnce(new Error("offline"));

    await userEvent.click(
      within(dialog).getByRole("button", { name: "Move Group to recycle bin" }),
    );
    expect((await within(dialog).findByRole("alert")).textContent).toBe("Something went wrong.");
    await userEvent.click(within(dialog).getByRole("button", { name: "Cancel" }));
    await waitFor(() => expect(screen.queryByRole("alertdialog")).toBeNull());
    await userEvent.click(screen.getByRole("button", { name: "Move Group to recycle bin" }));

    expect(within(await screen.findByRole("alertdialog")).queryByRole("alert")).toBeNull();
  });
});
