import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createMemoryRouter, RouterProvider } from "react-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { apiDownload, apiRequest } from "../../api/apiClient";
import type { ContactListItem } from "../../api/contacts";
import i18n from "../../i18n";
import { ContactListPage } from "./ContactListPage";

vi.mock("../../api/apiClient", () => ({ apiRequest: vi.fn(), apiDownload: vi.fn() }));

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
  async function openFilters() {
    await userEvent.click(await screen.findByRole("button", { name: "Filters" }));
  }

  it("AC-015-01 has Tags is applied, shown as a chip and the panel closes", async () => {
    answer("VIEWER", () => pageOf(THREE));
    const router = renderPage();
    await openFilters();

    await userEvent.selectOptions(await screen.findByLabelText("has Tags"), "udelezenec2025");
    await userEvent.click(screen.getByRole("button", { name: "Apply" }));

    await waitFor(() => expect(router.state.location.search).toBe("?hasTagIds=t2"));
    expect(listCalls()).toContain(`${LIST}?q=&sort=name&dir=asc&page=1&hasTagIds=t2`);
    expect(await screen.findByText("has Tags: udelezenec2025")).toBeTruthy();
    expect(screen.queryByRole("button", { name: "Apply" })).toBeNull();
    expect(screen.getByRole("button", { name: "Filters" }).getAttribute("aria-expanded")).toBe(
      "false",
    );
  });

  it("R-015-01 … R-015-04 every filter of the panel goes to the query", async () => {
    answer("VIEWER", () => pageOf(THREE));
    const router = renderPage();
    await openFilters();

    await userEvent.selectOptions(await screen.findByLabelText("has Tags"), "udelezenec2025");
    await userEvent.selectOptions(screen.getByLabelText("has Tags"), "neObvescaj");
    await userEvent.selectOptions(screen.getByLabelText("without Tags"), "neObvescaj");
    await userEvent.type(screen.getByLabelText("Attribute key"), "Kompetenca");
    await userEvent.type(screen.getByLabelText("Attribute value"), "SPLETNO");
    await userEvent.type(screen.getByLabelText("Company"), "Šola");
    await userEvent.selectOptions(screen.getByLabelText("Country"), "Slovenia");
    await userEvent.click(screen.getByRole("button", { name: "Apply" }));

    await waitFor(() =>
      expect(router.state.location.search).toBe(
        "?hasTagIds=t2&hasTagIds=t1&withoutTagIds=t1&attributeKey=Kompetenca" +
          "&attributeValue=SPLETNO&company=%C5%A0ola&country=SI",
      ),
    );
    expect(await screen.findByText("has Tags: udelezenec2025, neObvescaj")).toBeTruthy();
    expect(screen.getByText("without Tags: neObvescaj")).toBeTruthy();
    expect(screen.getByText("Attribute: Kompetenca = SPLETNO")).toBeTruthy();
    expect(screen.getByText("Company: Šola")).toBeTruthy();
    expect(screen.getByText("Country: Slovenia")).toBeTruthy();
  });

  it("R-015-01 a chosen Tag can be taken out of the panel again", async () => {
    answer("VIEWER", () => pageOf(THREE));
    renderPage();
    await openFilters();

    await userEvent.selectOptions(await screen.findByLabelText("has Tags"), "udelezenec2025");
    await userEvent.click(screen.getByRole("button", { name: "Remove udelezenec2025" }));

    expect(screen.queryByRole("button", { name: "Remove udelezenec2025" })).toBeNull();
  });

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

  it("R-015-06 Clear filters in the panel removes every filter", async () => {
    answer("VIEWER", () => pageOf(THREE));
    const router = renderPage(`${LIST}?country=SI`);
    await openFilters();

    await userEvent.click(screen.getByRole("button", { name: "Clear filters" }));

    await waitFor(() => expect(router.state.location.search).toBe(""));
  });

  it("R-015-07 applying keeps the sort and starts at Page 1", async () => {
    answer("VIEWER", () => pageOf(THREE));
    const router = renderPage(`${LIST}?sort=email&dir=desc&page=3`);
    await openFilters();

    await userEvent.selectOptions(await screen.findByLabelText("Country"), "Austria");
    await userEvent.click(screen.getByRole("button", { name: "Apply" }));

    await waitFor(() =>
      expect(router.state.location.search).toBe("?sort=email&dir=desc&country=AT"),
    );
  });

  it("R-015-06 the panel opens with the Active filters", async () => {
    answer("VIEWER", () => pageOf(THREE));
    renderPage(`${LIST}?attributeKey=jezik&country=AT`);
    await openFilters();

    expect((screen.getByLabelText("Attribute key") as HTMLInputElement).value).toBe("jezik");
    expect((screen.getByLabelText("Country") as HTMLSelectElement).value).toBe("AT");
  });

  it("EX-015-02 an empty filtered list shows No Contacts match.", async () => {
    answer("VIEWER", () => pageOf([]));
    renderPage(`${LIST}?hasTagIds=t2&withoutTagIds=t2`);

    expect(await screen.findByText("No Contacts match.")).toBeTruthy();
    expect(screen.queryByText("No Contacts yet.")).toBeNull();
  });
});

describe("WF-06 Export and Checked contacts (US-016)", () => {
  const check = (name: string) =>
    userEvent.click(screen.getByRole("checkbox", { name: `Check ${name}` }));

  it("AC-016-02 a checked row is counted and Export checked sends it", async () => {
    vi.mocked(apiDownload).mockReturnValue(new Promise(() => {}));
    answer("VIEWER", () => pageOf(THREE));
    renderPage(`${LIST}?q=a`);
    await screen.findByText("3 Contacts");

    await check("Maja Zupan");
    expect(screen.getByText("1 checked")).toBeTruthy();
    await userEvent.click(screen.getByRole("button", { name: "Export checked (1)" }));

    expect(apiDownload).toHaveBeenCalledWith(
      "/groups/g1/exports",
      expect.objectContaining({ q: "a", contactIds: ["c3"] }),
    );
  });

  it("D-24 (b) checks stay across Pages and are cleared when the search changes", async () => {
    answer("VIEWER", (path) =>
      path.includes("page=2") ? pageOf([item("c4", "Ana", "Bizjak")], 51, 2) : pageOf(THREE, 51),
    );
    renderPage();
    await screen.findByText("51 Contacts");
    await check("Maja Zupan");

    await userEvent.click(screen.getByRole("button", { name: "Next" }));
    await screen.findByRole("link", { name: "Ana Bizjak" });
    await check("Ana Bizjak");
    expect(screen.getByText("2 checked")).toBeTruthy();

    await userEvent.type(screen.getByLabelText("Search"), "x");
    await waitFor(() => expect(screen.queryByText("2 checked")).toBeNull());
    expect(screen.getByRole("button", { name: "Export checked" })).toBeTruthy();
  });

  it("EX-016-02 an empty filtered list: Export disabled, Nothing to export.", async () => {
    answer("VIEWER", () => pageOf([]));
    renderPage(`${LIST}?q=xyz`);

    expect(await screen.findByText("No Contacts match.")).toBeTruthy();
    expect(screen.getByRole("button", { name: "Export" }).hasAttribute("disabled")).toBe(true);
    expect(screen.getByText("Nothing to export.")).toBeTruthy();
  });
});

describe("WF-06 Contact list states and paging (US-014)", () => {
  it("UI-14 the loading state is named Loading …", () => {
    vi.mocked(apiRequest).mockImplementation(() => new Promise(() => undefined));
    renderPage();

    expect(screen.getByRole("status", { name: "Loading …" })).toBeTruthy();
  });

  it("R-014-04 the search box hints what it searches and Order offers Ascending", async () => {
    answer("VIEWER", () => pageOf(THREE));
    renderPage();

    const search = await screen.findByLabelText("Search");
    expect(search.getAttribute("placeholder")).toBe("Name, e-mail, phone, company or city");
    expect(
      within(screen.getByLabelText("Order")).getByRole("option", { name: "Ascending" }),
    ).toBeTruthy();
  });

  it("UI-17 an Owner gets Add a Contact in the header and in the mobile action bar", async () => {
    answer("OWNER", () => pageOf(THREE));
    renderPage();

    await screen.findByText("3 Contacts");
    expect(screen.getAllByRole("button", { name: "Add a Contact" })).toHaveLength(2);
  });

  it("AC-014-05 the Pages are a navigation named Pages; Previous goes back one Page", async () => {
    answer("VIEWER", (path) => pageOf(THREE, 120, Number(/page=(\d+)/.exec(path)?.[1])));
    const router = renderPage(`${LIST}?page=2`);

    const pages = await screen.findByRole("navigation", { name: "Pages" });
    expect(within(pages).getByText("Page 2 of 3")).toBeTruthy();
    const previous = within(pages).getByRole("button", { name: "Previous" }) as HTMLButtonElement;
    expect(previous.disabled).toBe(false);
    await userEvent.click(previous);

    expect(await screen.findByText("Page 1 of 3")).toBeTruthy();
    expect(router.state.location.search).toBe("");
    expect(listCalls()).toContain(`${LIST}?q=&sort=name&dir=asc&page=1`);
  });

  it("AC-014-05 Next is disabled on the last Page", async () => {
    answer("VIEWER", (path) => pageOf(THREE, 120, Number(/page=(\d+)/.exec(path)?.[1])));
    renderPage(`${LIST}?page=3`);

    expect(await screen.findByText("Page 3 of 3")).toBeTruthy();
    expect((screen.getByRole("button", { name: "Next" }) as HTMLButtonElement).disabled).toBe(true);
  });

  it("AC-014-02 an unchanged search box never resets the Page", async () => {
    answer("VIEWER", (path) => pageOf(THREE, 120, Number(/page=(\d+)/.exec(path)?.[1])));
    const router = renderPage(`${LIST}?page=2`);
    await screen.findByText("Page 2 of 3");

    await new Promise((resolve) => setTimeout(resolve, 400));

    expect(router.state.location.search).toBe("?page=2");
  });

  it("AC-014-02 typing asks only for the whole search, not for every letter", async () => {
    answer("VIEWER", () => pageOf(THREE));
    const router = renderPage();

    await userEvent.type(await screen.findByLabelText("Search"), "cuk");

    await waitFor(() => expect(router.state.location.search).toBe("?q=cuk"));
    expect(listCalls().filter((path) => path.includes("q=c&") || path.includes("q=cu&"))).toEqual(
      [],
    );
  });
});

describe("WF-06 filter panel and chips, details (US-015)", () => {
  async function openFilters() {
    await userEvent.click(await screen.findByRole("button", { name: "Filters" }));
  }

  it("R-015-03 a value without a key is no filter", async () => {
    answer("VIEWER", () => pageOf(THREE));
    const router = renderPage();
    await openFilters();

    await userEvent.type(screen.getByLabelText("Attribute key"), "   ");
    await userEvent.type(screen.getByLabelText("Attribute value"), "SPLETNO");
    await userEvent.click(screen.getByRole("button", { name: "Apply" }));

    await waitFor(() =>
      expect(screen.getByRole("button", { name: "Filters" }).getAttribute("aria-expanded")).toBe(
        "false",
      ),
    );
    expect(router.state.location.search).not.toContain("attributeValue");
    expect(listCalls().some((path) => path.includes("attributeValue"))).toBe(false);
  });

  it("R-015-01 the panel is a region named Filters; each Tag picker says what it filters", async () => {
    answer("VIEWER", () => pageOf(THREE));
    renderPage();
    await openFilters();

    expect(screen.getByRole("region", { name: "Filters" })).toBeTruthy();
    const hint = (label: string) => {
      const id = screen.getByLabelText(label).getAttribute("aria-describedby") ?? "";
      return document.getElementById(id)?.textContent;
    };
    expect(hint("has Tags")).toBe("Contacts with all of these Tags");
    expect(hint("without Tags")).toBe("Contacts with none of these Tags");
    for (const label of ["has Tags", "without Tags", "Country"]) {
      expect(
        within(screen.getByLabelText(label)).getByRole("option", { name: "Select …" }),
      ).toBeTruthy();
    }
  });

  it("R-015-01 nothing chosen: the Tag pickers show no chips", async () => {
    answer("VIEWER", () => pageOf(THREE));
    renderPage();
    await openFilters();

    expect(within(screen.getByRole("region", { name: "Filters" })).queryAllByRole("list")).toEqual(
      [],
    );
  });

  it("R-015-01 a chosen Tag is no longer offered, and taking one out keeps the others", async () => {
    answer("VIEWER", () => pageOf(THREE));
    renderPage();
    await openFilters();

    await userEvent.selectOptions(await screen.findByLabelText("has Tags"), "udelezenec2025");
    await userEvent.selectOptions(screen.getByLabelText("has Tags"), "neObvescaj");
    expect(
      within(screen.getByLabelText("has Tags"))
        .getAllByRole("option")
        .map((option) => option.textContent),
    ).toEqual(["Select …"]);
    await userEvent.click(screen.getByRole("button", { name: "Remove udelezenec2025" }));

    expect(screen.getByRole("button", { name: "Remove neObvescaj" })).toBeTruthy();
    expect(
      within(screen.getByLabelText("has Tags"))
        .getAllByRole("option")
        .map((option) => option.textContent),
    ).toEqual(["Select …", "udelezenec2025"]);
  });

  it("R-015-05 a Tag of the URL that the Group does not have reads …", async () => {
    answer("VIEWER", () => pageOf(THREE));
    renderPage(`${LIST}?hasTagIds=t9`);

    expect(await screen.findByText("has Tags: …")).toBeTruthy();
    await openFilters();
    expect(screen.getByRole("button", { name: "Remove …" })).toBeTruthy();
  });

  it("R-015-01 until the Tags are read, the Tag pickers offer no Tag", async () => {
    vi.mocked(apiRequest).mockImplementation((_method: string, path: string) => {
      if (path === "/groups") {
        return Promise.resolve([{ id: "g1", name: "Konferenca 2025", role: "VIEWER" }]);
      }
      if (path === "/groups/g1/tags") return new Promise(() => undefined);
      return Promise.resolve(pageOf(THREE));
    });
    renderPage();
    await openFilters();

    expect(within(screen.getByLabelText("has Tags")).getAllByRole("option")).toHaveLength(1);
  });

  it("R-015-06 an Attribute key alone is shown as its key, and its chip removes key and value", async () => {
    answer("VIEWER", () => pageOf(THREE));
    const router = renderPage(`${LIST}?attributeKey=jezik&company=Alfa`);

    expect(await screen.findByText("Attribute: jezik")).toBeTruthy();
    await userEvent.click(screen.getByRole("button", { name: "Remove filter Attribute" }));

    await waitFor(() => expect(router.state.location.search).toBe("?company=Alfa"));
  });

  it("R-015-06 removing the Attribute chip removes its value too", async () => {
    answer("VIEWER", () => pageOf(THREE));
    const router = renderPage(`${LIST}?attributeKey=jezik&attributeValue=sl&country=SI`);

    await userEvent.click(await screen.findByRole("button", { name: "Remove filter Attribute" }));

    await waitFor(() => expect(router.state.location.search).toBe("?country=SI"));
  });

  it("R-015-06 the without Tags, Company and Country chips each remove only their filter", async () => {
    answer("VIEWER", () => pageOf(THREE));
    const router = renderPage(`${LIST}?withoutTagIds=t1&company=Alfa&country=SI`);

    await userEvent.click(
      await screen.findByRole("button", { name: "Remove filter without Tags" }),
    );
    await waitFor(() => expect(router.state.location.search).toBe("?company=Alfa&country=SI"));

    await userEvent.click(screen.getByRole("button", { name: "Remove filter Company" }));
    await waitFor(() => expect(router.state.location.search).toBe("?country=SI"));

    await userEvent.click(screen.getByRole("button", { name: "Remove filter Country" }));
    await waitFor(() => expect(router.state.location.search).toBe(""));
  });
});
