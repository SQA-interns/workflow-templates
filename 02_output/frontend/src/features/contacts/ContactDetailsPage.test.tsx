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
    [{ path: "/groups/:groupId/contacts/:contactId", element: <ContactDetailsPage /> }],
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
});
