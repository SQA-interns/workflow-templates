/// <reference types="vitest/config" />
import path from "node:path";
import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

// Dev server proxies the API to a local backend (ADR-05, ADR-08).
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: { alias: { "@": path.resolve(__dirname, "src") } },
  server: { port: 5173, proxy: { "/api": "http://localhost:8081" } },
  test: {
    environment: "jsdom",
    globals: false,
    setupFiles: ["src/test/setup.ts"],
    // TagsField tests take ~1.4 s each under coverage and hit the 5 s default under load (quality gate).
    testTimeout: 15000,
    include: ["src/**/*.test.{ts,tsx}"],
    coverage: {
      provider: "v8",
      include: ["src/**/*.{ts,tsx}"],
      // Measurement exclusions (docs/03_test-strategy.md).
      exclude: [
        "src/components/ui/**",
        "src/main.tsx",
        "src/test/**",
        "src/styles/**",
        "src/i18n/*.json",
        "src/**/*.test.{ts,tsx}",
      ],
      reporter: ["text-summary", "html", "json-summary"],
    },
  },
});
