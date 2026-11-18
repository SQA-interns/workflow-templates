import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiPath, type ListQuery, NO_FILTERS } from "../features/contacts/listQuery";
import { apiRequest } from "./apiClient";
import {
  type Contact,
  useContact,
  useContactList,
  useEmailMatches,
  useUpdateContact,
} from "./contacts";

vi.mock("./apiClient", () => ({ apiRequest: vi.fn() }));

function setup() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  return { queryClient, wrapper };
}

const CONTACT = { id: "c1", versionNumber: 3 } as Contact;
const QUERY: ListQuery = { q: "novak", sort: "name", dir: "asc", page: 2, filters: NO_FILTERS };

beforeEach(() => {
  vi.mocked(apiRequest).mockReset();
});

describe("Contact reads and saves (US-006, US-007, US-014)", () => {
  it("R-007-01 a saved Contact replaces the one read and the lists are read again", async () => {
    const { queryClient, wrapper } = setup();
    const saved = { ...CONTACT, versionNumber: 4 };
    vi.mocked(apiRequest).mockResolvedValue(saved);
    const invalidate = vi.spyOn(queryClient, "invalidateQueries");
    const { result } = renderHook(() => useUpdateContact("g1", "c1"), { wrapper });

    await act(() =>
      result.current.mutateAsync({
        read: CONTACT,
        fields: {} as Contact["fields"],
        attributes: [],
        tags: ["vip"],
      }),
    );

    expect(apiRequest).toHaveBeenCalledWith("PUT", "/groups/g1/contacts/c1", {
      fields: {},
      attributes: [],
      tags: ["vip"],
      versionNumber: 3,
    });
    expect(queryClient.getQueryData(["groups", "g1", "contacts", "c1"])).toEqual(saved);
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["groups", "g1", "contacts", "list"] });
  });

  it("R-006-06 asks for the Contacts with the e-mail under the e-mail's key", async () => {
    const { queryClient, wrapper } = setup();
    const matches = [{ id: "c1", name: "Ana Kovač" }];
    vi.mocked(apiRequest).mockResolvedValue(matches);

    const { result } = renderHook(() => useEmailMatches("g1", "ana+1@example.si"), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(apiRequest).toHaveBeenCalledWith(
      "GET",
      "/groups/g1/contacts/email-matches?email=ana%2B1%40example.si",
    );
    expect(
      queryClient.getQueryData(["groups", "g1", "contacts", "email-matches", "ana+1@example.si"]),
    ).toEqual(matches);
  });

  it("R-006-06 asks nothing while there is no e-mail", () => {
    const { wrapper } = setup();

    renderHook(() => useEmailMatches("g1", null), { wrapper });

    expect(apiRequest).not.toHaveBeenCalled();
  });

  it("R-014-01 reads a Page of the list for the query under the list key", async () => {
    const { queryClient, wrapper } = setup();
    const page = { items: [], total: 0, page: 2, pageSize: 50 };
    vi.mocked(apiRequest).mockResolvedValue(page);

    const { result } = renderHook(() => useContactList("g1", QUERY), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(apiRequest).toHaveBeenCalledWith("GET", apiPath("g1", QUERY));
    expect(queryClient.getQueryData(["groups", "g1", "contacts", "list", QUERY])).toEqual(page);
  });

  it("R-014-06 reads one Contact under its key", async () => {
    const { queryClient, wrapper } = setup();
    vi.mocked(apiRequest).mockResolvedValue(CONTACT);

    const { result } = renderHook(() => useContact("g1", "c1"), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(apiRequest).toHaveBeenCalledWith("GET", "/groups/g1/contacts/c1");
    expect(queryClient.getQueryData(["groups", "g1", "contacts", "c1"])).toEqual(CONTACT);
  });
});
