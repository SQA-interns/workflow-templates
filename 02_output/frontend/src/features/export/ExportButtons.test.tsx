import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { apiDownload } from "../../api/apiClient";
import { ApiError } from "../../api/problem";
import i18n from "../../i18n";
import { type ListQuery, NO_FILTERS } from "../contacts/listQuery";
import { ExportButtons } from "./ExportButtons";

vi.mock("../../api/apiClient", () => ({ apiDownload: vi.fn() }));

const QUERY: ListQuery = {
  q: "novak",
  sort: "company",
  dir: "desc",
  page: 2,
  filters: { ...NO_FILTERS, hasTagIds: ["t1"], country: "SI" },
};
const FILE = { blob: new Blob(["PK"]), fileName: "kontakti-Konferenca 2025-2026-03-05.xlsx" };

function renderButtons(total: number, checked: string[] = []) {
  render(
    <QueryClientProvider client={new QueryClient()}>
      <ExportButtons groupId="g1" query={QUERY} total={total} checked={checked} />
    </QueryClientProvider>,
  );
}

let saved: string[];

beforeEach(async () => {
  vi.mocked(apiDownload).mockReset();
  saved = [];
  vi.stubGlobal("URL", { ...URL, createObjectURL: () => "blob:x", revokeObjectURL: () => {} });
  vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(function (
    this: HTMLAnchorElement,
  ) {
    saved.push(this.download);
  });
  await i18n.changeLanguage("en");
});
afterEach(async () => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
  await i18n.changeLanguage("sl");
});

const exportButton = () => screen.getByRole("button", { name: "Export" });

describe("Export buttons of WF-06 (US-016)", () => {
  it("AC-016-01 Export sends the list query and saves the file with its name", async () => {
    vi.mocked(apiDownload).mockResolvedValue(FILE);
    renderButtons(2);

    await userEvent.click(exportButton());

    await waitFor(() => expect(saved).toEqual([FILE.fileName]));
    expect(apiDownload).toHaveBeenCalledWith("/groups/g1/exports", {
      q: "novak",
      sort: "company",
      dir: "desc",
      ...NO_FILTERS,
      hasTagIds: ["t1"],
      country: "SI",
    });
  });

  it("AC-016-02 Export checked sends only the Checked contacts", async () => {
    vi.mocked(apiDownload).mockResolvedValue(FILE);
    renderButtons(3, ["c3"]);

    expect(screen.getByText("1 checked")).toBeTruthy();
    await userEvent.click(screen.getByRole("button", { name: "Export checked (1)" }));

    await waitFor(() => expect(saved).toEqual([FILE.fileName]));
    expect(vi.mocked(apiDownload).mock.calls[0]?.[1]).toMatchObject({ contactIds: ["c3"] });
  });

  it("R-016-02 Export checked is disabled while no Contact is checked", () => {
    renderButtons(3);

    expect(screen.getByRole("button", { name: "Export checked" }).hasAttribute("disabled")).toBe(
      true,
    );
    expect(screen.queryByText(/^\d+ checked$/)).toBeNull();
  });

  it("R-016-07 EX-016-02 an empty scope disables Export: Nothing to export.", () => {
    renderButtons(0);

    expect(exportButton().hasAttribute("disabled")).toBe(true);
    expect(screen.getByText("Nothing to export.")).toBeTruthy();
  });

  it("R-016-05 AC-016-03 more than 10,000 Contacts: Export disabled with the message", () => {
    renderButtons(10_001);

    expect(exportButton().hasAttribute("disabled")).toBe(true);
    expect(screen.getByText("Narrow the list to at most 10,000 Contacts to export.")).toBeTruthy();
  });

  it("EX-016-01 exactly 10,000 Contacts can be exported", () => {
    renderButtons(10_000);

    expect(exportButton().hasAttribute("disabled")).toBe(false);
    expect(screen.queryByText("Nothing to export.")).toBeNull();
  });

  it("R-016-05 a refused Export shows the message of the problem and saves nothing", async () => {
    vi.mocked(apiDownload).mockRejectedValue(new ApiError(409, { code: "export.tooMany" }));
    renderButtons(2);

    await userEvent.click(exportButton());

    expect((await screen.findByRole("alert")).textContent).toBe(
      "Narrow the list to at most 10,000 Contacts to export.",
    );
    expect(saved).toEqual([]);
  });

  it("UI-12 both buttons are disabled while the file is made", async () => {
    vi.mocked(apiDownload).mockReturnValue(new Promise(() => {}));
    renderButtons(2, ["c1"]);

    await userEvent.click(exportButton());

    await waitFor(() => expect(exportButton().hasAttribute("disabled")).toBe(true));
    expect(
      screen.getByRole("button", { name: "Export checked (1)" }).hasAttribute("disabled"),
    ).toBe(true);
  });
});
