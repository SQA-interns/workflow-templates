import { describe, expect, it } from "vitest";
import {
  checkFields,
  countryOptions,
  emptyFields,
  FIELD_NAMES,
  type FieldValues,
} from "./contactFields";

function fields(values: Partial<FieldValues>): FieldValues {
  return { ...emptyFields(), ...values };
}

describe("Contact Fields (specification §3, R-006-02 … R-006-05, R-006-07)", () => {
  it("D-23 the Fields are those of the Contact form, in its order", () => {
    expect(FIELD_NAMES).toEqual([
      "firstName",
      "lastName",
      "email",
      "phone",
      "company",
      "jobTitle",
      "street",
      "postalCode",
      "city",
      "country",
      "note",
    ]);
  });

  it("R-006-02 only a company is enough", () => {
    expect(checkFields(fields({ company: "Podjetje d.o.o." })).problems).toEqual([]);
  });

  it("R-006-02 EX-006-05 no name part after trimming is rejected", () => {
    const { problems } = checkFields(
      fields({ firstName: "   ", company: "  ", email: "info@example.si" }),
    );
    expect(problems).toEqual([{ field: "name", code: "contact.nameRequired" }]);
  });

  it("R-006-07 every Field is trimmed and blank ones become empty", () => {
    const { trimmed } = checkFields(fields({ firstName: "  Janez ", lastName: "   " }));
    expect(trimmed.firstName).toBe("Janez");
    expect(trimmed.lastName).toBeNull();
  });

  it("R-006-03 EX-006-01 EX-006-02 a first name of 100 characters passes, of 101 does not", () => {
    expect(checkFields(fields({ firstName: "J".repeat(100) })).problems).toEqual([]);
    expect(checkFields(fields({ firstName: "J".repeat(101) })).problems).toEqual([
      { field: "firstName", code: "field.tooLong", params: { max: 100 } },
    ]);
  });

  it("R-006-03 EX-006-03 the note may have 2000 characters, counted as characters", () => {
    expect(checkFields(fields({ firstName: "Janez", note: "😀".repeat(2000) })).problems).toEqual(
      [],
    );
    expect(checkFields(fields({ firstName: "Janez", note: "n".repeat(2001) })).problems).toEqual([
      { field: "note", code: "field.tooLong", params: { max: 2000 } },
    ]);
  });

  it.each(["janez@", "janez", "@example.si", "janez@example", "ja nez@example.si", "a@b@c.si"])(
    "R-006-04 AC-006-04 %s is not a valid e-mail address",
    (email) => {
      expect(checkFields(fields({ firstName: "Janez", email })).problems).toEqual([
        { field: "email", code: "field.emailInvalid" },
      ]);
    },
  );

  it("R-006-04 a valid e-mail address passes", () => {
    expect(
      checkFields(fields({ firstName: "Janez", email: "Janez.Novak@example.si" })).problems,
    ).toEqual([]);
  });

  it("D-23 a phone with other characters than digits, spaces and + - ( ) / is rejected", () => {
    expect(
      checkFields(fields({ firstName: "Janez", phone: "+386 (1) 123-45/67" })).problems,
    ).toEqual([]);
    expect(checkFields(fields({ firstName: "Janez", phone: "041.123.456" })).problems).toEqual([
      { field: "phone", code: "field.phoneInvalid" },
    ]);
  });

  it("R-006-05 the country list offers the ISO 3166-1 alpha-2 codes by name in the language", () => {
    const en = countryOptions("en");
    expect(en).toHaveLength(249);
    expect(en).toContainEqual({ code: "SI", name: "Slovenia" });
    const sl = countryOptions("sl");
    expect(sl).toContainEqual({ code: "SI", name: "Slovenija" });
    expect(sl.findIndex((c) => c.name === "Češka")).toBeLessThan(
      sl.findIndex((c) => c.name === "Danska"),
    );
  });
});
