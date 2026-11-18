import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "./apiClient";
import {
  useAcceptInvitation,
  useCreateInvitation,
  useInvitation,
  useOpenInvitations,
  useRevokeInvitation,
} from "./invitations";

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

describe("Invitations (US-004)", () => {
  it("R-004-08 reads the open Invitations under the Group's invitations key", async () => {
    const { queryClient, wrapper } = setup();
    vi.mocked(apiRequest).mockResolvedValue([]);

    const { result } = renderHook(() => useOpenInvitations("g1"), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(apiRequest).toHaveBeenCalledWith("GET", "/groups/g1/invitations");
    expect(queryClient.getQueryData(["groups", "g1", "invitations"])).toEqual([]);
  });

  it("R-004-03 creating an Invitation reads the open Invitations again", async () => {
    const { wrapper, invalidate } = setup();
    vi.mocked(apiRequest).mockResolvedValue({ invitation: {}, link: "x" });
    const { result } = renderHook(() => useCreateInvitation("g1"), { wrapper });
    const request = { email: "eva@example.si", role: "EDITOR" as const };

    await act(() => result.current.mutateAsync(request));

    expect(apiRequest).toHaveBeenCalledWith("POST", "/groups/g1/invitations", request);
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["groups", "g1", "invitations"] });
  });

  it("R-004-09 revoking an Invitation reads the open Invitations again", async () => {
    const { wrapper, invalidate } = setup();
    vi.mocked(apiRequest).mockResolvedValue(undefined);
    const { result } = renderHook(() => useRevokeInvitation("g1"), { wrapper });

    await act(() => result.current.mutateAsync("i1"));

    expect(apiRequest).toHaveBeenCalledWith("DELETE", "/groups/g1/invitations/i1");
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["groups", "g1", "invitations"] });
  });

  it("R-004-04 reads the Invitation of a link under its token", async () => {
    const { queryClient, wrapper } = setup();
    const view = { groupName: "Klub", role: "VIEWER", expiresAt: "x" };
    vi.mocked(apiRequest).mockResolvedValue(view);

    const { result } = renderHook(() => useInvitation("tok"), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(apiRequest).toHaveBeenCalledWith("GET", "/invitations/tok");
    expect(queryClient.getQueryData(["invitations", "tok"])).toEqual(view);
  });

  it("R-004-04 accepting an Invitation reads the Group list again", async () => {
    const { wrapper, invalidate } = setup();
    vi.mocked(apiRequest).mockResolvedValue({ id: "g1" });
    const { result } = renderHook(() => useAcceptInvitation("tok"), { wrapper });

    await act(() => result.current.mutateAsync());

    expect(apiRequest).toHaveBeenCalledWith("POST", "/invitations/tok/accept");
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["groups"] });
  });
});
