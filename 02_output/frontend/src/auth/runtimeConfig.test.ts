import { afterEach, describe, expect, it, vi } from "vitest";
import { loadRuntimeConfig } from "./runtimeConfig";

function serve(body: unknown, status = 200) {
  const fetchMock = vi.fn().mockResolvedValue(
    new Response(JSON.stringify(body), {
      status,
      headers: { "Content-Type": "application/json" },
    }),
  );
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

afterEach(() => vi.unstubAllGlobals());

describe("runtime configuration /config.json (ADR-05, runtime-config.schema.json)", () => {
  it("reads the Firebase settings and the emulator URL", async () => {
    const fetchMock = serve({
      firebaseApiKey: "fake-api-key",
      firebaseAuthDomain: "localhost",
      firebaseProjectId: "demo-contacts",
      authEmulatorUrl: "http://localhost:9199",
    });

    const config = await loadRuntimeConfig();

    expect(fetchMock.mock.calls[0]?.[0]).toBe("/config.json");
    expect(config).toEqual({
      firebaseApiKey: "fake-api-key",
      firebaseAuthDomain: "localhost",
      firebaseProjectId: "demo-contacts",
      authEmulatorUrl: "http://localhost:9199",
    });
  });

  it("the emulator URL is optional (staging, production)", async () => {
    serve({ firebaseApiKey: "k", firebaseAuthDomain: "d", firebaseProjectId: "p" });

    expect((await loadRuntimeConfig()).authEmulatorUrl).toBeUndefined();
  });

  it("rejects a configuration without a required setting", async () => {
    serve({ firebaseApiKey: "k", firebaseAuthDomain: "d" });

    await expect(loadRuntimeConfig()).rejects.toThrow();
  });

  it("rejects when /config.json cannot be read", async () => {
    serve({}, 404);

    await expect(loadRuntimeConfig()).rejects.toThrow();
  });
});
