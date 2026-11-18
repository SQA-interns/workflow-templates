import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "./apiClient";
import { useDuplicates, useMerge, useUndoMerge } from "./duplicates";
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

beforeEach(() => {
  vi.mocked(apiRequest).mockReset();
});

describe("Possible duplicates and Merges (US-017, US-018, US-019)", () => {
  it("R-017-01 reads the Possible duplicates under their key", async () => {
    const { queryClient, wrapper } = setup();
    const pairs = [
      { first: { id: "c1", name: "A" }, second: { id: "c2", name: "B" }, reasons: ["sameEmail"] },
    ];
    vi.mocked(apiRequest).mockResolvedValue(pairs);

    const { result } = renderHook(() => useDuplicates("g1"), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(apiRequest).toHaveBeenCalledWith("GET", "/groups/g1/duplicates");
    expect(queryClient.getQueryData(["groups", "g1", "duplicates"])).toEqual(pairs);
  });

  it("R-018-02 a Merge reads the lists, both Contacts and the Possible duplicates again", async () => {
    const { wrapper, invalidate } = setup();
    vi.mocked(apiRequest).mockResolvedValue({});
    const { result } = renderHook(() => useMerge("g1"), { wrapper });
    const request = {
      primaryId: "c1",
      otherId: "c2",
      primaryVersionNumber: 1,
      otherVersionNumber: 2,
      fieldChoices: {},
      attributeChoices: {},
    };

    await act(() => result.current.mutateAsync(request));

    expect(apiRequest).toHaveBeenCalledWith("POST", "/groups/g1/merges", request);
    expect(invalidate.mock.calls).toEqual([
      [{ queryKey: ["groups", "g1", "contacts", "list"] }],
      [{ queryKey: ["groups", "g1", "contacts", "c1"] }],
      [{ queryKey: ["groups", "g1", "contacts", "c2"] }],
      [{ queryKey: ["groups", "g1", "duplicates"] }],
    ]);
  });

  it("R-019-01 undoing a Merge reads the lists, both Contacts and the Possible duplicates again", async () => {
    const { wrapper, invalidate } = setup();
    vi.mocked(apiRequest).mockResolvedValue({});
    const { result } = renderHook(() => useUndoMerge("g1", "c1"), { wrapper });

    await act(() => result.current.mutateAsync({ mergeId: "m1", otherId: "c2", versionNumber: 5 }));

    expect(apiRequest).toHaveBeenCalledWith("POST", "/groups/g1/merges/m1/undo", {
      versionNumber: 5,
    });
    expect(invalidate.mock.calls).toEqual([
      [{ queryKey: ["groups", "g1", "contacts", "list"] }],
      [{ queryKey: ["groups", "g1", "contacts", "c1"] }],
      [{ queryKey: ["groups", "g1", "contacts", "c2"] }],
      [{ queryKey: ["groups", "g1", "duplicates"] }],
    ]);
  });

  it("R-019-03 after a refused undo only the Primary contact is read again", async () => {
    const { wrapper, invalidate } = setup();
    vi.mocked(apiRequest).mockRejectedValue(new ApiError(409, { code: "version.conflict" }));
    const { result } = renderHook(() => useUndoMerge("g1", "c1"), { wrapper });

    await act(() =>
      result.current
        .mutateAsync({ mergeId: "m1", otherId: "c2", versionNumber: 5 })
        .catch(() => undefined),
    );

    expect(invalidate.mock.calls).toEqual([
      [{ queryKey: ["groups", "g1", "contacts", "c1"], exact: true }],
    ]);
  });
});
