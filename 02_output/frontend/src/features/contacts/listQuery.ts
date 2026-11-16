// What the User asked of the Contact list, kept in the URL so the list, its count and later the
// filters and the Export use the same query (specification §4 US-015).

export const SORTS = ["name", "company", "email", "updatedAt"] as const;
export type Sort = (typeof SORTS)[number];
export type Dir = "asc" | "desc";

export interface ListQuery {
  q: string;
  sort: Sort;
  dir: Dir;
  page: number;
}

const DEFAULTS: ListQuery = { q: "", sort: "name", dir: "asc", page: 1 };

function isSort(value: string | null): value is Sort {
  return (SORTS as readonly (string | null)[]).includes(value);
}

/** The query of the URL; anything unknown is the default (R-014-02). */
export function listQueryOf(params: URLSearchParams): ListQuery {
  const sort = params.get("sort");
  const dir = params.get("dir");
  const page = Number(params.get("page"));
  return {
    q: params.get("q") ?? DEFAULTS.q,
    sort: isSort(sort) ? sort : DEFAULTS.sort,
    dir: dir === "desc" ? "desc" : DEFAULTS.dir,
    page: Number.isInteger(page) && page >= 1 ? page : DEFAULTS.page,
  };
}

/** The URL parameters of a query, without the defaults. */
export function searchParamsOf(query: ListQuery): URLSearchParams {
  const params = new URLSearchParams();
  if (query.q !== DEFAULTS.q) params.set("q", query.q);
  if (query.sort !== DEFAULTS.sort) params.set("sort", query.sort);
  if (query.dir !== DEFAULTS.dir) params.set("dir", query.dir);
  if (query.page !== DEFAULTS.page) params.set("page", String(query.page));
  return params;
}

/** `GET /groups/{groupId}/contacts` with every parameter of the query (contract listContacts). */
export function apiPath(groupId: string, query: ListQuery): string {
  const params = new URLSearchParams({
    q: query.q,
    sort: query.sort,
    dir: query.dir,
    page: String(query.page),
  });
  return `/groups/${groupId}/contacts?${params.toString()}`;
}
