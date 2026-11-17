import { afterEach, beforeEach, describe, expect, it } from "vitest";
import i18n from "../../i18n";
import { changeText } from "./changes";

beforeEach(async () => {
  await i18n.changeLanguage("en");
});
afterEach(async () => {
  await i18n.changeLanguage("sl");
});

const t = (key: string) => i18n.t(key);

describe("A changed item of a Version (R-010-01)", () => {
  it("AC-010-01 a Field shows its label with the old and the new value", () => {
    expect(
      changeText(
        { item: "email", oldValue: "janez.novak@example.si", newValue: "janez@novak.si" },
        t,
        "en",
      ),
    ).toBe("E-mail: janez.novak@example.si → janez@novak.si");
  });

  it("R-010-01 an empty Field value is shown as (empty)", () => {
    expect(changeText({ item: "city", oldValue: null, newValue: "Ljubljana" }, t, "en")).toBe(
      "City: (empty) → Ljubljana",
    );
    expect(changeText({ item: "city", oldValue: "Ljubljana", newValue: null }, t, "en")).toBe(
      "City: Ljubljana → (empty)",
    );
  });

  it("R-010-01 a country is shown by its name (UI-18)", () => {
    expect(changeText({ item: "country", oldValue: "SI", newValue: "AT" }, t, "en")).toBe(
      "Country: Slovenia → Austria",
    );
  });

  it("AC-008-01 an added Attribute: key: (empty) → value", () => {
    expect(
      changeText(
        { item: "attribute:kompetenca", oldValue: null, newValue: "spletnoProgramiranje" },
        t,
        "en",
      ),
    ).toBe("kompetenca: (empty) → spletnoProgramiranje");
  });

  it("AC-008-04 a removed Attribute: key: value → (removed)", () => {
    expect(
      changeText(
        { item: "attribute:kompetenca", oldValue: "mobilnoProgramiranje", newValue: null },
        t,
        "en",
      ),
    ).toBe("kompetenca: mobilnoProgramiranje → (removed)");
  });

  it("AC-009-01 an added Tag: tag:<name> added", () => {
    expect(
      changeText(
        { item: "tag:udelezenec2025", oldValue: null, newValue: "udelezenec2025" },
        t,
        "en",
      ),
    ).toBe("tag:udelezenec2025 added");
  });

  it("AC-009-04 a removed Tag: tag:<name> removed", () => {
    expect(
      changeText({ item: "tag:neObvescaj", oldValue: "neObvescaj", newValue: null }, t, "en"),
    ).toBe("tag:neObvescaj removed");
  });
});
