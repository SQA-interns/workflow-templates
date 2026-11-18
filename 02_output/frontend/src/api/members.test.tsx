import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "./apiClient";
import { type Member, useChangeRole, useMembers, useRemoveMember } from "./members";

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

const ANA: Member = { userId: "u1", displayName: "Ana", email: "ana@example.si", role: "EDITOR" };
const EVA: Member = { userId: "u2", displayName: "Eva", email: "eva@example.si", role: "VIEWER" };

describe("Members (US-005)", () => {
  it("R-005-01 reads the Members under the Group's members key", async () => {
    const { queryClient, wrapper } = setup();
    vi.mocked(apiRequest).mockResolvedValue([ANA, EVA]);

    const { result } = renderHook(() => useMembers("g1"), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(apiRequest).toHaveBeenCalledWith("GET", "/groups/g1/members");
    expect(queryClient.getQueryData(["groups", "g1", "members"])).toEqual([ANA, EVA]);
  });

  it("R-005-02 the changed Member replaces only that Member in the list read", async () => {
    const { queryClient, wrapper } = setup();
    queryClient.setQueryData(["groups", "g1", "members"], [ANA, EVA]);
    const changed = { ...EVA, role: "EDITOR" };
    vi.mocked(apiRequest).mockResolvedValue(changed);
    const { result } = renderHook(() => useChangeRole("g1"), { wrapper });

    await act(() => result.current.mutateAsync({ userId: "u2", role: "EDITOR" }));

    expect(apiRequest).toHaveBeenCalledWith("PATCH", "/groups/g1/members/u2", { role: "EDITOR" });
    expect(queryClient.getQueryData(["groups", "g1", "members"])).toEqual([ANA, changed]);
  });

  it("R-005-02 a role change succeeds while no Member list has been read", async () => {
    const { wrapper } = setup();
    vi.mocked(apiRequest).mockResolvedValue({ ...EVA, role: "EDITOR" });
    const { result } = renderHook(() => useChangeRole("g1"), { wrapper });

    await act(() => result.current.mutateAsync({ userId: "u2", role: "EDITOR" }));

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
  });

  it("R-005-03 removing a Member reads the Members again", async () => {
    const { wrapper, invalidate } = setup();
    vi.mocked(apiRequest).mockResolvedValue(undefined);
    const { result } = renderHook(() => useRemoveMember("g1"), { wrapper });

    await act(() => result.current.mutateAsync("u2"));

    expect(apiRequest).toHaveBeenCalledWith("DELETE", "/groups/g1/members/u2");
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["groups", "g1", "members"] });
  });
});
