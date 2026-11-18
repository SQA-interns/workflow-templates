import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "./apiClient";
import type { Contact } from "./contacts";
import { ApiError } from "./problem";
import {
  type RecycleBinEntry,
  useMergedIntoNames,
  useMoveToRecycleBin,
  usePermanentlyDelete,
  useRestoreFromRecycleBin,
} from "./recycleBin";

vi.mock("./apiClient", () => ({ apiRequest: vi.fn() }));

function setup() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  return { queryClient, wrapper, invalidate: vi.spyOn(queryClient, "invalidateQueries") };
}

beforeEach(() => {
  vi.mocked(apiRequest).mockReset();
});

const ENTRY: RecycleBinEntry = {
  contact: { id: "c1", name: "Ana Kovač" },
  versionNumber: 4,
  deletedBy: "Eva",
  deletedAt: "x",
  removalAt: "y",
};
const READ = { id: "c1", versionNumber: 4, mergedFrom: [] } as unknown as Contact;
const CONFLICT = new ApiError(409, { code: "version.conflict" });

const RESTORE_INVALIDATIONS = [
  [{ queryKey: ["groups", "g1", "recycle-bin"] }],
  [{ queryKey: ["groups", "g1", "contacts", "c1"], exact: true }],
  [{ queryKey: ["groups", "g1", "contacts", "list"] }],
];

describe("Recycle bin (US-012, US-013)", () => {
  it("D-21 reads the merged names under the Contact's merged-into key", async () => {
    const { queryClient, wrapper } = setup();
    vi.mocked(apiRequest).mockResolvedValue(READ);

    const { result } = renderHook(() => useMergedIntoNames("g1", "c1", true), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(queryClient.getQueryData(["groups", "g1", "contacts", "c1", "merged-into"])).toEqual([]);
  });

  it("R-012-03 a restore reads the recycle bin, the Contact and the lists again", async () => {
    const { wrapper, invalidate } = setup();
    vi.mocked(apiRequest).mockResolvedValue(READ);
    const { result } = renderHook(() => useRestoreFromRecycleBin("g1"), { wrapper });

    await act(() => result.current.mutateAsync(ENTRY));

    expect(apiRequest).toHaveBeenCalledWith("POST", "/groups/g1/recycle-bin/c1/restore", {
      versionNumber: 4,
    });
    expect(invalidate.mock.calls).toEqual(RESTORE_INVALIDATIONS);
  });

  it("R-012-04 a refused restore reads the same again", async () => {
    const { wrapper, invalidate } = setup();
    vi.mocked(apiRequest).mockRejectedValue(CONFLICT);
    const { result } = renderHook(() => useRestoreFromRecycleBin("g1"), { wrapper });

    await act(() => result.current.mutateAsync(ENTRY).catch(() => undefined));

    expect(invalidate.mock.calls).toEqual(RESTORE_INVALIDATIONS);
  });

  it("R-012-01 the moved Contact replaces the one read; lists and recycle bin are read again", async () => {
    const { queryClient, wrapper, invalidate } = setup();
    const moved = { ...READ, versionNumber: 5 };
    vi.mocked(apiRequest).mockResolvedValue(moved);
    const { result } = renderHook(() => useMoveToRecycleBin("g1", "c1"), { wrapper });

    await act(() => result.current.mutateAsync(READ));

    expect(apiRequest).toHaveBeenCalledWith("POST", "/groups/g1/contacts/c1/move-to-recycle-bin", {
      versionNumber: 4,
    });
    expect(queryClient.getQueryData(["groups", "g1", "contacts", "c1"])).toEqual(moved);
    expect(invalidate.mock.calls).toEqual([
      [{ queryKey: ["groups", "g1", "contacts", "list"] }],
      [{ queryKey: ["groups", "g1", "recycle-bin"] }],
    ]);
  });

  it("BR-06 after a refused move only the Contact itself is read again", async () => {
    const { wrapper, invalidate } = setup();
    vi.mocked(apiRequest).mockRejectedValue(CONFLICT);
    const { result } = renderHook(() => useMoveToRecycleBin("g1", "c1"), { wrapper });

    await act(() => result.current.mutateAsync(READ).catch(() => undefined));

    expect(invalidate.mock.calls).toEqual([
      [{ queryKey: ["groups", "g1", "contacts", "c1"], exact: true }],
    ]);
  });

  it("R-013-01 a permanent delete forgets the Contact and reads bin, lists and Tags again", async () => {
    const { queryClient, wrapper, invalidate } = setup();
    queryClient.setQueryData(["groups", "g1", "contacts", "c1"], READ);
    queryClient.setQueryData(["groups", "g1", "contacts", "c1", "versions"], []);
    vi.mocked(apiRequest).mockResolvedValue(undefined);
    const { result } = renderHook(() => usePermanentlyDelete("g1"), { wrapper });

    await act(() => result.current.mutateAsync(ENTRY));

    expect(apiRequest).toHaveBeenCalledWith("DELETE", "/groups/g1/recycle-bin/c1");
    expect(queryClient.getQueryState(["groups", "g1", "contacts", "c1"])).toBeUndefined();
    expect(
      queryClient.getQueryState(["groups", "g1", "contacts", "c1", "versions"]),
    ).toBeUndefined();
    expect(invalidate.mock.calls).toEqual([
      [{ queryKey: ["groups", "g1", "recycle-bin"] }],
      [{ queryKey: ["groups", "g1", "contacts", "list"] }],
      [{ queryKey: ["groups", "g1", "tags"] }],
    ]);
  });
});
