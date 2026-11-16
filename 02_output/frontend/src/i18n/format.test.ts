import { describe, expect, it } from "vitest";
import { formatDate, formatDateTime, formatNumber } from "./format";

describe("formats of the project-wide facts (R-020-03)", () => {
  it("AC-020-03 a time reads dd.mm.yyyy HH:mm in Europe/Ljubljana in both languages", () => {
    expect(formatDateTime("2026-03-05T09:00:00Z")).toBe("05.03.2026 10:00");
  });

  it("R-020-03 summer time and 24-hour clock in Europe/Ljubljana", () => {
    expect(formatDateTime("2026-07-01T12:07:00Z")).toBe("01.07.2026 14:07");
  });

  it("R-020-03 a date alone reads dd.mm.yyyy", () => {
    expect(formatDate("2026-03-05T23:30:00Z")).toBe("06.03.2026");
  });

  it("EX-020-02 10000 reads 10.000 in Slovenian and 10,000 in English", () => {
    expect(formatNumber(10000, "sl")).toBe("10.000");
    expect(formatNumber(10000, "en")).toBe("10,000");
  });
});
