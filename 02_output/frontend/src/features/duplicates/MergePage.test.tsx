import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createMemoryRouter, RouterProvider } from "react-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "../../api/apiClient";
import { ApiError } from "../../api/problem";
import i18n from "../../i18n";
import { emptyFields } from "../contacts/contactFields";
import { MergePage } from "./MergePage";

vi.mock("../../api/apiClient", () => ({ apiRequest: vi.fn() }));

/** "Janez Novak" at Version 3 without phone, and "J. Novak" with phone (AC-018-01). */
const JANEZ = {
  id: "c1",
  versionNumber: 3,
  state: "active",
  fields: { ...emptyFields(), firstName: "Janez", lastName: "Novak", email: "j@n.si" },
  attributes: [{ key: "kompetenca", value: "java" }],
  tags: [{ id: "t1", name: "udelezenec2025" }],
  updatedAt: "2026-03-05T10:00:00Z",
  mergedFrom: [],
  mergedInto: null,
};
const J_NOVAK = {
  ...JANEZ,
  id: "c2",
  versionNumber: 1,
  fields: { ...JANEZ.fields, firstName: "J.", phone: "+386 41 123 456" },
  attributes: [{ key: "kompetenca", value: "python" }],
  tags: [{ id: "t2", name: "vip" }],
};

let merge: (body: unknown) => Promise<unknown>;

function answer() {
  vi.mocked(apiRequest).mockImplementation((method: string, path: string, body?: unknown) => {
    if (method === "POST") return merge(body);
    if (path === "/groups") {
      return Promise.resolve([{ id: "g1", name: "Konferenca 2025", role: "EDITOR" }]);
    }
    return Promise.resolve(path.endsWith("/c1") ? JANEZ : J_NOVAK);
  });
}

function renderPage() {
  const router = createMemoryRouter(
    [
      { path: "/groups/:groupId/merge", element: <MergePage /> },
      { path: "/groups/:groupId/duplicates", element: <p>Duplicates list</p> },
      { path: "/groups/:groupId/contacts/:contactId", element: <p>Details of the Primary</p> },
    ],
    { initialEntries: ["/groups/g1/merge?first=c1&second=c2"] },
  );
  render(
    <QueryClientProvider
      client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}
    >
      <RouterProvider router={router} />
    </QueryClientProvider>,
  );
}

function radio(name: string): HTMLInputElement {
  return screen.getByRole<HTMLInputElement>("radio", { name });
}

beforeEach(async () => {
  vi.mocked(apiRequest).mockReset();
  merge = () => Promise.resolve({ id: "m1", primaryId: "c1", otherId: "c2", undone: false });
  answer();
  await i18n.changeLanguage("en");
});
afterEach(async () => {
  await i18n.changeLanguage("sl");
});

describe("WF-18 Merge (US-018)", () => {
  it("AC-018-05 the first Contact is Primary; a blank Primary value preselects the Other's", async () => {
    renderPage();

    expect(await screen.findByRole("heading", { level: 1, name: "Merge" })).toBeTruthy();
    expect(await screen.findByRole("heading", { name: "Primary contact" })).toBeTruthy();
    expect(radio("Janez Novak").checked).toBe(true);
    expect(radio("J. Novak").checked).toBe(false);
    expect(radio("Janez").checked).toBe(true);
    expect(radio("+386 41 123 456").checked).toBe(true);
  });

  it("R-018-01 choosing the other Primary contact preselects its values", async () => {
    renderPage();

    await userEvent.click(await screen.findByRole("radio", { name: "J. Novak" }));

    expect(radio("J.").checked).toBe(true);
    expect(radio("Janez").checked).toBe(false);
  });

  it("R-018-02 Merge sends the Primary contact, the Version numbers and every choice", async () => {
    merge = vi.fn(() => Promise.resolve({ id: "m1", primaryId: "c1", otherId: "c2" }));
    renderPage();

    await userEvent.click(await screen.findByRole("radio", { name: "python" }));
    await userEvent.click(screen.getByRole("button", { name: "Merge" }));

    expect(await screen.findByText("Details of the Primary")).toBeTruthy();
    expect(apiRequest).toHaveBeenCalledWith("POST", "/groups/g1/merges", {
      primaryId: "c1",
      otherId: "c2",
      primaryVersionNumber: 3,
      otherVersionNumber: 1,
      fieldChoices: {
        firstName: "primary",
        lastName: "primary",
        email: "primary",
        phone: "other",
      },
      attributeChoices: { kompetenca: "other" },
    });
  });

  it("R-018-02 shows the Tags and the Attributes of both", async () => {
    renderPage();

    expect(await screen.findByText("udelezenec2025")).toBeTruthy();
    expect(screen.getByText("vip")).toBeTruthy();
    expect(radio("java").checked).toBe(false);
    expect(radio("python").checked).toBe(false);
  });

  it("AC-018-03 a refused Merge shows Choose a value for kompetenca.", async () => {
    merge = () =>
      Promise.reject(
        new ApiError(400, { code: "merge.choiceRequired", params: { key: "kompetenca" } }),
      );
    renderPage();

    await userEvent.click(await screen.findByRole("button", { name: "Merge" }));

    expect(await screen.findByText("Choose a value for kompetenca.")).toBeTruthy();
  });

  it("EX-018-06 Cancel goes back to Possible duplicates without a change", async () => {
    renderPage();

    await userEvent.click(await screen.findByRole("button", { name: "Cancel" }));

    expect(await screen.findByText("Duplicates list")).toBeTruthy();
    expect(apiRequest).not.toHaveBeenCalledWith("POST", expect.anything(), expect.anything());
  });

  it("UI-14 shows loading, then an error with Try again", async () => {
    vi.mocked(apiRequest).mockRejectedValue(new Error("down"));

    renderPage();

    expect(screen.getAllByRole("status").length).toBeGreaterThan(0);
    expect((await screen.findAllByRole("button", { name: "Try again" })).length).toBeGreaterThan(0);
  });
});
