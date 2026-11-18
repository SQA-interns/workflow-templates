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
  createdAt: "2026-03-01T09:00:00Z",
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
  createdAt: "2026-03-02T09:00:00Z",
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

function renderPage(search = "first=c1&second=c2") {
  const router = createMemoryRouter(
    [
      { path: "/groups/:groupId/merge", element: <MergePage /> },
      { path: "/groups/:groupId/duplicates", element: <p>Duplicates list</p> },
      { path: "/groups/:groupId/contacts/:contactId", element: <p>Details of the Primary</p> },
    ],
    { initialEntries: [`/groups/g1/merge?${search}`] },
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

  it("R-018-01 the Contact created first is Primary, whichever the list shows first (D-74)", async () => {
    renderPage("first=c2&second=c1");

    expect(
      (await screen.findByRole<HTMLInputElement>("radio", { name: "Janez Novak" })).checked,
    ).toBe(true);
    expect(radio("J. Novak").checked).toBe(false);
    expect(radio("Janez").checked).toBe(true);
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
  it("UI-14 a failed first Contact shows the error even when the second one loaded", async () => {
    vi.mocked(apiRequest).mockImplementation((_method: string, path: string) =>
      path.endsWith("/c1") ? Promise.reject(new Error("down")) : Promise.resolve(J_NOVAK),
    );
    renderPage();

    expect(await screen.findByRole("button", { name: "Try again" })).toBeTruthy();
    expect(screen.queryByRole("button", { name: "Merge" })).toBeNull();
  });

  it("UI-14 a failed second Contact shows the error even when the first one loaded", async () => {
    vi.mocked(apiRequest).mockImplementation((_method: string, path: string) =>
      path.endsWith("/c2") ? Promise.reject(new Error("down")) : Promise.resolve(JANEZ),
    );
    renderPage();

    expect(await screen.findByRole("button", { name: "Try again" })).toBeTruthy();
    expect(screen.queryByRole("button", { name: "Merge" })).toBeNull();
  });

  it("WF-18 shows the intro and links back to Possible duplicates", async () => {
    renderPage();

    expect(await screen.findByText(/Choose the Primary contact and, per Field/)).toBeTruthy();
    await userEvent.click(screen.getByRole("link", { name: "Possible duplicates" }));

    expect(await screen.findByText("Duplicates list")).toBeTruthy();
  });

  it("WF-18 names the sections and the columns, one radio group per Field", async () => {
    renderPage();

    expect(await screen.findByRole("heading", { name: "Fields" })).toBeTruthy();
    expect(screen.getByRole("heading", { name: "Attributes" })).toBeTruthy();
    expect(screen.getByRole("heading", { name: "Tags" })).toBeTruthy();
    expect(screen.getByText("Field")).toBeTruthy();
    expect(screen.getByText("Other contact")).toBeTruthy();
    expect(screen.getAllByText("Primary contact")).toHaveLength(2);
    const firstName = screen.getByRole("radiogroup", { name: "First name" });
    expect(firstName.textContent).toContain("Janez");
    expect(firstName.textContent).toContain("J.");
    expect(screen.getByRole("radiogroup", { name: "Phone" })).toBeTruthy();
  });

  it("WF-18 the two values of one Field or Attribute form one radio group", async () => {
    renderPage();

    const janez = await screen.findByRole<HTMLInputElement>("radio", { name: "Janez" });
    expect(janez.name).not.toBe("");
    expect(radio("J.").name).toBe(janez.name);
    expect(radio("+386 41 123 456").name).not.toBe(janez.name);
    expect(radio("java").name).not.toBe("");
    expect(radio("python").name).toBe(radio("java").name);
    expect(radio("java").name).not.toBe(janez.name);
  });

  it("R-018-01 a value missing in one Contact is offered as (empty)", async () => {
    vi.mocked(apiRequest).mockImplementation((_method: string, path: string) =>
      Promise.resolve(
        path.endsWith("/c1")
          ? { ...JANEZ, fields: { firstName: "Janez", lastName: "Novak", email: "j@n.si" } }
          : J_NOVAK,
      ),
    );
    renderPage();

    const phone = await screen.findByRole("radiogroup", { name: "Phone" });
    expect(phone.textContent).toContain("(empty)");
    expect(radio("+386 41 123 456").checked).toBe(true);
  });

  it("R-018-01 choosing the second Contact as Primary preselects the first's value where it is blank", async () => {
    merge = vi.fn(() => Promise.resolve({ id: "m1", primaryId: "c2", otherId: "c1" }));
    vi.mocked(apiRequest).mockImplementation((method: string, path: string, body?: unknown) => {
      if (method === "POST") return merge(body);
      return Promise.resolve(
        path.endsWith("/c1") ? JANEZ : { ...J_NOVAK, fields: { ...J_NOVAK.fields, email: "" } },
      );
    });
    renderPage();

    await userEvent.click(await screen.findByRole("radio", { name: "J. Novak" }));

    expect(radio("j@n.si").checked).toBe(true);
    await userEvent.click(screen.getByRole("button", { name: "Merge" }));
    await screen.findByText("Details of the Primary");
    expect(merge).toHaveBeenCalledWith(
      expect.objectContaining({
        primaryId: "c2",
        otherId: "c1",
        fieldChoices: {
          firstName: "primary",
          lastName: "primary",
          email: "other",
          phone: "primary",
        },
      }),
    );
  });

  it("R-018-01 choosing the first Contact again preselects its values against the second", async () => {
    renderPage();

    await userEvent.click(await screen.findByRole("radio", { name: "J. Novak" }));
    await userEvent.click(radio("Janez Novak"));

    expect(radio("Janez").checked).toBe(true);
    expect(radio("+386 41 123 456").checked).toBe(true);
  });

  it("R-018-02 choosing a value per Field and Attribute keeps the other choices", async () => {
    merge = vi.fn(() => Promise.resolve({ id: "m1", primaryId: "c1", otherId: "c2" }));
    renderPage();

    await userEvent.click(await screen.findByRole("radio", { name: "J." }));
    await userEvent.click(radio("python"));

    expect(radio("J.").checked).toBe(true);
    expect(radio("Janez").checked).toBe(false);
    expect(radio("python").checked).toBe(true);
    expect(radio("java").checked).toBe(false);
    await userEvent.click(screen.getByRole("button", { name: "Merge" }));
    await screen.findByText("Details of the Primary");
    expect(merge).toHaveBeenCalledWith(
      expect.objectContaining({
        fieldChoices: { firstName: "other", lastName: "primary", email: "primary", phone: "other" },
        attributeChoices: { kompetenca: "other" },
      }),
    );
  });

  it("EX-018-01 an Attribute with the same value in both is shown once, without a choice", async () => {
    vi.mocked(apiRequest).mockImplementation((_method: string, path: string) =>
      Promise.resolve(
        path.endsWith("/c1")
          ? JANEZ
          : { ...J_NOVAK, attributes: [{ key: "kompetenca", value: "java" }] },
      ),
    );
    renderPage();

    expect(await screen.findByText("kompetenca")).toBeTruthy();
    expect(screen.getByText("java")).toBeTruthy();
    expect(screen.queryByRole("radio", { name: "java" })).toBeNull();
  });

  it("R-018-02 no error is shown before Merge", async () => {
    renderPage();

    await screen.findByRole("button", { name: "Merge" });
    expect(screen.queryByRole("alert")).toBeNull();
  });

  it("R-018-02 another refusal is shown once, below the Tags", async () => {
    merge = () => Promise.reject(new ApiError(409, { code: "merge.notPossible" }));
    renderPage();

    await userEvent.click(await screen.findByRole("button", { name: "Merge" }));

    expect(await screen.findByText("These Contacts cannot be merged.")).toBeTruthy();
    expect(screen.getAllByRole("alert")).toHaveLength(1);
  });

  it("R-018-02 a failure without a problem shows the general error", async () => {
    merge = () => Promise.reject(new Error("network"));
    renderPage();

    await userEvent.click(await screen.findByRole("button", { name: "Merge" }));

    expect((await screen.findByRole("alert")).textContent).toBe("Something went wrong.");
  });
});
