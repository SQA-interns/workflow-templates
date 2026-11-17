import { describe, expect, it } from "vitest";
import { apiPath, hasFilters, listQueryOf, NO_FILTERS, searchParamsOf } from "./listQuery";

describe("Contact list query in the URL (R-014-01 … R-014-04, specification §4 US-015)", () => {
  it("R-014-02 no parameters: name A–Z, Page 1, no search", () => {
    expect(listQueryOf(new URLSearchParams())).toEqual({
      q: "",
      sort: "name",
      dir: "asc",
      page: 1,
      filters: NO_FILTERS,
    });
  });

  it("R-014-03 sort, order, Page and search are read from the URL", () => {
    expect(listQueryOf(new URLSearchParams("q=novak&sort=company&dir=desc&page=3"))).toEqual({
      q: "novak",
      sort: "company",
      dir: "desc",
      page: 3,
      filters: NO_FILTERS,
    });
  });

  it("R-014-03 unknown values fall back to the defaults", () => {
    expect(listQueryOf(new URLSearchParams("sort=size&dir=up&page=-2"))).toEqual({
      q: "",
      sort: "name",
      dir: "asc",
      page: 1,
      filters: NO_FILTERS,
    });
  });

  it("R-014-01 the URL keeps only what differs from the defaults", () => {
    expect(
      searchParamsOf({ q: "", sort: "name", dir: "asc", page: 1, filters: NO_FILTERS }).toString(),
    ).toBe("");
    expect(
      searchParamsOf({
        q: "čuk",
        sort: "email",
        dir: "desc",
        page: 2,
        filters: NO_FILTERS,
      }).toString(),
    ).toBe("q=%C4%8Duk&sort=email&dir=desc&page=2");
  });

  it("R-014-01 the API path asks for exactly that Page", () => {
    expect(
      apiPath("g1", { q: "a_b", sort: "name", dir: "asc", page: 1, filters: NO_FILTERS }),
    ).toBe("/groups/g1/contacts?q=a_b&sort=name&dir=asc&page=1");
  });

  const filters = {
    hasTagIds: ["t1", "t2"],
    withoutTagIds: ["t3"],
    attributeKey: "Kompetenca",
    attributeValue: "SPLETNO",
    company: "Šola",
    country: "SI",
  };
  const filtered =
    "hasTagIds=t1&hasTagIds=t2&withoutTagIds=t3&attributeKey=Kompetenca" +
    "&attributeValue=SPLETNO&company=%C5%A0ola&country=SI";

  it("R-015-05 the filters are read from the URL with the search", () => {
    expect(listQueryOf(new URLSearchParams(`q=maja&${filtered}`)).filters).toEqual(filters);
  });

  it("R-015-06 the URL keeps the set filters only", () => {
    const query = { q: "", sort: "name", dir: "asc", page: 1, filters } as const;
    expect(searchParamsOf(query).toString()).toBe(filtered);
    expect(searchParamsOf({ ...query, filters: { ...NO_FILTERS, country: "AT" } }).toString()).toBe(
      "country=AT",
    );
  });

  it("R-015-05 the API path asks with every set filter", () => {
    const query = { q: "", sort: "name", dir: "asc", page: 1, filters } as const;
    expect(apiPath("g1", query)).toBe(
      `/groups/g1/contacts?q=&sort=name&dir=asc&page=1&${filtered}`,
    );
  });

  it("R-015-06 a query has filters while any one is set", () => {
    expect(hasFilters(NO_FILTERS)).toBe(false);
    expect(hasFilters({ ...NO_FILTERS, withoutTagIds: ["t3"] })).toBe(true);
    expect(hasFilters({ ...NO_FILTERS, company: "Šola" })).toBe(true);
  });
});
