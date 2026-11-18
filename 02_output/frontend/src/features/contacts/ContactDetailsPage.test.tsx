import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createMemoryRouter, RouterProvider } from "react-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "../../api/apiClient";
import { ApiError } from "../../api/problem";
import i18n from "../../i18n";
import { emptyFields } from "./contactFields";
import { ContactDetailsPage } from "./ContactDetailsPage";

vi.mock("../../api/apiClient", () => ({ apiRequest: vi.fn() }));

const JANEZ = {
  id: "c1",
  versionNumber: 3,
  state: "active",
  fields: {
    ...emptyFields(),
    firstName: "Janez",
    lastName: "Novak",
    email: "janez.novak@example.si",
    company: "Podjetje d.o.o.",
    country: "SI",
    note: null,
    city: null,
  },
  attributes: [{ key: "kompetenca", value: "spletnoProgramiranje" }],
  tags: [{ id: "t1", name: "udelezenec2025" }],
  createdAt: "2026-03-05T09:00:00Z",
  updatedAt: "2026-03-05T10:00:00Z",
  mergedFrom: [],
  mergedInto: null,
};

/** Answers GET /groups with the User's Role and every other read with the Contact. */
function answer(contact: unknown, role = "EDITOR") {
  vi.mocked(apiRequest).mockImplementation((_method: string, path: string) =>
    Promise.resolve(path === "/groups" ? [{ id: "g1", name: "Konferenca 2025", role }] : contact),
  );
}

function renderPage() {
  const router = createMemoryRouter(
    [
      { path: "/groups/:groupId/contacts", element: <p>Contact list</p> },
      { path: "/groups/:groupId/contacts/:contactId", element: <ContactDetailsPage /> },
    ],
    { initialEntries: ["/groups/g1/contacts/c1"] },
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
  answer(JANEZ);
  await i18n.changeLanguage("en");
});
afterEach(async () => {
  await i18n.changeLanguage("sl");
});

describe("WF-07 Contact details (R-014-06)", () => {
  it("R-014-06 shows the Contact's name and its Fields with their labels", async () => {
    renderPage();

    expect(await screen.findByRole("heading", { level: 1, name: "Janez Novak" })).toBeTruthy();
    expect(apiRequest).toHaveBeenCalledWith("GET", "/groups/g1/contacts/c1");
    const fields = screen.getByRole("region", { name: "Fields" });
    expect(within(fields).getByText("E-mail").nextSibling?.textContent).toBe(
      "janez.novak@example.si",
    );
    expect(within(fields).getByText("Country").nextSibling?.textContent).toBe("Slovenia");
    expect(within(fields).getByText("City").nextSibling?.textContent).toBe("—");
  });

  it("R-014-06 shows the Attributes and the Tags", async () => {
    renderPage();

    const attributes = await screen.findByRole("region", { name: "Attributes" });
    expect(within(attributes).getByText("kompetenca").nextSibling?.textContent).toBe(
      "spletnoProgramiranje",
    );
    expect(
      within(screen.getByRole("region", { name: "Tags" })).getByText("udelezenec2025"),
    ).toBeTruthy();
  });

  it("R-014-06 links to the History with the current Version and its time", async () => {
    renderPage();

    const links = await screen.findAllByRole("link", { name: "History" });
    expect(links.map((link) => link.getAttribute("href"))).toContain(
      "/groups/g1/contacts/c1/history",
    );
    expect(screen.getByText("Version 3 · 05.03.2026 11:00")).toBeTruthy();
  });

  it("R-014-06 without Attributes or Tags it says so", async () => {
    answer({ ...JANEZ, attributes: [], tags: [] });
    renderPage();

    expect(await screen.findByText("No Attributes.")).toBeTruthy();
    expect(screen.getByText("No Tags.")).toBeTruthy();
  });

  it("US-007 an Owner or Editor gets Edit, which opens the Contact form", async () => {
    renderPage();

    const edit = await screen.findByRole("link", { name: "Edit" });
    expect(edit.getAttribute("href")).toBe("/groups/g1/contacts/c1/edit");
  });

  it("BR-03 a Viewer gets no Edit", async () => {
    answer(JANEZ, "VIEWER");
    renderPage();

    await screen.findByRole("heading", { level: 1, name: "Janez Novak" });
    expect(screen.queryByRole("link", { name: "Edit" })).toBeNull();
  });

  it("AC-012-01 Move to recycle bin, confirmed, moves the Version read and opens the list", async () => {
    renderPage();

    await userEvent.click(await screen.findByRole("button", { name: "Move to recycle bin" }));
    const dialog = screen.getByRole("alertdialog", { name: "Move to recycle bin?" });
    expect(
      within(dialog).getByText("You can restore it from the recycle bin for 30 days."),
    ).toBeTruthy();
    await userEvent.click(within(dialog).getByRole("button", { name: "Move to recycle bin" }));

    expect(await screen.findByText("Contact list")).toBeTruthy();
    expect(apiRequest).toHaveBeenCalledWith("POST", "/groups/g1/contacts/c1/move-to-recycle-bin", {
      versionNumber: 3,
    });
  });

  it("EX-012-03 Cancel closes the confirmation and moves nothing", async () => {
    renderPage();

    await userEvent.click(await screen.findByRole("button", { name: "Move to recycle bin" }));
    await userEvent.click(screen.getByRole("button", { name: "Cancel" }));

    expect(screen.queryByRole("alertdialog")).toBeNull();
    expect(apiRequest).not.toHaveBeenCalledWith("POST", expect.anything(), expect.anything());
  });

  it("BR-06 a refused move shows its message in the confirmation", async () => {
    renderPage();
    await userEvent.click(await screen.findByRole("button", { name: "Move to recycle bin" }));
    vi.mocked(apiRequest).mockRejectedValueOnce(
      new ApiError(409, { code: "version.conflict", params: { changedBy: "Ana Kovač" } }),
    );

    const dialog = screen.getByRole("alertdialog");
    await userEvent.click(within(dialog).getByRole("button", { name: "Move to recycle bin" }));

    expect(await within(dialog).findByRole("alert")).toBeTruthy();
    expect(screen.queryByText("Contact list")).toBeNull();
  });

  it("AC-012-04 a Viewer gets no Move to recycle bin", async () => {
    answer(JANEZ, "VIEWER");
    renderPage();
    await screen.findByRole("heading", { level: 1, name: "Janez Novak" });
    expect(screen.queryByRole("button", { name: "Move to recycle bin" })).toBeNull();
  });

  it("EX-012-06 a Contact in the recycle bin offers no Move to recycle bin", async () => {
    answer({ ...JANEZ, state: "inRecycleBin" });
    renderPage();
    await screen.findByRole("heading", { level: 1, name: "Janez Novak" });
    expect(screen.queryByRole("button", { name: "Move to recycle bin" })).toBeNull();
  });

  it("BR-02 an unknown Contact: This page cannot be found.", async () => {
    vi.mocked(apiRequest).mockRejectedValue(new ApiError(404, { code: "notFound" }));
    renderPage();

    expect(await screen.findByText("This page cannot be found.")).toBeTruthy();
  });

  it("UI-14 an error shows Something went wrong. and Try again asks again", async () => {
    vi.mocked(apiRequest).mockRejectedValueOnce(new Error("down"));
    renderPage();

    expect(await screen.findByText("Something went wrong.")).toBeTruthy();
    await userEvent.click(screen.getByRole("button", { name: "Try again" }));

    expect(await screen.findByRole("heading", { level: 1, name: "Janez Novak" })).toBeTruthy();
  });

  it("R-018-05 the Primary contact shows Merged from with a link to the Other contact's History", async () => {
    answer({
      ...JANEZ,
      mergedFrom: [
        { mergeId: "m1", contact: { id: "c2", name: "J. Novak" }, undone: false },
        { mergeId: "m0", contact: { id: "c3", name: "Jani Novak" }, undone: true },
      ],
    });

    renderPage();

    const link = await screen.findByRole("link", { name: "J. Novak" });
    expect(link.getAttribute("href")).toBe("/groups/g1/contacts/c2/history");
    expect(screen.getByText(/Merged from/)).toBeTruthy();
    expect(screen.queryByRole("link", { name: "Jani Novak" })).toBeNull();
  });

  describe("US-019 Undo merge", () => {
    const MERGED = {
      ...JANEZ,
      versionNumber: 4,
      mergedFrom: [{ mergeId: "m1", contact: { id: "c2", name: "J. Novak" }, undone: false }],
    };

    it("R-019-01 Undo merge, confirmed, undoes the Merge with the Version read", async () => {
      answer(MERGED);
      renderPage();
      await userEvent.click(await screen.findByRole("button", { name: "Undo merge" }));
      const dialog = screen.getByRole("alertdialog", { name: "Undo merge?" });

      await userEvent.click(within(dialog).getByRole("button", { name: "Undo merge" }));

      expect(apiRequest).toHaveBeenCalledWith("POST", "/groups/g1/merges/m1/undo", {
        versionNumber: 4,
      });
      await vi.waitFor(() => expect(screen.queryByRole("alertdialog")).toBeNull());
    });

    it("EX-019-03 Cancel closes the confirmation and undoes nothing", async () => {
      answer(MERGED);
      renderPage();
      await userEvent.click(await screen.findByRole("button", { name: "Undo merge" }));

      await userEvent.click(screen.getByRole("button", { name: "Cancel" }));

      expect(screen.queryByRole("alertdialog")).toBeNull();
      expect(apiRequest).not.toHaveBeenCalledWith("POST", expect.anything(), expect.anything());
    });

    it("R-019-02 a refused undo shows its message in the confirmation", async () => {
      answer(MERGED);
      renderPage();
      await userEvent.click(await screen.findByRole("button", { name: "Undo merge" }));
      vi.mocked(apiRequest).mockRejectedValueOnce(
        new ApiError(409, { code: "merge.changedAfter" }),
      );

      const dialog = screen.getByRole("alertdialog");
      await userEvent.click(within(dialog).getByRole("button", { name: "Undo merge" }));

      expect((await within(dialog).findByRole("alert")).textContent).toBe(
        "The Contact changed after the Merge. Restore the Versions you need instead.",
      );
    });

    it("BR-03 a Viewer gets no Undo merge", async () => {
      answer(MERGED, "VIEWER");
      renderPage();
      await screen.findByRole("link", { name: "J. Novak" });
      expect(screen.queryByRole("button", { name: "Undo merge" })).toBeNull();
    });
  });
});
