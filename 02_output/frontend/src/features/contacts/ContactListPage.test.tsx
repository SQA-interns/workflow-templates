import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createMemoryRouter, RouterProvider } from "react-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "../../api/apiClient";
import type { ContactListItem } from "../../api/contacts";
import i18n from "../../i18n";
import { ContactListPage } from "./ContactListPage";

vi.mock("../../api/apiClient", () => ({ apiRequest: vi.fn() }));

const LIST = "/groups/g1/contacts";

function item(id: string, firstName: string, lastName: string): ContactListItem {
  return {
    id,
    firstName,
    lastName,
    company: null,
    email: null,
    phone: null,
    tags: [],
    updatedAt: "2026-03-05T10:00:00Z",
  };
}

const THREE = [
  item("c1", "Žiga", "Čuk"),
  item("c2", "Janez", "Novak"),
  item("c3", "Maja", "Zupan"),
];

const TAGS = [
  { id: "t1", name: "neObvescaj" },
  { id: "t2", name: "udelezenec2025" },
];

/** Answers GET /groups with the Role and the list with a Page per query (or an error). */
function answer(role: string, page: (path: string) => unknown) {
  vi.mocked(apiRequest).mockImplementation((_method: string, path: string) => {
    if (path === "/groups") return Promise.resolve([{ id: "g1", name: "Konferenca 2025", role }]);
    if (path === "/groups/g1/tags") return Promise.resolve(TAGS);
    const result = page(path);
    return result instanceof Error ? Promise.reject(result) : Promise.resolve(result);
  });
}

function pageOf(items: ContactListItem[], total = items.length, number = 1) {
  return { items, total, page: number, pageSize: 50 };
}

function listCalls(): string[] {
  return vi
    .mocked(apiRequest)
    .mock.calls.map(([, path]) => path as string)
    .filter((path) => path.startsWith(`${LIST}?`));
}

function renderPage(entry = LIST) {
  const router = createMemoryRouter(
    [
      { path: "/groups/:groupId/contacts", element: <ContactListPage /> },
      { path: "/groups/:groupId/contacts/new", element: <p>contact form</p> },
    ],
    { initialEntries: [entry] },
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

beforeEach(async () => {
  vi.mocked(apiRequest).mockReset();
  await i18n.changeLanguage("en");
});
afterEach(async () => {
  await i18n.changeLanguage("sl");
});

describe("WF-06 Contact list (US-014)", () => {
  it("AC-014-01 shows the count and the rows of the first Page by name", async () => {
    answer("VIEWER", () => pageOf(THREE));
    renderPage();

    expect(await screen.findByText("3 Contacts")).toBeTruthy();
    expect(screen.getByRole("heading", { level: 1, name: "Contacts" })).toBeTruthy();
    expect(screen.getAllByRole("link").map((l) => l.textContent)).toEqual([
      "Žiga Čuk",
      "Janez Novak",
      "Maja Zupan",
    ]);
    expect(listCalls()).toEqual([`${LIST}?q=&sort=name&dir=asc&page=1`]);
  });

  it("UI-18 the count is a number of the language, with its plural", async () => {
    answer("VIEWER", (path) => pageOf([], path.includes("q=one") ? 1 : 10000));
    renderPage(`${LIST}?q=one`);
    expect(await screen.findByText("1 Contact")).toBeTruthy();

    await i18n.changeLanguage("sl");
    renderPage(`${LIST}?q=many`);
    expect(await screen.findByText("10.000 kontaktov")).toBeTruthy();
  });

  it("AC-014-02 typing a search asks for the matching Contacts from Page 1", async () => {
    answer("VIEWER", (path) =>
      pageOf(path.includes("q=cuk") ? [THREE[0] as ContactListItem] : THREE),
    );
    const router = renderPage(`${LIST}?page=2`);

    await userEvent.type(await screen.findByLabelText("Search"), "cuk");

    expect(await screen.findByText("1 Contact")).toBeTruthy();
    expect(listCalls()).toContain(`${LIST}?q=cuk&sort=name&dir=asc&page=1`);
    expect(router.state.location.search).toBe("?q=cuk");
  });

  it("AC-014-04 no match: No Contacts match. and Clear search shows the whole list", async () => {
    answer("VIEWER", (path) => (path.includes("q=xyz") ? pageOf([]) : pageOf(THREE)));
    const router = renderPage(`${LIST}?q=xyz`);

    expect(await screen.findByText("No Contacts match.")).toBeTruthy();
    await userEvent.click(screen.getByRole("button", { name: "Clear search" }));

    expect(await screen.findByText("3 Contacts")).toBeTruthy();
    expect(router.state.location.search).toBe("");
    expect((screen.getByLabelText("Search") as HTMLInputElement).value).toBe("");
  });

  it("EX-014-02 an empty Group: No Contacts yet. and Add a Contact opens the form", async () => {
    answer("OWNER", () => pageOf([]));
    const router = renderPage();

    expect(await screen.findByText("No Contacts yet.")).toBeTruthy();
    const add = screen.getAllByRole("button", { name: "Add a Contact" });
    await userEvent.click(add[0] as HTMLElement);

    expect(router.state.location.pathname).toBe(`${LIST}/new`);
  });

  it("ADR-04 a Viewer gets no Add a Contact", async () => {
    answer("VIEWER", () => pageOf([]));
    renderPage();

    expect(await screen.findByText("No Contacts yet.")).toBeTruthy();
    expect(screen.queryByRole("button", { name: "Add a Contact" })).toBeNull();
  });

  it("AC-014-05 Next and Previous move between the Pages of the total", async () => {
    answer("VIEWER", (path) => pageOf(THREE, 120, Number(/page=(\d+)/.exec(path)?.[1])));
    const router = renderPage();

    expect(await screen.findByText("Page 1 of 3")).toBeTruthy();
    expect((screen.getByRole("button", { name: "Previous" }) as HTMLButtonElement).disabled).toBe(
      true,
    );
    await userEvent.click(screen.getByRole("button", { name: "Next" }));

    expect(await screen.findByText("Page 2 of 3")).toBeTruthy();
    expect(router.state.location.search).toBe("?page=2");
    expect(listCalls()).toContain(`${LIST}?q=&sort=name&dir=asc&page=2`);
  });

  it("EX-014-01 50 Contacts are one Page without paging", async () => {
    answer("VIEWER", () => pageOf(THREE, 50));
    renderPage();

    expect(await screen.findByText("50 Contacts")).toBeTruthy();
    expect(screen.queryByRole("button", { name: "Next" })).toBeNull();
  });

  it("R-014-04 only the search box is labelled Search", async () => {
    answer("VIEWER", () => pageOf(THREE));
    renderPage();

    expect(await screen.findAllByLabelText(/Search/)).toHaveLength(1);
  });

  it("R-014-03 Sort by and Order ask for the whole list in that order", async () => {
    answer("VIEWER", () => pageOf(THREE));
    const router = renderPage(`${LIST}?page=2`);

    await userEvent.selectOptions(await screen.findByLabelText("Sort by"), "Company");
    await userEvent.selectOptions(screen.getByLabelText("Order"), "Descending");

    await waitFor(() => expect(router.state.location.search).toBe("?sort=company&dir=desc"));
    expect(listCalls()).toContain(`${LIST}?q=&sort=company&dir=desc&page=1`);
  });

  it("UI-14 an error shows Something went wrong. and Try again asks again", async () => {
    let fail = true;
    answer("VIEWER", () => (fail ? new Error("down") : pageOf(THREE)));
    renderPage();

    expect(await screen.findByText("Something went wrong.")).toBeTruthy();
    fail = false;
    await userEvent.click(screen.getByRole("button", { name: "Try again" }));

    expect(await screen.findByText("3 Contacts")).toBeTruthy();
  });
});

describe("WF-06 filter panel and chips (US-015)", () => {
  it("R-015-06 removing a chip removes only that filter", async () => {
    answer("VIEWER", () => pageOf(THREE));
    const router = renderPage(`${LIST}?hasTagIds=t2&company=%C5%A0ola`);

    await userEvent.click(await screen.findByRole("button", { name: "Remove filter has Tags" }));

    await waitFor(() => expect(router.state.location.search).toBe("?company=%C5%A0ola"));
  });

  it("R-015-06 Clear filters removes every filter and keeps the search and sort", async () => {
    answer("VIEWER", () => pageOf(THREE));
    const router = renderPage(`${LIST}?q=maja&sort=company&hasTagIds=t2&withoutTagIds=t1`);

    await userEvent.click(await screen.findByRole("link", { name: "Clear filters" }));

    await waitFor(() => expect(router.state.location.search).toBe("?q=maja&sort=company"));
    expect(screen.queryByRole("link", { name: "Clear filters" })).toBeNull();
  });

  it("EX-015-02 an empty filtered list shows No Contacts match.", async () => {
    answer("VIEWER", () => pageOf([]));
    renderPage(`${LIST}?hasTagIds=t2&withoutTagIds=t2`);

    expect(await screen.findByText("No Contacts match.")).toBeTruthy();
    expect(screen.queryByText("No Contacts yet.")).toBeNull();
  });
});
