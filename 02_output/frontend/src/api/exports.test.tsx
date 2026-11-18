import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { type ListQuery, NO_FILTERS } from "../features/contacts/listQuery";
import { apiDownload } from "./apiClient";
import { useExport } from "./exports";

vi.mock("./apiClient", () => ({ apiDownload: vi.fn() }));

const QUERY: ListQuery = { q: "", sort: "name", dir: "asc", page: 1, filters: NO_FILTERS };

let revoked: string[];

beforeEach(() => {
  revoked = [];
  vi.stubGlobal("URL", {
    ...URL,
    createObjectURL: () => "blob:export",
    revokeObjectURL: (url: string) => revoked.push(url),
  });
  vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(() => {});
});
afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe("Export download (US-016)", () => {
  it("R-016-01 releases the file's address after the download has started", async () => {
    vi.mocked(apiDownload).mockResolvedValue({ blob: new Blob(["PK"]), fileName: "k.xlsx" });
    const wrapper = ({ children }: { children: ReactNode }) => (
      <QueryClientProvider client={new QueryClient()}>{children}</QueryClientProvider>
    );
    const { result } = renderHook(() => useExport("g1", QUERY), { wrapper });

    await act(() => result.current.mutateAsync(undefined));

    expect(HTMLAnchorElement.prototype.click).toHaveBeenCalledTimes(1);
    await waitFor(() => expect(revoked).toEqual(["blob:export"]));
  });
});
