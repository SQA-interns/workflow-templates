import { afterEach, describe, expect, it, vi } from "vitest";
import { apiRequest, setTokenSource } from "./apiClient";
import { ApiError } from "./problem";

function respond(status: number, body: unknown, type = "application/json") {
  const fetchMock = vi.fn().mockResolvedValue(
    new Response(body === undefined ? null : JSON.stringify(body), {
      status,
      headers: { "Content-Type": type },
    }),
  );
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

afterEach(() => {
  vi.unstubAllGlobals();
  setTokenSource(async () => null);
});

describe("apiClient (ADR-06, R-001-02)", () => {
  it("R-001-02 sends the ID token as a Bearer token to /api/v1", async () => {
    const fetchMock = respond(200, { email: "ana.kovac@example.si" });
    setTokenSource(async () => "id-token");

    const me = await apiRequest<{ email: string }>("GET", "/me");

    expect(me.email).toBe("ana.kovac@example.si");
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe("/api/v1/me");
    expect(new Headers(init.headers).get("Authorization")).toBe("Bearer id-token");
  });

  it("R-001-06 sends no Authorization header without a token", async () => {
    const fetchMock = respond(200, {});

    await apiRequest("GET", "/me");

    const [, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(new Headers(init.headers).has("Authorization")).toBe(false);
  });

  it("sends a JSON body", async () => {
    const fetchMock = respond(200, {});

    await apiRequest("PATCH", "/me", { language: "en" });

    const [, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(init.method).toBe("PATCH");
    expect(init.body).toBe('{"language":"en"}');
    expect(new Headers(init.headers).get("Content-Type")).toBe("application/json");
  });

  it("returns undefined for 204", async () => {
    respond(204, undefined);

    await expect(apiRequest("DELETE", "/me")).resolves.toBeUndefined();
  });

  it("R-001-05 turns a problem into an ApiError with status and code", async () => {
    respond(
      403,
      { type: "about:blank", title: "Forbidden", status: 403, code: "auth.emailNotVerified" },
      "application/problem+json",
    );

    const error = await apiRequest("GET", "/groups").catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).status).toBe(403);
    expect((error as ApiError).code).toBe("auth.emailNotVerified");
  });

  it("SB-07 an answer without a problem body becomes code internal", async () => {
    respond(502, "Bad Gateway", "text/html");

    const error = (await apiRequest("GET", "/groups").catch((e: unknown) => e)) as ApiError;

    expect(error.status).toBe(502);
    expect(error.code).toBe("internal");
  });
});
