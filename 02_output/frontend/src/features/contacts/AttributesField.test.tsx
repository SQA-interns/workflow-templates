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
import { NewContactPage } from "./NewContactPage";

vi.mock("../../api/apiClient", () => ({ apiRequest: vi.fn() }));

const DETAILS = "/groups/g1/contacts/c1";

function janez(attributes: { key: string; value: string }[]) {
  return {
    id: "c1",
    versionNumber: 2,
    state: "active",
    fields: { ...emptyFields(), firstName: "Janez", lastName: "Novak" },
    attributes,
    tags: [],
    createdAt: "2026-03-05T09:00:00Z",
    updatedAt: "2026-03-05T10:00:00Z",
    mergedFrom: [],
    mergedInto: null,
  };
}

/** Answers GET with the Contact and PUT or POST with the save function. */
function answer(
  read: () => unknown,
  save: () => Promise<unknown> = () => Promise.resolve({ id: "c1" }),
) {
  vi.mocked(apiRequest).mockImplementation((method: string) =>
    method === "GET" ? Promise.resolve(read()) : save(),
  );
}

function renderAt(path: string) {
  const router = createMemoryRouter(
    [
      { path: "/groups/:groupId/contacts", element: <p>contact list</p> },
      { path: "/groups/:groupId/contacts/new", element: <NewContactPage /> },
      { path: "/groups/:groupId/contacts/:contactId", element: <p>contact details</p> },
      { path: "/groups/:groupId/contacts/:contactId/edit", element: <EditContactPage /> },
    ],
    { initialEntries: [path] },
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

async function addAttribute(key: string, value: string) {
  await userEvent.click(screen.getByRole("button", { name: "Add an Attribute" }));
  await userEvent.type(screen.getAllByLabelText("Key").at(-1) as HTMLElement, key);
  if (value) await userEvent.type(screen.getAllByLabelText("Value").at(-1) as HTMLElement, value);
}

async function save() {
  await userEvent.click(screen.getByRole("button", { name: "Save" }));
}

function sent(method: "PUT" | "POST") {
  return vi
    .mocked(apiRequest)
    .mock.calls.filter(([m]) => m === method)
    .map(([, , body]) => (body as { attributes: unknown }).attributes);
}

beforeEach(async () => {
  vi.mocked(apiRequest).mockReset();
  await i18n.changeLanguage("en");
});
afterEach(async () => {
  await i18n.changeLanguage("sl");
});

describe("WF-08 Attributes of a Contact (US-008)", () => {
  it("R-008-06 the form shows the saved Attributes in the order read", async () => {
    answer(() => janez([{ key: "kompetenca", value: "spletnoProgramiranje" }]));
    renderAt(`${DETAILS}/edit`);

    expect(await screen.findByDisplayValue("kompetenca")).toBeTruthy();
    expect(screen.getByRole("heading", { level: 2, name: "Attributes" })).toBeTruthy();
    expect(screen.getByLabelText<HTMLInputElement>("Value").value).toBe("spletnoProgramiranje");
  });

  it("AC-008-01 an added Attribute is sent trimmed with the Fields", async () => {
    answer(() => janez([]));
    const router = renderAt(`${DETAILS}/edit`);
    await screen.findByDisplayValue("Janez");

    await addAttribute(" kompetenca ", "spletnoProgramiranje");
    await save();

    await waitFor(() => expect(router.state.location.pathname).toBe(DETAILS));
    expect(sent("PUT")).toEqual([[{ key: "kompetenca", value: "spletnoProgramiranje" }]]);
  });

  it("AC-008-02 a changed value is sent", async () => {
    answer(() => janez([{ key: "kompetenca", value: "spletnoProgramiranje" }]));
    renderAt(`${DETAILS}/edit`);
    await screen.findByDisplayValue("kompetenca");

    await userEvent.clear(screen.getByLabelText("Value"));
    await userEvent.type(screen.getByLabelText("Value"), "mobilnoProgramiranje");
    await save();

    await waitFor(() =>
      expect(sent("PUT")).toEqual([[{ key: "kompetenca", value: "mobilnoProgramiranje" }]]),
    );
  });

  it("AC-008-03 the same key twice: The key Kompetenca is used twice. and nothing is sent", async () => {
    answer(() => janez([{ key: "kompetenca", value: "mobilnoProgramiranje" }]));
    renderAt(`${DETAILS}/edit`);
    await screen.findByDisplayValue("kompetenca");

    await addAttribute("Kompetenca", "java");
    await save();

    expect(await screen.findByText("The key Kompetenca is used twice.")).toBeTruthy();
    expect(screen.getAllByLabelText("Key").at(-1)).toBe(document.activeElement);
    expect(sent("PUT")).toEqual([]);
  });

  it("AC-008-04 Remove takes the Attribute out of the save", async () => {
    answer(() => janez([{ key: "kompetenca", value: "mobilnoProgramiranje" }]));
    renderAt(`${DETAILS}/edit`);
    await screen.findByDisplayValue("kompetenca");

    await userEvent.click(screen.getByRole("button", { name: "Remove" }));
    await save();

    await waitFor(() => expect(sent("PUT")).toEqual([[]]));
  });

  it("EX-008-02 a key of only spaces: A key must have 1 to 50 characters.", async () => {
    answer(() => janez([]));
    renderAt(`${DETAILS}/edit`);
    await screen.findByDisplayValue("Janez");

    await addAttribute("   ", "v");
    await save();

    expect(await screen.findByText("A key must have 1 to 50 characters.")).toBeTruthy();
    expect(sent("PUT")).toEqual([]);
  });

  it("EX-008-04 an empty value is sent as empty", async () => {
    answer(() => janez([]));
    renderAt(`${DETAILS}/edit`);
    await screen.findByDisplayValue("Janez");

    await addAttribute("kompetenca", "");
    await save();

    await waitFor(() => expect(sent("PUT")).toEqual([[{ key: "kompetenca", value: "" }]]));
  });

  it("R-008-03 an Attribute problem of the API stands at its row", async () => {
    answer(
      () => janez([{ key: "opis", value: "x" }]),
      () =>
        Promise.reject(
          new ApiError(400, {
            code: "validation",
            errors: [{ field: "attributes[0].value", code: "attribute.valueTooLong" }],
          }),
        ),
    );
    renderAt(`${DETAILS}/edit`);
    await screen.findByDisplayValue("opis");

    await userEvent.type(screen.getByLabelText("Value"), "y");
    await save();

    expect(await screen.findByText("A value may have at most 500 characters.")).toBeTruthy();
    expect(screen.queryByText("Something went wrong.")).toBeNull();
  });

  it("R-008-05 the API's limit of 50 Attributes stands in the Attributes section", async () => {
    answer(
      () => janez([]),
      () =>
        Promise.reject(
          new ApiError(400, {
            code: "validation",
            errors: [{ field: "attributes", code: "attribute.tooMany" }],
          }),
        ),
    );
    renderAt(`${DETAILS}/edit`);
    await screen.findByDisplayValue("Janez");

    await addAttribute("kompetenca", "java");
    await save();

    expect(await screen.findByText("A Contact may have at most 50 Attributes.")).toBeTruthy();
  });

  it("R-008-01 a new Contact is sent with its Attributes", async () => {
    answer(() => []);
    renderAt("/groups/g1/contacts/new");

    await userEvent.type(screen.getByLabelText("First name"), "Janez");
    await addAttribute("kompetenca", "java");
    await save();

    await waitFor(() => expect(sent("POST")).toEqual([[{ key: "kompetenca", value: "java" }]]));
  });
});
