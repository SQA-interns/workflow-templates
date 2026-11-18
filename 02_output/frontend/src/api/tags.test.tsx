import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "./apiClient";
import { useGroupTags, useTagSuggestions } from "./tags";

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

const TAGS = [{ id: "t1", name: "vip" }];

describe("Tag suggestions and Tag filters (US-009, US-015)", () => {
  it("R-009-02 asks for the Tags containing the text under the text's key", async () => {
    const { queryClient, wrapper } = setup();
    vi.mocked(apiRequest).mockResolvedValue(TAGS);

    const { result } = renderHook(() => useTagSuggestions("g1", "v i"), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(apiRequest).toHaveBeenCalledWith("GET", "/groups/g1/tags?q=v%20i");
    expect(queryClient.getQueryData(["groups", "g1", "tags", "v i"])).toEqual(TAGS);
  });

  it("R-009-02 a text of exactly 50 characters is asked", async () => {
    const { wrapper } = setup();
    vi.mocked(apiRequest).mockResolvedValue([]);
    const text = "ž".repeat(50);

    const { result } = renderHook(() => useTagSuggestions("g1", text), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(apiRequest).toHaveBeenCalledTimes(1);
  });

  it("R-009-02 nothing is asked for an empty or a longer than 50 characters text", () => {
    const { wrapper } = setup();

    renderHook(() => useTagSuggestions("g1", ""), { wrapper });
    renderHook(() => useTagSuggestions("g1", "a".repeat(51)), { wrapper });

    expect(apiRequest).not.toHaveBeenCalled();
  });

  it("R-015-01 reads every Tag of the Group under the empty text's key", async () => {
    const { queryClient, wrapper } = setup();
    vi.mocked(apiRequest).mockResolvedValue(TAGS);

    const { result } = renderHook(() => useGroupTags("g1"), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(apiRequest).toHaveBeenCalledWith("GET", "/groups/g1/tags");
    expect(queryClient.getQueryData(["groups", "g1", "tags", ""])).toEqual(TAGS);
  });
});
