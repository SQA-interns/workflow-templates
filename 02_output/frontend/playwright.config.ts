import { defineConfig, devices } from "@playwright/test";

// Browser tests against the local stack (environments.md, NFR-06): UI acceptance tests in
// acceptance/, the end-to-end scenarios in e2e/ (one test each). Every test empties the stack
// first, so tests run one at a time (D-38). Screenshots at the three widths (UI-22).
export default defineConfig({
  testDir: ".",
  testMatch: ["acceptance/**/*.spec.ts", "e2e/**/*.spec.ts"],
  outputDir: "test-results",
  fullyParallel: false,
  workers: 1,
  timeout: 90_000,
  expect: { timeout: 10_000 },
  reporter: [
    ["list"],
    ["html", { open: "never" }],
    ["junit", { outputFile: "test-results/junit.xml" }],
  ],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? "http://localhost:8080",
    actionTimeout: 15_000,
    navigationTimeout: 30_000,
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  projects: [
    { name: "chromium", use: { ...devices["Desktop Chrome"] } },
    { name: "firefox", use: { ...devices["Desktop Firefox"] } },
    { name: "webkit", use: { ...devices["Desktop Safari"] } },
  ],
});
