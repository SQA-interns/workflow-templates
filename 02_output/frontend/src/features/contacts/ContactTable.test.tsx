import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createMemoryRouter, RouterProvider } from "react-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { ContactListItem } from "../../api/contacts";
import i18n from "../../i18n";
import { ContactTable } from "./ContactTable";
import type { Selection } from "./useSelection";

const JANEZ: ContactListItem = {
  id: "c1",
  firstName: "Janez",
  lastName: "Novak",
  company: "Podjetje d.o.o.",
  email: "janez.novak@example.si",
  phone: "+386 41 123 456",
  tags: [{ id: "t1", name: "udelezenec2025" }],
  updatedAt: "2026-03-05T10:00:00Z",
};
const ALFA: ContactListItem = {
  id: "c2",
  firstName: null,
  lastName: null,
  company: "Alfa d.o.o.",
  email: null,
  phone: null,
  tags: [],
  updatedAt: "2026-03-05T10:00:00Z",
};

function selectionOf(checked: string[] = []): Selection {
  return {
    checked,
    isChecked: (id) => checked.includes(id),
    check: vi.fn(),
    checkAll: vi.fn(),
  };
}

function renderTable(items: ContactListItem[], selection = selectionOf()) {
  const router = createMemoryRouter(
    [
      {
        path: "/groups/:groupId/contacts",
        element: <ContactTable groupId="g1" items={items} selection={selection} />,
      },
    ],
    { initialEntries: ["/groups/g1/contacts"] },
  );
  render(<RouterProvider router={router} />);
}

beforeEach(async () => {
  await i18n.changeLanguage("en");
});
afterEach(async () => {
  await i18n.changeLanguage("sl");
});

describe("Contact table of WF-06 (R-014-01)", () => {
  it("R-014-01 has the columns Name, Company, E-mail, Phone and Tags", () => {
    renderTable([JANEZ]);

    const headers = screen.getAllByRole("columnheader").map((h) => h.textContent);
    expect(headers).toEqual(["", "Name", "Company", "E-mail", "Phone", "Tags"]);
  });

  it("R-014-06 the name opens the Contact's details", () => {
    renderTable([JANEZ]);

    const link = screen.getByRole("link", { name: "Janez Novak" });
    expect(link.getAttribute("href")).toBe("/groups/g1/contacts/c1");
  });

  it("R-014-01 a row shows the company, e-mail, phone and Tags", () => {
    renderTable([JANEZ]);

    const row = screen.getAllByRole("row")[1];
    expect(row).toBeTruthy();
    const cells = within(row as HTMLElement)
      .getAllByRole("cell")
      .map((c) => c.textContent);
    expect(cells).toEqual([
      "",
      "Janez Novak",
      "Podjetje d.o.o.",
      "janez.novak@example.si",
      "+386 41 123 456",
      "udelezenec2025",
    ]);
  });

  it("EX-014-04 a Contact with only a company shows it in the name position", () => {
    renderTable([ALFA]);

    expect(screen.getByRole("link", { name: "Alfa d.o.o." })).toBeTruthy();
  });

  it("R-014-01 rows keep the order of the API", () => {
    renderTable([ALFA, JANEZ]);

    expect(screen.getAllByRole("link").map((l) => l.textContent)).toEqual([
      "Alfa d.o.o.",
      "Janez Novak",
    ]);
  });

  it("R-016-02 each row has a checkbox named after the Contact that checks it", async () => {
    const selection = selectionOf(["c2"]);
    renderTable([JANEZ, ALFA], selection);

    const janez = screen.getByRole("checkbox", { name: "Check Janez Novak" });
    expect((janez as HTMLInputElement).checked).toBe(false);
    expect(
      (screen.getByRole("checkbox", { name: "Check Alfa d.o.o." }) as HTMLInputElement).checked,
    ).toBe(true);

    await userEvent.click(janez);
    expect(selection.check).toHaveBeenCalledWith("c1", true);
  });

  it("R-016-02 the header checkbox checks every Contact of the Page", async () => {
    const selection = selectionOf(["c2"]);
    renderTable([JANEZ, ALFA], selection);

    await userEvent.click(screen.getByRole("checkbox", { name: "Check all on this Page" }));

    expect(selection.checkAll).toHaveBeenCalledWith(["c1", "c2"], true);
  });

  it("R-016-02 the header checkbox is checked when the whole Page is", async () => {
    const selection = selectionOf(["c1", "c2"]);
    renderTable([JANEZ, ALFA], selection);

    const all = screen.getByRole("checkbox", { name: "Check all on this Page" });
    expect((all as HTMLInputElement).checked).toBe(true);

    await userEvent.click(all);
    expect(selection.checkAll).toHaveBeenCalledWith(["c1", "c2"], false);
  });

  it("R-016-02 the header checkbox of an empty Page is not checked", () => {
    renderTable([], selectionOf(["c1"]));

    const all = screen.getByRole("checkbox", { name: "Check all on this Page" });
    expect((all as HTMLInputElement).checked).toBe(false);
  });
});
