import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createMemoryRouter, RouterProvider } from "react-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "../../api/apiClient";
import { ApiError } from "../../api/problem";
import i18n from "../../i18n";
import { NewContactPage } from "./NewContactPage";

vi.mock("../../api/apiClient", () => ({ apiRequest: vi.fn() }));

const CONTACTS = "/groups/g1/contacts";

function renderPage() {
  const router = createMemoryRouter(
    [
      { path: "/groups/:groupId/contacts", element: <p>contact list</p> },
      { path: "/groups/:groupId/contacts/new", element: <NewContactPage /> },
    ],
    { initialEntries: [`${CONTACTS}/new`] },
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

async function fill(label: string, value: string) {
  await userEvent.type(screen.getByLabelText(label), value);
}

async function save() {
  await userEvent.click(screen.getByRole("button", { name: "Save" }));
}

function posts() {
  return vi.mocked(apiRequest).mock.calls.filter(([method]) => method === "POST");
}

beforeEach(async () => {
  vi.mocked(apiRequest).mockReset();
  vi.mocked(apiRequest).mockImplementation((method: string) =>
    Promise.resolve(method === "POST" ? { id: "c1" } : []),
  );
  await i18n.changeLanguage("en");
});
afterEach(async () => {
  await i18n.changeLanguage("sl");
});

describe("WF-08 Add a Contact (US-006)", () => {
  it("AC-006-01 saving sends every Field trimmed and opens the Contact list", async () => {
    const router = renderPage();
    expect(screen.getByRole("heading", { level: 1, name: "Add a Contact" })).toBeTruthy();

    await fill("First name", " Janez ");
    await fill("Last name", "Novak");
    await fill("E-mail", "janez.novak@example.si");
    await fill("Phone", "+386 41 123 456");
    await fill("Company", "Podjetje d.o.o.");
    await fill("Job title", "Direktor");
    await fill("Street", "Slovenska cesta 1");
    await fill("Postal code", "1000");
    await fill("City", "Ljubljana");
    await userEvent.selectOptions(screen.getByLabelText("Country"), "Slovenia");
    await fill("Note", "Govornik");
    await save();

    expect(posts()).toEqual([
      [
        "POST",
        "/groups/g1/contacts",
        {
          fields: {
            firstName: "Janez",
            lastName: "Novak",
            email: "janez.novak@example.si",
            phone: "+386 41 123 456",
            company: "Podjetje d.o.o.",
            jobTitle: "Direktor",
            street: "Slovenska cesta 1",
            postalCode: "1000",
            city: "Ljubljana",
            country: "SI",
            note: "Govornik",
          },
          attributes: [],
          tags: [],
        },
      ],
    ]);
    await waitFor(() => expect(router.state.location.pathname).toBe(CONTACTS));
  });

  it("AC-006-02 only a company is saved, the other Fields empty", async () => {
    renderPage();

    await fill("Company", "Podjetje d.o.o.");
    await save();

    await waitFor(() => expect(posts()).toHaveLength(1));
    const body = posts()[0]?.[2] as { fields: Record<string, string | null> };
    expect(body.fields.company).toBe("Podjetje d.o.o.");
    expect(body.fields.firstName).toBeNull();
    expect(body.fields.country).toBeNull();
  });

  it("AC-006-03 only an e-mail: Enter a first name, last name or company. and nothing is sent", async () => {
    renderPage();

    await fill("E-mail", "info@example.si");
    await save();

    expect((await screen.findByRole("alert")).textContent).toBe(
      "Enter a first name, last name or company.",
    );
    expect(posts()).toEqual([]);
    expect((screen.getByLabelText("E-mail") as HTMLInputElement).value).toBe("info@example.si");
  });

  it("AC-006-04 an invalid e-mail is shown next to the field and nothing is sent", async () => {
    renderPage();

    await fill("First name", "Janez");
    await fill("E-mail", "janez@");
    await save();

    const email = screen.getByLabelText("E-mail");
    expect(email.getAttribute("aria-invalid")).toBe("true");
    expect(screen.getByText("Enter a valid e-mail address.").id).toBe(
      email.getAttribute("aria-errormessage"),
    );
    expect(posts()).toEqual([]);
  });

  it("EX-006-02 a first name of 101 characters: First name may have at most 100 characters.", async () => {
    renderPage();

    await userEvent.click(screen.getByLabelText("First name"));
    await userEvent.paste("J".repeat(101));
    await save();

    expect(await screen.findByText("First name may have at most 100 characters.")).toBeTruthy();
    expect(posts()).toEqual([]);
  });

  it("D-23 a phone with a dot: Enter a valid phone number.", async () => {
    renderPage();

    await fill("First name", "Janez");
    await fill("Phone", "041.123.456");
    await save();

    expect(await screen.findByText("Enter a valid phone number.")).toBeTruthy();
    expect(posts()).toEqual([]);
  });

  it("AC-006-06 the API's refusal is shown and the values are kept", async () => {
    vi.mocked(apiRequest).mockRejectedValue(new ApiError(403, { code: "auth.forbidden" }));
    renderPage();

    await fill("First name", "Janez");
    await save();

    expect((await screen.findByRole("alert")).textContent).toBe("You are not allowed to do this.");
    expect((screen.getByLabelText("First name") as HTMLInputElement).value).toBe("Janez");
  });

  it("R-006-03 a Field problem of the API is shown next to its field", async () => {
    vi.mocked(apiRequest).mockRejectedValue(
      new ApiError(400, {
        code: "validation",
        errors: [{ field: "note", code: "field.tooLong", params: { field: "note", max: 2000 } }],
      }),
    );
    renderPage();

    await fill("First name", "Janez");
    await save();

    expect(await screen.findByText("Note may have at most 2,000 characters.")).toBeTruthy();
  });

  it("UI-12 Cancel returns to the Contact list without saving", async () => {
    const router = renderPage();

    await fill("First name", "Janez");
    await userEvent.click(screen.getByRole("button", { name: "Cancel" }));

    expect(router.state.location.pathname).toBe(CONTACTS);
    expect(posts()).toEqual([]);
  });

  it("R-006-05 the country is chosen from a list; empty by default", () => {
    renderPage();

    const country = screen.getByLabelText("Country") as HTMLSelectElement;
    expect(country.value).toBe("");
    expect(screen.getByRole("option", { name: "Slovenia" })).toBeTruthy();
  });

  it("AC-006-05 leaving an existing e-mail warns with its Contact and saving still works", async () => {
    vi.mocked(apiRequest).mockImplementation((method: string, path: string) =>
      Promise.resolve(
        method === "POST"
          ? { id: "c2" }
          : path.includes("email-matches")
            ? [{ id: "c1", name: "Janez Novak" }]
            : [],
      ),
    );
    const router = renderPage();

    await fill("First name", "J.");
    await fill("Last name", "Novak");
    await fill("E-mail", " Janez.Novak@example.si ");
    await userEvent.tab();

    expect(
      await screen.findByText("A Contact with this e-mail already exists: Janez Novak."),
    ).toBeTruthy();
    expect(apiRequest).toHaveBeenCalledWith(
      "GET",
      "/groups/g1/contacts/email-matches?email=Janez.Novak%40example.si",
    );
    await save();
    await waitFor(() => expect(posts()).toHaveLength(1));
    await waitFor(() => expect(router.state.location.pathname).toBe(CONTACTS));
  });

  it("R-006-06 an e-mail without a match shows no warning", async () => {
    renderPage();

    await fill("E-mail", "maja.zupan@example.si");
    await userEvent.tab();

    await waitFor(() =>
      expect(apiRequest).toHaveBeenCalledWith(
        "GET",
        "/groups/g1/contacts/email-matches?email=maja.zupan%40example.si",
      ),
    );
    expect(screen.queryByText(/already exists/)).toBeNull();
  });

  it("R-006-06 a blank or invalid e-mail is not looked up", async () => {
    renderPage();

    await userEvent.click(screen.getByLabelText("E-mail"));
    await userEvent.tab();
    await fill("E-mail", "janez@");
    await userEvent.tab();

    expect(vi.mocked(apiRequest).mock.calls.filter(([method]) => method === "GET")).toEqual([]);
  });
});
