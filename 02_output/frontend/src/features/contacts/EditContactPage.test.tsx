import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createMemoryRouter, RouterProvider } from "react-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "../../api/apiClient";
import { ApiError } from "../../api/problem";
import i18n from "../../i18n";
import { emptyFields } from "./contactFields";
import { EditContactPage } from "./EditContactPage";

vi.mock("../../api/apiClient", () => ({ apiRequest: vi.fn() }));

const DETAILS = "/groups/g1/contacts/c1";

function janez(versionNumber: number, fields: Record<string, string | null> = {}) {
  return {
    id: "c1",
    versionNumber,
    state: "active",
    fields: {
      ...emptyFields(),
      firstName: "Janez",
      lastName: "Novak",
      email: "janez.novak@example.si",
      phone: null,
      company: null,
      jobTitle: null,
      street: null,
      postalCode: null,
      city: null,
      country: null,
      note: null,
      ...fields,
    },
    attributes: [{ key: "kompetenca", value: "spletnoProgramiranje" }],
    tags: [{ id: "t1", name: "udelezenec2025" }],
    createdAt: "2026-03-05T09:00:00Z",
    updatedAt: "2026-03-05T10:00:00Z",
    mergedFrom: [],
    mergedInto: null,
  };
}

/** Answers GET with the Contact the read function gives and PUT with the save function. */
function answer(read: () => unknown, save: () => Promise<unknown> = () => Promise.resolve({})) {
  vi.mocked(apiRequest).mockImplementation((method: string) =>
    method === "PUT" ? save() : Promise.resolve(read()),
  );
}

function renderPage() {
  const router = createMemoryRouter(
    [
      { path: "/groups/:groupId/contacts/:contactId", element: <p>contact details</p> },
      { path: "/groups/:groupId/contacts/:contactId/edit", element: <EditContactPage /> },
    ],
    { initialEntries: [`${DETAILS}/edit`] },
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

async function replace(label: string, value: string) {
  await userEvent.clear(screen.getByLabelText(label));
  if (value) await userEvent.paste(value);
}

async function save() {
  await userEvent.click(screen.getByRole("button", { name: "Save" }));
}

function puts() {
  return vi.mocked(apiRequest).mock.calls.filter(([method]) => method === "PUT");
}

beforeEach(async () => {
  vi.mocked(apiRequest).mockReset();
  await i18n.changeLanguage("en");
});
afterEach(async () => {
  await i18n.changeLanguage("sl");
});

describe("WF-08 Edit Contact (US-007)", () => {
  it("AC-007-01 the form shows the Contact; saving sends the Fields with the Version read", async () => {
    answer(() => janez(1));
    const router = renderPage();

    expect(await screen.findByDisplayValue("janez.novak@example.si")).toBeTruthy();
    expect(screen.getByRole("heading", { level: 1, name: "Edit Contact" })).toBeTruthy();
    await replace("E-mail", " janez@novak.si ");
    await save();

    await waitFor(() => expect(router.state.location.pathname).toBe(DETAILS));
    expect(puts()).toEqual([
      [
        "PUT",
        "/groups/g1/contacts/c1",
        {
          fields: { ...janez(1).fields, email: "janez@novak.si" },
          attributes: [{ key: "kompetenca", value: "spletnoProgramiranje" }],
          tags: ["udelezenec2025"],
          versionNumber: 1,
        },
      ],
    ]);
  });

  it("AC-007-02 a stale Version shows who changed it; Reload shows the current Version", async () => {
    let current = janez(2);
    answer(
      () => current,
      () =>
        Promise.reject(
          new ApiError(409, { code: "version.conflict", params: { changedBy: "Marko Kranjc" } }),
        ),
    );
    renderPage();
    await screen.findByDisplayValue("janez.novak@example.si");

    current = janez(3, { city: "Maribor" });
    await replace("Job title", "CEO");
    await save();

    expect(await screen.findByText("Contact was changed by Marko Kranjc, reload")).toBeTruthy();
    await userEvent.click(screen.getByRole("button", { name: "Reload" }));
    await waitFor(() =>
      expect(screen.getByLabelText<HTMLInputElement>("City").value).toBe("Maribor"),
    );
    expect(screen.getByLabelText<HTMLInputElement>("Job title").value).toBe("");
    expect(screen.queryByText("Contact was changed by Marko Kranjc, reload")).toBeNull();
  });

  it("AC-007-03 clearing every name part shows the error and sends nothing", async () => {
    answer(() => janez(1));
    renderPage();
    await screen.findByDisplayValue("Janez");

    await replace("First name", "");
    await replace("Last name", "");
    await save();

    expect(await screen.findByText("Enter a first name, last name or company.")).toBeTruthy();
    expect(puts()).toEqual([]);
  });

  it("AC-007-04 saving without changes: No changes to save.", async () => {
    answer(
      () => janez(2, { city: "Maribor" }),
      () => Promise.reject(new ApiError(409, { code: "contact.noChanges" })),
    );
    const router = renderPage();
    await screen.findByDisplayValue("Maribor");

    await save();

    expect(await screen.findByText("No changes to save.")).toBeTruthy();
    expect(router.state.location.pathname).toBe(`${DETAILS}/edit`);
  });

  it("EX-007-03 a Contact in the recycle bin: This Contact is in the recycle bin.", async () => {
    answer(
      () => janez(1),
      () => Promise.reject(new ApiError(409, { code: "contact.inRecycleBin" })),
    );
    renderPage();
    await screen.findByDisplayValue("Janez");

    await replace("City", "Maribor");
    await save();

    expect(await screen.findByText("This Contact is in the recycle bin.")).toBeTruthy();
  });

  it("EX-007-05 a Contact that is not the Group's shows the not-found page", async () => {
    vi.mocked(apiRequest).mockRejectedValue(new ApiError(404, { code: "notFound" }));
    renderPage();

    expect(await screen.findByText("This page cannot be found.")).toBeTruthy();
  });

  it("UI-14 another failure to read the Contact shows the error, not the not-found page", async () => {
    vi.mocked(apiRequest).mockRejectedValue(new ApiError(403, { code: "auth.forbidden" }));
    renderPage();

    expect(await screen.findByText("Something went wrong.")).toBeTruthy();
    expect(screen.queryByText("This page cannot be found.")).toBeNull();
  });

  it("UI-12 Cancel returns to the Contact's details without saving", async () => {
    answer(() => janez(1));
    const router = renderPage();
    await screen.findByDisplayValue("Janez");

    await userEvent.click(screen.getByRole("button", { name: "Cancel" }));

    expect(router.state.location.pathname).toBe(DETAILS);
    expect(puts()).toEqual([]);
  });
});
