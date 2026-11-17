// What the User asked of the Contact list, kept in the URL so the list, its count, the filters and
// the Export use the same query (specification §4 US-015).

export const SORTS = ["name", "company", "email", "updatedAt"] as const;
export type Sort = (typeof SORTS)[number];
export type Dir = "asc" | "desc";

/** The Active filters (R-015-01 … R-015-04); an empty text or list is not set. */
export interface Filters {
  hasTagIds: string[];
  withoutTagIds: string[];
  attributeKey: string;
  attributeValue: string;
  company: string;
  country: string;
}

export interface ListQuery {
  q: string;
  sort: Sort;
  dir: Dir;
  page: number;
  filters: Filters;
}

export const NO_FILTERS: Filters = {
  hasTagIds: [],
  withoutTagIds: [],
  attributeKey: "",
  attributeValue: "",
  company: "",
  country: "",
};

const DEFAULTS: ListQuery = { q: "", sort: "name", dir: "asc", page: 1, filters: NO_FILTERS };

/** Whether any filter is set (R-015-06: chips and "Clear filters" show then). */
export function hasFilters(filters: Filters): boolean {
  return entriesOf(filters).length > 0;
}

function filtersOf(params: URLSearchParams): Filters {
  return {
    hasTagIds: params.getAll("hasTagIds"),
    withoutTagIds: params.getAll("withoutTagIds"),
    attributeKey: params.get("attributeKey") ?? "",
    attributeValue: params.get("attributeValue") ?? "",
    company: params.get("company") ?? "",
    country: params.get("country") ?? "",
  };
}

/** The set filters as URL parameters, in the contract's order. */
function entriesOf(filters: Filters): [string, string][] {
  const texts: [string, string][] = [
    ["attributeKey", filters.attributeKey],
    ["attributeValue", filters.attributeValue],
    ["company", filters.company],
    ["country", filters.country],
  ];
  return [
    ...filters.hasTagIds.map((id): [string, string] => ["hasTagIds", id]),
    ...filters.withoutTagIds.map((id): [string, string] => ["withoutTagIds", id]),
    ...texts.filter(([, value]) => value !== ""),
  ];
}

function appendFilters(params: URLSearchParams, filters: Filters): URLSearchParams {
  for (const [name, value] of entriesOf(filters)) params.append(name, value);
  return params;
}

function isSort(value: string | null): value is Sort {
  return (SORTS as readonly (string | null)[]).includes(value);
}

/** The query of the URL; anything unknown is the default (R-014-02), filters as given (R-015-05). */
export function listQueryOf(params: URLSearchParams): ListQuery {
  const sort = params.get("sort");
  const dir = params.get("dir");
  const page = Number(params.get("page"));
  return {
    q: params.get("q") ?? DEFAULTS.q,
    sort: isSort(sort) ? sort : DEFAULTS.sort,
    dir: dir === "desc" ? "desc" : DEFAULTS.dir,
    page: Number.isInteger(page) && page >= 1 ? page : DEFAULTS.page,
    filters: filtersOf(params),
  };
}

/** The URL parameters of a query, without the defaults. */
export function searchParamsOf(query: ListQuery): URLSearchParams {
  const params = new URLSearchParams();
  if (query.q !== DEFAULTS.q) params.set("q", query.q);
  if (query.sort !== DEFAULTS.sort) params.set("sort", query.sort);
  if (query.dir !== DEFAULTS.dir) params.set("dir", query.dir);
  if (query.page !== DEFAULTS.page) params.set("page", String(query.page));
  return appendFilters(params, query.filters);
}

/** `GET /groups/{groupId}/contacts` with every parameter of the query (contract listContacts). */
export function apiPath(groupId: string, query: ListQuery): string {
  const params = new URLSearchParams({
    q: query.q,
    sort: query.sort,
    dir: query.dir,
    page: String(query.page),
  });
  return `/groups/${groupId}/contacts?${appendFilters(params, query.filters).toString()}`;
}
