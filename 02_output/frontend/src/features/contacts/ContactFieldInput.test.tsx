import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import i18n from "../../i18n";
import { ContactFieldInput, type FieldProps } from "./ContactFieldInput";
import type { FieldName } from "./contactFields";

function renderField(name: FieldName, props: Partial<FieldProps> = {}) {
  const onChange = vi.fn();
  render(
    <ContactFieldInput
      name={name}
      language="en"
      props={{ id: "f", label: "Label", value: "", onChange, ...props }}
    />,
  );
  return onChange;
}

beforeEach(async () => {
  await i18n.changeLanguage("en");
});
afterEach(async () => {
  await i18n.changeLanguage("sl");
});

describe("one Field of WF-08 (UI-13, UI-19)", () => {
  it("UI-13 the label names the box; hint and error describe it", () => {
    renderField("firstName", {
      label: "First name",
      hint: "At most 100 characters",
      error: "Too long",
    });

    const box = screen.getByLabelText("First name");
    expect(box.getAttribute("aria-describedby")).toBe("f-hint f-error");
    expect(box.getAttribute("aria-invalid")).toBe("true");
    expect(box.getAttribute("aria-errormessage")).toBe("f-error");
    expect(document.getElementById("f-error")?.textContent).toBe("Too long");
  });

  it("UI-13 without an error the box is not invalid", () => {
    renderField("lastName", { label: "Last name" });

    const box = screen.getByLabelText("Last name");
    expect(box.getAttribute("aria-invalid")).toBeNull();
    expect(box.getAttribute("aria-describedby")).toBeNull();
  });

  it("R-006-01 typing reports the value", async () => {
    const onChange = renderField("city", { label: "City" });

    await userEvent.type(screen.getByLabelText("City"), "L");

    expect(onChange).toHaveBeenCalledWith("L");
  });

  it("R-006-04 the e-mail box is for e-mail addresses, the phone box for numbers", () => {
    renderField("email", { id: "e", label: "E-mail" });
    renderField("phone", { id: "p", label: "Phone" });

    expect(screen.getByLabelText("E-mail").getAttribute("type")).toBe("email");
    expect(screen.getByLabelText("Phone").getAttribute("type")).toBe("tel");
  });

  it("R-006-03 the note is a text area", () => {
    renderField("note", { label: "Note" });

    expect(screen.getByLabelText("Note").tagName).toBe("TEXTAREA");
  });

  it("R-006-05 the country is chosen by name and reported as its ISO code", async () => {
    const onChange = renderField("country", { label: "Country" });

    const country = screen.getByLabelText("Country");
    expect(country.tagName).toBe("SELECT");
    expect(screen.getByRole("option", { name: "Select …" })).toBeTruthy();
    await userEvent.selectOptions(country, "Slovenia");

    expect(onChange).toHaveBeenCalledWith("SI");
  });

  it("UI-13 the hint is shown under the box with the id that describes it", () => {
    renderField("firstName", { label: "First name", hint: "At most 100 characters" });

    expect(document.getElementById("f-hint")?.textContent).toBe("At most 100 characters");
    expect(screen.getByLabelText("First name").getAttribute("aria-describedby")).toBe("f-hint");
  });

  it("UI-13 without a hint, notice or error nothing is shown under the box", () => {
    renderField("firstName", { label: "First name" });

    expect(document.getElementById("f-hint")).toBeNull();
    expect(document.getElementById("f-error")).toBeNull();
    expect(screen.queryByRole("status")).toBeNull();
  });

  it("R-006-06 a notice is announced politely and describes the box", () => {
    renderField("email", { label: "E-mail", notice: "A Contact with this e-mail already exists." });

    const notice = document.getElementById("f-notice");
    expect(notice?.textContent).toBe("A Contact with this e-mail already exists.");
    expect(screen.getByRole("status").contains(notice)).toBe(true);
    expect(screen.getByLabelText("E-mail").getAttribute("aria-describedby")).toBe("f-notice");
  });

  it("R-006-06 an empty notice keeps the polite region but shows nothing", () => {
    renderField("email", { label: "E-mail", notice: "" });

    expect(screen.getByRole("status").textContent).toBe("");
    expect(document.getElementById("f-notice")).toBeNull();
    expect(screen.getByLabelText("E-mail").getAttribute("aria-describedby")).toBeNull();
  });

  it("R-006-01 the other Fields are plain text boxes", () => {
    renderField("city", { label: "City" });

    expect(screen.getByLabelText("City").getAttribute("type")).toBe("text");
  });

  it("R-006-05 the country names follow a change of language", () => {
    const props = { id: "f", label: "Country", value: "", onChange: vi.fn() };
    const { rerender } = render(<ContactFieldInput name="country" language="en" props={props} />);
    expect(screen.getByRole("option", { name: "Slovenia" })).toBeTruthy();

    rerender(<ContactFieldInput name="country" language="sl" props={props} />);

    expect(screen.getByRole("option", { name: "Slovenija" })).toBeTruthy();
  });
});
