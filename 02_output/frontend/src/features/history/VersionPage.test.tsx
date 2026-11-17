import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, within } from "@testing-library/react";
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

function answer(version: unknown) {
  vi.mocked(apiRequest).mockImplementation(() =>
    version instanceof Error ? Promise.reject(version) : Promise.resolve(version),
  );
}

function renderPage() {
  const router = createMemoryRouter(
    [
      { path: "/groups/:groupId/contacts/:contactId/history/:number", element: <VersionPage /> },
      { path: "/groups/:groupId/contacts/:contactId/history", element: <p>History</p> },
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
