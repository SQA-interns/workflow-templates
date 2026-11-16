import { describe, expect, it } from "vitest";
import { apiPath, listQueryOf, searchParamsOf } from "./listQuery";

describe("Contact list query in the URL (R-014-01 … R-014-04, specification §4 US-015)", () => {
  it("R-014-02 no parameters: name A–Z, Page 1, no search", () => {
    expect(listQueryOf(new URLSearchParams())).toEqual({
      q: "",
      sort: "name",
      dir: "asc",
      page: 1,
    });
  });

  it("R-014-03 sort, order, Page and search are read from the URL", () => {
    expect(listQueryOf(new URLSearchParams("q=novak&sort=company&dir=desc&page=3"))).toEqual({
      q: "novak",
      sort: "company",
      dir: "desc",
      page: 3,
    });
  });

  it("R-014-03 unknown values fall back to the defaults", () => {
    expect(listQueryOf(new URLSearchParams("sort=size&dir=up&page=-2"))).toEqual({
      q: "",
      sort: "name",
      dir: "asc",
      page: 1,
    });
  });

  it("R-014-01 the URL keeps only what differs from the defaults", () => {
    expect(searchParamsOf({ q: "", sort: "name", dir: "asc", page: 1 }).toString()).toBe("");
    expect(searchParamsOf({ q: "čuk", sort: "email", dir: "desc", page: 2 }).toString()).toBe(
      "q=%C4%8Duk&sort=email&dir=desc&page=2",
    );
  });

  it("R-014-01 the API path asks for exactly that Page", () => {
    expect(apiPath("g1", { q: "a_b", sort: "name", dir: "asc", page: 1 })).toBe(
      "/groups/g1/contacts?q=a_b&sort=name&dir=asc&page=1",
    );
  });
});
