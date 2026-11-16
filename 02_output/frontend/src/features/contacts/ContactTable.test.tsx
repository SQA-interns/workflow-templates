import { render, screen, within } from "@testing-library/react";
import { createMemoryRouter, RouterProvider } from "react-router";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import type { ContactListItem } from "../../api/contacts";
import i18n from "../../i18n";
import { ContactTable } from "./ContactTable";

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

function renderTable(items: ContactListItem[]) {
  const router = createMemoryRouter(
    [{ path: "/groups/:groupId/contacts", element: <ContactTable groupId="g1" items={items} /> }],
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
    expect(headers).toEqual(["Name", "Company", "E-mail", "Phone", "Tags"]);
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
});
