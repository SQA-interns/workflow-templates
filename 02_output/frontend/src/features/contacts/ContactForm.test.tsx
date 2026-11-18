import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createMemoryRouter, RouterProvider } from "react-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "../../api/apiClient";
import { ApiError } from "../../api/problem";
import i18n from "../../i18n";
import { type ContactFields, emptyFields } from "./contactFields";
import { ContactForm } from "./ContactForm";

vi.mock("../../api/apiClient", () => ({ apiRequest: vi.fn() }));

const JANEZ: ContactFields = { ...emptyFields(), firstName: "Janez", lastName: "Novak" };

function renderForm(props: { error?: unknown; onReload?: () => void; initial?: ContactFields }) {
  const onSave = vi.fn();
  const router = createMemoryRouter(
    [
      {
        path: "/form",
        element: (
          <ContactForm
            title="Edit Contact"
            initial={props.initial ?? JANEZ}
            backTo="/back"
            busy={false}
            error={props.error}
            onSave={onSave}
            onCancel={vi.fn()}
            onReload={props.onReload}
          />
        ),
      },
    ],
    { initialEntries: ["/form"] },
  );
  render(
    <QueryClientProvider client={new QueryClient()}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  );
  return onSave;
}

function describedText(label: string) {
  const ids = screen.getByLabelText(label).getAttribute("aria-describedby") ?? "";
  return ids
    .split(" ")
    .map((id) => document.getElementById(id)?.textContent)
    .join(" | ");
}

async function save() {
  await userEvent.click(screen.getByRole("button", { name: "Save" }));
}

const NOTE_TOO_LONG = new ApiError(400, {
  code: "validation",
  errors: [{ field: "note", code: "field.tooLong", params: { max: 2000 } }],
});

beforeEach(async () => {
  vi.mocked(apiRequest).mockReset();
  vi.mocked(apiRequest).mockResolvedValue([]);
  await i18n.changeLanguage("en");
});
afterEach(async () => {
  await i18n.changeLanguage("sl");
});

describe("WF-08 Contact form (UI-13)", () => {
  it("WF-08 the form shows the way back, its sections and the hints of the limits", () => {
    renderForm({});

    expect(screen.getByRole("link", { name: "Contacts" }).getAttribute("href")).toBe("/back");
    expect(screen.getByRole("heading", { level: 2, name: "Fields" })).toBeTruthy();
    expect(screen.getByText("Enter at least a first name, last name or company.")).toBeTruthy();
    expect(describedText("First name")).toBe("At most 100 characters");
    expect(describedText("Phone")).toBe("Digits, spaces and + - ( ) /");
    expect(describedText("Note")).toBe("At most 2,000 characters");
    expect(screen.getByLabelText("Last name").getAttribute("aria-describedby")).toBeNull();
  });

  it("UI-14 a form without a failed save shows no error", () => {
    renderForm({});

    expect(screen.queryByRole("alert")).toBeNull();
  });

  it("R-006-03 a Field problem of the last save stands at its Field", () => {
    renderForm({ error: NOTE_TOO_LONG });

    expect(describedText("Note")).toBe(
      "At most 2,000 characters | Note may have at most 2,000 characters.",
    );
  });

  it("R-006-02 the form's own problem replaces the problems of the last save and is focused", async () => {
    renderForm({ error: NOTE_TOO_LONG });

    await userEvent.clear(screen.getByLabelText("First name"));
    await userEvent.clear(screen.getByLabelText("Last name"));
    await save();

    expect(screen.getByRole("alert").textContent).toBe("Enter a first name, last name or company.");
    expect(screen.queryByText("Note may have at most 2,000 characters.")).toBeNull();
    expect(document.activeElement).toBe(screen.getByLabelText("First name"));
  });

  it("R-006-04 the first Field with a problem is focused", async () => {
    const onSave = renderForm({});

    await userEvent.type(screen.getByLabelText("E-mail"), "janez@");
    await save();

    expect(document.activeElement).toBe(screen.getByLabelText("E-mail"));
    expect(onSave).not.toHaveBeenCalled();
  });

  it("R-008-02 an Attribute problem of the form replaces the problems of the last save", async () => {
    renderForm({ error: NOTE_TOO_LONG });

    await userEvent.click(screen.getByRole("button", { name: "Add an Attribute" }));
    await save();

    expect(screen.getByText("A key must have 1 to 50 characters.")).toBeTruthy();
    expect(screen.queryByText("Note may have at most 2,000 characters.")).toBeNull();
  });

  it("R-009-05 a Tag problem of the form is shown after a failed save", async () => {
    renderForm({ error: new ApiError(403, { code: "auth.forbidden" }) });

    await userEvent.type(screen.getByLabelText("Add a Tag"), "vip, 2025{Enter}");

    expect(screen.getByText("A Tag must have 1 to 50 characters and no comma.")).toBeTruthy();
  });

  it("R-009-01 spaces left in the Tag box are not a Tag and do not stop the save", async () => {
    const onSave = renderForm({});

    await userEvent.type(screen.getByLabelText("Add a Tag"), "   ");
    await save();

    expect(onSave).toHaveBeenCalledWith(expect.objectContaining({ firstName: "Janez" }), [], []);
  });

  it("R-007-03 No changes to save. stands beside Save, not as an alert", () => {
    renderForm({ error: new ApiError(409, { code: "contact.noChanges" }) });

    const save = screen.getByRole("button", { name: "Save" });
    expect(within(save.parentElement as HTMLElement).getByRole("status").textContent).toBe(
      "No changes to save.",
    );
    expect(screen.queryByRole("alert")).toBeNull();
  });

  it("BR-06 a conflict offers Reload, which reads the current Version", async () => {
    const onReload = vi.fn();
    renderForm({
      error: new ApiError(409, { code: "version.conflict", params: { changedBy: "Marko" } }),
      onReload,
    });

    await userEvent.click(screen.getByRole("button", { name: "Reload" }));

    expect(onReload).toHaveBeenCalled();
  });

  it("BR-06 there is no Reload without a way to read again", () => {
    renderForm({
      error: new ApiError(409, { code: "version.conflict", params: { changedBy: "Marko" } }),
    });

    expect(screen.getByRole("alert").textContent).toBe("Contact was changed by Marko, reload");
    expect(screen.queryByRole("button", { name: "Reload" })).toBeNull();
  });

  it("BR-03 another refusal offers no Reload", () => {
    renderForm({ error: new ApiError(403, { code: "auth.forbidden" }), onReload: vi.fn() });

    expect(screen.getByRole("alert").textContent).toBe("You are not allowed to do this.");
    expect(screen.queryByRole("button", { name: "Reload" })).toBeNull();
  });
});
