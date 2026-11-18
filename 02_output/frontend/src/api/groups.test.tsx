import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "./apiClient";
import {
  type Group,
  useCanChangeContacts,
  useCreateGroup,
  useDeletedGroups,
  useGroups,
  useIsMember,
  useIsOwner,
  useMoveGroupToRecycleBin,
  useRenameGroup,
  useRestoreGroup,
} from "./groups";

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

const GROUPS: Group[] = [
  { id: "g1", name: "Družina", role: "OWNER" },
  { id: "g2", name: "Konferenca", role: "VIEWER" },
  { id: "g3", name: "Klub", role: "EDITOR" },
];

/** Renders a hook once the Group list has been read into the cache. */
async function withGroups<T>(hook: () => T) {
  const { wrapper } = setup();
  vi.mocked(apiRequest).mockResolvedValue(GROUPS);
  const { result } = renderHook(() => ({ groups: useGroups(), value: hook() }), { wrapper });
  await waitFor(() => expect(result.current.groups.isSuccess).toBe(true));
  return result.current.value;
}

beforeEach(() => {
  vi.mocked(apiRequest).mockReset();
});

describe("Groups of the User and the User's Role (US-002, BR-01, BR-03, BR-04)", () => {
  it("R-002-03 reads the Group list under the key groups", async () => {
    const { queryClient, wrapper } = setup();
    vi.mocked(apiRequest).mockResolvedValue(GROUPS);

    const { result } = renderHook(() => useGroups(), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(apiRequest).toHaveBeenCalledWith("GET", "/groups");
    expect(queryClient.getQueryData(["groups"])).toEqual(GROUPS);
  });

  it("BR-03 the Role of the Group asked decides whether Contacts can change", async () => {
    expect(await withGroups(() => useCanChangeContacts("g3"))).toBe(true);
    expect(await withGroups(() => useCanChangeContacts("g1"))).toBe(true);
    expect(await withGroups(() => useCanChangeContacts("g2"))).toBe(false);
  });

  it("BR-01 the User is a Member only of a Group in the list", async () => {
    expect(await withGroups(() => useIsMember("g2"))).toBe(true);
    expect(await withGroups(() => useIsMember("other"))).toBe(false);
  });

  it("BR-01 the User is no Member while the Group list is not read yet", () => {
    const { wrapper } = setup();
    vi.mocked(apiRequest).mockReturnValue(new Promise(() => {}));

    const { result } = renderHook(() => useIsMember("g1"), { wrapper });

    expect(result.current).toBe(false);
  });

  it("BR-04 the User is the Owner only of a Group with the Role Owner", async () => {
    expect(await withGroups(() => useIsOwner("g1"))).toBe(true);
    expect(await withGroups(() => useIsOwner("g2"))).toBe(false);
  });
});

describe("Group changes read the Group list again (US-002, US-003)", () => {
  it("R-002-01 creating a Group posts its name and reads the Group list again", async () => {
    const { wrapper, invalidate } = setup();
    vi.mocked(apiRequest).mockResolvedValue(GROUPS[0]);
    const { result } = renderHook(() => useCreateGroup(), { wrapper });

    await act(() => result.current.mutateAsync("Družina"));

    expect(apiRequest).toHaveBeenCalledWith("POST", "/groups", { name: "Družina" });
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["groups"] });
  });

  it("R-003-01 renaming a Group reads the Group list again", async () => {
    const { wrapper, invalidate } = setup();
    vi.mocked(apiRequest).mockResolvedValue(GROUPS[0]);
    const { result } = renderHook(() => useRenameGroup("g1"), { wrapper });

    await act(() => result.current.mutateAsync("Družina Novak"));

    expect(apiRequest).toHaveBeenCalledWith("PATCH", "/groups/g1", { name: "Družina Novak" });
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["groups"] });
  });

  it("R-003-02 moving a Group to the recycle bin reads the Group list again", async () => {
    const { wrapper, invalidate } = setup();
    vi.mocked(apiRequest).mockResolvedValue(undefined);
    const { result } = renderHook(() => useMoveGroupToRecycleBin("g1"), { wrapper });

    await act(() => result.current.mutateAsync());

    expect(apiRequest).toHaveBeenCalledWith("POST", "/groups/g1/move-to-recycle-bin");
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["groups"] });
  });

  it("R-003-04 reads the deleted Groups under their own key", async () => {
    const { queryClient, wrapper } = setup();
    const deleted = [{ id: "g9", name: "Stara", deletedAt: "x", removalAt: "y" }];
    vi.mocked(apiRequest).mockResolvedValue(deleted);

    const { result } = renderHook(() => useDeletedGroups(), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(apiRequest).toHaveBeenCalledWith("GET", "/deleted-groups");
    expect(queryClient.getQueryData(["deleted-groups"])).toEqual(deleted);
  });

  it("R-003-05 restoring a Group reads both Group lists again", async () => {
    const { wrapper, invalidate } = setup();
    vi.mocked(apiRequest).mockResolvedValue(GROUPS[0]);
    const { result } = renderHook(() => useRestoreGroup(), { wrapper });

    await act(() => result.current.mutateAsync("g9"));

    expect(apiRequest).toHaveBeenCalledWith("POST", "/deleted-groups/g9/restore");
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["groups"] });
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["deleted-groups"] });
  });
});
