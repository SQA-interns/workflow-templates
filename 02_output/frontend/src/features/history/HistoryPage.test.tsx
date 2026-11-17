import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, within } from "@testing-library/react";
import { createMemoryRouter, RouterProvider } from "react-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "../../api/apiClient";
import { ApiError } from "../../api/problem";
import i18n from "../../i18n";
import { emptyFields } from "../contacts/contactFields";
import { HistoryPage } from "./HistoryPage";

vi.mock("../../api/apiClient", () => ({ apiRequest: vi.fn() }));

const JANEZ = {
  id: "c1",
  versionNumber: 2,
  state: "active",
  fields: { ...emptyFields(), firstName: "Janez", lastName: "Novak" },
  attributes: [],
  tags: [],
  updatedAt: "2026-07-01T08:00:00Z",
};

const HISTORY = [
  {
    number: 2,
    kind: "edited",
    changedBy: "Marko Kranjc",
    changedAt: "2026-07-01T08:00:00Z",
    changes: [{ item: "email", oldValue: "janez.novak@example.si", newValue: "janez@novak.si" }],
  },
  {
    number: 1,
    kind: "created",
    changedBy: "Ana Kovač",
    changedAt: "2026-03-05T09:00:00Z",
    changes: [{ item: "firstName", oldValue: null, newValue: "Janez" }],
  },
];

/** Answers the History path with the History and the Contact path with the Contact. */
function answer(history: unknown) {
  vi.mocked(apiRequest).mockImplementation((_method: string, path: string) =>
    path.endsWith("/versions")
      ? history instanceof Error
        ? Promise.reject(history)
        : Promise.resolve(history)
      : Promise.resolve(JANEZ),
  );
}

function renderPage() {
  const router = createMemoryRouter(
    [
      { path: "/groups/:groupId/contacts/:contactId/history", element: <HistoryPage /> },
      { path: "/groups/:groupId/contacts/:contactId/history/:number", element: <p>Version</p> },
    ],
    { initialEntries: ["/groups/g1/contacts/c1/history"] },
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
  answer(HISTORY);
  await i18n.changeLanguage("en");
});
afterEach(async () => {
  await i18n.changeLanguage("sl");
});

describe("WF-09 History (R-010-01)", () => {
  it("AC-011-01 a restored Version names the Version it restored", async () => {
    answer([
      {
        number: 3,
        kind: "restored",
        changedBy: "Marko Kranjc",
        changedAt: "2026-07-02T08:00:00Z",
        restoredFrom: 1,
        changes: [
          { item: "email", oldValue: "janez@novak.si", newValue: "janez.novak@example.si" },
        ],
      },
      ...HISTORY,
    ]);
    renderPage();

    const version3 = within(await screen.findByRole("region", { name: "Version 3" }));
    expect(version3.getByText("restored · from Version 1")).toBeTruthy();
    expect(screen.getByRole("region", { name: "Version 2" })).toBeTruthy();
  });

  it("R-010-01 lists the Versions newest first with kind, who, when and what", async () => {
    renderPage();

    expect(
      await screen.findByRole("heading", { level: 1, name: "History of Janez Novak" }),
    ).toBeTruthy();
    expect(apiRequest).toHaveBeenCalledWith("GET", "/groups/g1/contacts/c1/versions");
    const versions = screen.getAllByRole("region");
    expect(versions.map((version) => version.getAttribute("aria-label"))).toEqual([
      "Version 2",
      "Version 1",
    ]);
    const second = within(screen.getByRole("region", { name: "Version 2" }));
    expect(second.getByText("edited")).toBeTruthy();
    expect(second.getByText("Marko Kranjc")).toBeTruthy();
    expect(second.getByText("01.07.2026 10:00")).toBeTruthy();
    expect(second.getByText("E-mail: janez.novak@example.si → janez@novak.si")).toBeTruthy();
    const first = within(screen.getByRole("region", { name: "Version 1" }));
    expect(first.getByText("created")).toBeTruthy();
    expect(first.getByText("Ana Kovač")).toBeTruthy();
    expect(first.getByText("05.03.2026 10:00")).toBeTruthy();
  });

  it("R-010-02 each Version links to its content", async () => {
    renderPage();

    const second = await screen.findByRole("region", { name: "Version 2" });
    expect(within(second).getByRole("link", { name: "View" }).getAttribute("href")).toBe(
      "/groups/g1/contacts/c1/history/2",
    );
  });

  it("EX-010-01 a time after midnight in Ljubljana shows the next day", async () => {
    answer([{ ...HISTORY[1], changedAt: "2026-12-31T23:30:00Z" }]);
    renderPage();

    expect(await screen.findByText("01.01.2027 00:30")).toBeTruthy();
  });

  it("EX-010-04 a Contact with one Version shows one entry", async () => {
    answer([HISTORY[1]]);
    renderPage();

    await screen.findByRole("region", { name: "Version 1" });
    expect(screen.getAllByRole("region")).toHaveLength(1);
  });

  it("AC-010-03 a History the User may not see shows Not found", async () => {
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
