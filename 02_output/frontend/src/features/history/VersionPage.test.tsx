import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { createMemoryRouter, RouterProvider } from "react-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "../../api/apiClient";
import { ApiError } from "../../api/problem";
import i18n from "../../i18n";
import { emptyFields } from "../contacts/contactFields";
import { VersionPage } from "./VersionPage";

vi.mock("../../api/apiClient", () => ({ apiRequest: vi.fn() }));

const VERSION_1 = {
  number: 1,
  kind: "created",
  changedBy: "Ana Kovač",
  changedAt: "2026-03-05T09:00:00Z",
  changes: [],
  snapshot: {
    fields: {
      ...emptyFields(),
      firstName: "Janez",
      lastName: "Novak",
      email: "janez.novak@example.si",
      city: "Ljubljana",
    },
    attributes: [{ key: "kompetenca", value: "spletnoProgramiranje" }],
    tags: ["udelezenec2025"],
  },
};

const JANEZ_AT_VERSION_2 = {
  id: "c1",
  versionNumber: 2,
  state: "active",
  fields: { ...VERSION_1.snapshot.fields, email: "janez@novak.si" },
  attributes: [],
  tags: [],
  updatedAt: "2026-07-01T08:00:00Z",
};

const settle = (value: unknown) =>
  value instanceof Error ? Promise.reject(value) : Promise.resolve(value);

/**
 * Answers the Version path with the Version, the Groups with Marko's role, the Contact with Janez
 * at Version 2 and a restore with `restored`.
 */
function answer(version: unknown, role = "EDITOR", restored: unknown = JANEZ_AT_VERSION_2) {
  vi.mocked(apiRequest).mockImplementation((method: string, path: string) => {
    if (method === "POST") return settle(restored);
    if (path === "/groups") return settle([{ id: "g1", name: "Konferenca 2025", role }]);
    if (path === "/groups/g1/contacts/c1") return settle(JANEZ_AT_VERSION_2);
    return settle(version);
  });
}

function renderPage() {
  const router = createMemoryRouter(
    [
      { path: "/groups/:groupId/contacts/:contactId/history/:number", element: <VersionPage /> },
      {
        path: "/groups/:groupId/contacts/:contactId/history",
        element: <p>History of the Contact</p>,
      },
    ],
    { initialEntries: ["/groups/g1/contacts/c1/history/1"] },
  );
  render(
    <QueryClientProvider
      client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}
    >
      <RouterProvider router={router} />
    </QueryClientProvider>,
  );
}

beforeEach(async () => {
  vi.mocked(apiRequest).mockReset();
  answer(VERSION_1);
  await i18n.changeLanguage("en");
});
afterEach(async () => {
  await i18n.changeLanguage("sl");
});

describe("WF-10 Version (R-010-02)", () => {

  it("R-021-03 a Version of a deleted User shows Deleted user", async () => {
    answer({ ...VERSION_1, changedBy: "user.deleted" });
    renderPage();

    expect(
      await screen.findByText("created · Deleted user · 05.03.2026 10:00 · read-only"),
    ).toBeTruthy();
  });
  it("R-010-02 shows the Fields, Attributes and Tags as they were after the Version", async () => {
    renderPage();

    expect(
      await screen.findByRole("heading", { level: 1, name: "Version 1 of Janez Novak" }),
    ).toBeTruthy();
    expect(apiRequest).toHaveBeenCalledWith("GET", "/groups/g1/contacts/c1/versions/1");
    expect(screen.getByText("created · Ana Kovač · 05.03.2026 10:00 · read-only")).toBeTruthy();
    const fields = within(screen.getByRole("region", { name: "Fields" }));
    expect(fields.getByText("E-mail").nextSibling?.textContent).toBe("janez.novak@example.si");
    expect(fields.getByText("City").nextSibling?.textContent).toBe("Ljubljana");
    expect(fields.getByText("Phone").nextSibling?.textContent).toBe("—");
    const attributes = within(screen.getByRole("region", { name: "Attributes" }));
    expect(attributes.getByText("kompetenca").nextSibling?.textContent).toBe(
      "spletnoProgramiranje",
    );
    const tags = within(screen.getByRole("region", { name: "Tags" }));
    expect(tags.getByText("udelezenec2025")).toBeTruthy();
  });

  it("AC-010-02 the Version is read-only: no inputs, no Edit, no Save", async () => {
    renderPage();

    await screen.findByRole("heading", { level: 1 });
    expect(screen.queryAllByRole("textbox")).toHaveLength(0);
    expect(screen.queryByRole("button", { name: "Edit" })).toBeNull();
    expect(screen.queryByRole("link", { name: "Edit" })).toBeNull();
    expect(screen.queryByRole("button", { name: "Save" })).toBeNull();
  });

  it("R-010-02 links back to the History", async () => {
    renderPage();

    expect((await screen.findByRole("link", { name: "History" })).getAttribute("href")).toBe(
      "/groups/g1/contacts/c1/history",
    );
  });

  it("EX-010-05 a Version number that does not exist shows Not found", async () => {
    answer(new ApiError(404, { code: "notFound" }));
    renderPage();

    expect(await screen.findByText("This page cannot be found.")).toBeTruthy();
  });

  it("UI-14 shows the error state with Try again", async () => {
    answer(new ApiError(500, { code: "internal" }));
    renderPage();

    expect(await screen.findByRole("button", { name: "Try again" })).toBeTruthy();
  });
});

describe("WF-10 Restore this Version (US-011)", () => {
  async function confirmRestore() {
    fireEvent.click(await screen.findByRole("button", { name: "Restore this Version" }));
    const dialog = within(
      await screen.findByRole("alertdialog", { name: "Restore this Version?" }),
    );
    expect(
      dialog.getByText(
        "The Contact gets the content of Version 1 as a new Version. No Version is deleted.",
      ),
    ).toBeTruthy();
    fireEvent.click(dialog.getByRole("button", { name: "Restore this Version" }));
  }

  it("AC-011-01 restores Version 1 with the current Version number read and opens the History", async () => {
    renderPage();

    await confirmRestore();

    expect(await screen.findByText("History of the Contact")).toBeTruthy();
    expect(apiRequest).toHaveBeenCalledWith("POST", "/groups/g1/contacts/c1/versions/1/restore", {
      versionNumber: 2,
    });
  });

  it("EX-011-03 Cancel closes the confirmation and restores nothing", async () => {
    renderPage();

    fireEvent.click(await screen.findByRole("button", { name: "Restore this Version" }));
    const dialog = await screen.findByRole("alertdialog");
    fireEvent.click(within(dialog).getByRole("button", { name: "Cancel" }));

    await waitFor(() => expect(screen.queryByRole("alertdialog")).toBeNull());
    expect(apiRequest).not.toHaveBeenCalledWith("POST", expect.anything(), expect.anything());
  });

  it.each([
    ["AC-011-04", "version.sameAsCurrent", "This Version is the same as the current one."],
    ["EX-011-04", "contact.inRecycleBin", "This Contact is in the recycle bin."],
  ])("%s a refused restore shows its message in the confirmation", async (_id, code, text) => {
    answer(VERSION_1, "EDITOR", new ApiError(409, { code }));
    renderPage();

    await confirmRestore();

    const alert = await screen.findByRole("alert");
    expect(alert.textContent).toBe(text);
    expect(screen.getByRole("alertdialog")).toBeTruthy();
  });

  it("AC-011-03 a Viewer is not offered Restore this Version", async () => {
    answer(VERSION_1, "VIEWER");
    renderPage();

    await screen.findByRole("heading", { level: 1 });
    await waitFor(() => expect(apiRequest).toHaveBeenCalledWith("GET", "/groups"));
    expect(screen.queryByRole("button", { name: "Restore this Version" })).toBeNull();
  });
});
