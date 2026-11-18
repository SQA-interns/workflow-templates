import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "./apiClient";
import type { Contact } from "./contacts";
import { useHistory, useRestoreVersion, useVersion } from "./history";
import { ApiError } from "./problem";

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

const READ = { id: "c1", versionNumber: 7 } as Contact;

beforeEach(() => {
  vi.mocked(apiRequest).mockReset();
});

describe("History and restoring a Version (US-010, US-011)", () => {
  it("R-010-01 reads the History under the Contact's versions key", async () => {
    const { queryClient, wrapper } = setup();
    vi.mocked(apiRequest).mockResolvedValue([]);

    const { result } = renderHook(() => useHistory("g1", "c1"), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(apiRequest).toHaveBeenCalledWith("GET", "/groups/g1/contacts/c1/versions");
    expect(queryClient.getQueryData(["groups", "g1", "contacts", "c1", "versions"])).toEqual([]);
  });

  it("R-010-02 reads one Version under its number", async () => {
    const { queryClient, wrapper } = setup();
    const version = { number: 2 };
    vi.mocked(apiRequest).mockResolvedValue(version);

    const { result } = renderHook(() => useVersion("g1", "c1", "2"), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(apiRequest).toHaveBeenCalledWith("GET", "/groups/g1/contacts/c1/versions/2");
    expect(queryClient.getQueryData(["groups", "g1", "contacts", "c1", "versions", "2"])).toEqual(
      version,
    );
  });

  it("R-011-01 the restored Contact replaces the one read; History and lists are read again", async () => {
    const { queryClient, wrapper, invalidate } = setup();
    const restored = { ...READ, versionNumber: 8 };
    vi.mocked(apiRequest).mockResolvedValue(restored);
    const { result } = renderHook(() => useRestoreVersion("g1", "c1", "2"), { wrapper });

    await act(() => result.current.mutateAsync(READ));

    expect(apiRequest).toHaveBeenCalledWith("POST", "/groups/g1/contacts/c1/versions/2/restore", {
      versionNumber: 7,
    });
    expect(queryClient.getQueryData(["groups", "g1", "contacts", "c1"])).toEqual(restored);
    expect(invalidate.mock.calls).toEqual([
      [{ queryKey: ["groups", "g1", "contacts", "c1", "versions"] }],
      [{ queryKey: ["groups", "g1", "contacts", "list"] }],
    ]);
  });

  it("BR-06 after a refused restore only the Contact itself is read again", async () => {
    const { wrapper, invalidate } = setup();
    vi.mocked(apiRequest).mockRejectedValue(new ApiError(409, { code: "version.conflict" }));
    const { result } = renderHook(() => useRestoreVersion("g1", "c1", "2"), { wrapper });

    await act(() => result.current.mutateAsync(READ).catch(() => undefined));

    expect(invalidate.mock.calls).toEqual([
      [{ queryKey: ["groups", "g1", "contacts", "c1"], exact: true }],
    ]);
  });
});
