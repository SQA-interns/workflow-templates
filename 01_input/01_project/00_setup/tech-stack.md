# Tech stack

> For: agent (reads), human (reviews) · Owner: Architect · Agent: read-only

The single source of truth for every platform, dependency and tool version.
Build manifests and lock files in `02_output/` (e.g. `pom.xml`, `package.json`, `build.gradle`, `*.csproj`, `Podfile`) must match this file exactly; the phase 0 gate checks it. Resolvability and vulnerability results are recorded by the agent in `docs/00_preflight-report.md`.

## Rules

- Exact versions only: no ranges, wildcards, `latest` or floating tags.
- Changing any entry below is a blocking decision (`general/rules/decisions.md`). The human's approval in the decision record replaces the entry; nobody edits this file mid-run.
- A pin that resolves and runs but whose tool reports another version (for example an image tag) is a non-blocking decision; the listed pin stays authoritative. A pin that does not resolve or run is blocking.
- A dependency or tool not listed here may be added only with an exact version and a non-blocking decision record; it is scanned in phase 6 like any other.
- Every dependency's licence must be compatible with the project's licence.
- A `platforms` or `tooling` entry may carry `version_check` (a command whose output contains the version) and, for `tooling`, `options` (flags, analysers or rule sets to enable or disable, for example analysers that need credentials not listed in `secrets.env.example`). Preflight uses them.
- `tooling` must include at least one tool for each purpose required by `general/quality/` and `general/security/`: build, format, lint, type check (if the language has one), test, coverage, mutation, static analysis, dependency scan, secret scan; and for a component with a user interface: the UI stack of `general/design/ui-baseline.md` (UI-01), a browser test runner with screenshots, and an accessibility checker.
- A version proposed by `/to-spec` and not yet verified is marked `[ASSUMPTION] verify in preflight`; `/preflight` confirms or replaces it through a decision.

Project licence: proprietary (Q-38). Allowed dependency licences: MIT, Apache-2.0, BSD-2-Clause, BSD-3-Clause, ISC, PostgreSQL, EPL-2.0 (test scope only); tools that are not distributed with the product may differ and are noted.

Versions verified to resolve on: not yet. Every version below is `[ASSUMPTION] verify in preflight`. Stack required by the customer: Spring Boot, React, PostgreSQL, Firebase Authentication (Q-13); versions and remaining choices by the developer (Q-38). Spring Boot manages the versions of its starters, Flyway, the PostgreSQL driver, Testcontainers and JUnit; the resolved managed versions are recorded in the preflight report.

## Platforms

```yaml
platforms:
  - { id: java, name: Eclipse Temurin JDK, version: 21.0.8, source: https://adoptium.net, version_check: "java -version" }   # [ASSUMPTION] verify in preflight
  - { id: node, name: Node.js, version: 22.20.0, source: https://nodejs.org, version_check: "node --version" }   # [ASSUMPTION] verify in preflight
  - { id: postgres, name: PostgreSQL (container postgres), version: 16.10, source: https://hub.docker.com/_/postgres }   # [ASSUMPTION] verify in preflight
  - { id: docker-compose, name: Docker Compose, version: host, source: host, version_check: "docker compose version" }   # host tool, not installed by the agent
```

## Dependencies

One entry per artifact, written the way the ecosystem writes coordinates.

```yaml
dependencies:
  # backend, runtime (Maven Central: https://repo.maven.apache.org/maven2)   all [ASSUMPTION] verify in preflight
  - { id: org.springframework.boot:spring-boot-starter-parent, ecosystem: maven, version: 3.5.7, scope: build, component: backend, source: maven-central, purpose: BOM and plugin management, license: Apache-2.0 }
  - { id: org.springframework.boot:spring-boot-starter-web, ecosystem: maven, version: 3.5.7, scope: runtime, component: backend, source: maven-central, purpose: REST API, license: Apache-2.0 }
  - { id: org.springframework.boot:spring-boot-starter-validation, ecosystem: maven, version: 3.5.7, scope: runtime, component: backend, source: maven-central, purpose: input validation, license: Apache-2.0 }
  - { id: org.springframework.boot:spring-boot-starter-data-jpa, ecosystem: maven, version: 3.5.7, scope: runtime, component: backend, source: maven-central, purpose: persistence, license: Apache-2.0 }
  - { id: org.springframework.boot:spring-boot-starter-security, ecosystem: maven, version: 3.5.7, scope: runtime, component: backend, source: maven-central, purpose: request authentication filter chain, license: Apache-2.0 }
  - { id: org.springframework.boot:spring-boot-starter-actuator, ecosystem: maven, version: 3.5.7, scope: runtime, component: backend, source: maven-central, purpose: health endpoint (NFR-04), license: Apache-2.0 }
  - { id: org.flywaydb:flyway-database-postgresql, ecosystem: maven, version: managed, scope: runtime, component: backend, source: maven-central, purpose: schema migrations (AR-07), license: Apache-2.0 }   # version managed by spring-boot-starter-parent; recorded in preflight
  - { id: org.postgresql:postgresql, ecosystem: maven, version: managed, scope: runtime, component: backend, source: maven-central, purpose: JDBC driver, license: BSD-2-Clause }
  - { id: com.google.firebase:firebase-admin, ecosystem: maven, version: 9.5.0, scope: runtime, component: backend, source: maven-central, purpose: ID token verification (AR-06), license: Apache-2.0 }
  - { id: org.apache.poi:poi-ooxml, ecosystem: maven, version: 5.4.1, scope: runtime, component: backend, source: maven-central, purpose: .xlsx Export (AR-09), license: Apache-2.0 }
  - { id: org.springdoc:springdoc-openapi-starter-webmvc-ui, ecosystem: maven, version: 2.8.13, scope: runtime, component: backend, source: maven-central, purpose: OpenAPI document (AR-02), license: Apache-2.0 }
  - { id: com.bucket4j:bucket4j_jdk17-core, ecosystem: maven, version: 8.14.0, scope: runtime, component: backend, source: maven-central, purpose: rate limit (SR-03), license: Apache-2.0 }
  # backend, test
  - { id: org.springframework.boot:spring-boot-starter-test, ecosystem: maven, version: 3.5.7, scope: test, component: backend, source: maven-central, purpose: JUnit 5, Mockito, AssertJ, license: Apache-2.0 }
  - { id: org.springframework.boot:spring-boot-testcontainers, ecosystem: maven, version: 3.5.7, scope: test, component: backend, source: maven-central, purpose: Testcontainers integration, license: Apache-2.0 }
  - { id: org.testcontainers:postgresql, ecosystem: maven, version: managed, scope: test, component: backend, source: maven-central, purpose: real PostgreSQL in tests, license: MIT }
  - { id: com.tngtech.archunit:archunit-junit5, ecosystem: maven, version: 1.4.1, scope: test, component: backend, source: maven-central, purpose: architecture constraints AR-03, AR-04, AR-06, license: Apache-2.0 }
  # frontend, runtime (npm: https://registry.npmjs.org)   all [ASSUMPTION] verify in preflight
  - { id: react, ecosystem: npm, version: 18.3.1, scope: runtime, component: frontend, source: npm, purpose: UI, license: MIT }
  - { id: react-dom, ecosystem: npm, version: 18.3.1, scope: runtime, component: frontend, source: npm, purpose: UI, license: MIT }
  - { id: react-router, ecosystem: npm, version: 7.9.4, scope: runtime, component: frontend, source: npm, purpose: routing, license: MIT }
  - { id: "@tanstack/react-query", ecosystem: npm, version: 5.90.5, scope: runtime, component: frontend, source: npm, purpose: API data fetching and caching, license: MIT }
  - { id: firebase, ecosystem: npm, version: 12.4.0, scope: runtime, component: frontend, source: npm, purpose: sign-in (US-001), license: Apache-2.0 }
  - { id: i18next, ecosystem: npm, version: 25.6.0, scope: runtime, component: frontend, source: npm, purpose: message catalogs (UI-30), license: MIT }
  - { id: react-i18next, ecosystem: npm, version: 16.2.1, scope: runtime, component: frontend, source: npm, purpose: message catalogs (UI-30), license: MIT }
  - { id: react-hook-form, ecosystem: npm, version: 7.65.0, scope: runtime, component: frontend, source: npm, purpose: forms, license: MIT }
  - { id: zod, ecosystem: npm, version: 4.1.12, scope: runtime, component: frontend, source: npm, purpose: form validation, license: MIT }
  - { id: tailwindcss, ecosystem: npm, version: 4.1.16, scope: build, component: frontend, source: npm, purpose: token-based styling (UI-01), license: MIT }
  - { id: "@tailwindcss/vite", ecosystem: npm, version: 4.1.16, scope: build, component: frontend, source: npm, purpose: Tailwind build plugin, license: MIT }
  - { id: radix-ui, ecosystem: npm, version: 1.4.3, scope: runtime, component: frontend, source: npm, purpose: accessible primitives for shadcn/ui (UI-01), license: MIT }
  - { id: lucide-react, ecosystem: npm, version: 0.548.0, scope: runtime, component: frontend, source: npm, purpose: icons (UI-01), license: ISC }
  - { id: class-variance-authority, ecosystem: npm, version: 0.7.1, scope: runtime, component: frontend, source: npm, purpose: shadcn/ui variants, license: Apache-2.0 }
  - { id: clsx, ecosystem: npm, version: 2.1.1, scope: runtime, component: frontend, source: npm, purpose: shadcn/ui class names, license: MIT }
  - { id: tailwind-merge, ecosystem: npm, version: 3.3.1, scope: runtime, component: frontend, source: npm, purpose: shadcn/ui class names, license: MIT }
  # frontend, test
  - { id: "@testing-library/react", ecosystem: npm, version: 16.3.0, scope: test, component: frontend, source: npm, purpose: component tests, license: MIT }
  - { id: "@testing-library/user-event", ecosystem: npm, version: 14.6.1, scope: test, component: frontend, source: npm, purpose: component tests, license: MIT }
  - { id: jsdom, ecosystem: npm, version: 27.0.1, scope: test, component: frontend, source: npm, purpose: DOM for unit tests, license: MIT }
  - { id: msw, ecosystem: npm, version: 2.11.6, scope: test, component: frontend, source: npm, purpose: API mocks in component tests, license: MIT }
  # containers
  - { id: postgres, ecosystem: container, version: 16.10, scope: runtime, component: all, source: docker-hub, purpose: database, license: PostgreSQL }
  - { id: eclipse-temurin, ecosystem: container, version: 21.0.8_9-jre, scope: runtime, component: backend, source: docker-hub, purpose: backend image base, license: GPL-2.0-with-classpath-exception }   # runtime platform, not linked into the product
  - { id: nginx, ecosystem: container, version: 1.29.2-alpine, scope: runtime, component: frontend, source: docker-hub, purpose: serve static build, proxy /api, license: BSD-2-Clause }
```

## Tooling

```yaml
tooling:
  # backend   all [ASSUMPTION] verify in preflight
  - { id: maven, ecosystem: maven, version: 3.9.11, purpose: build, component: backend, source: maven-central, version_check: "./mvnw -v" }
  - { id: spotless-maven-plugin, ecosystem: maven, version: 2.46.1, purpose: format, component: backend, source: maven-central, options: "google-java-format 1.28.0" }
  - { id: maven-checkstyle-plugin, ecosystem: maven, version: 3.6.0, purpose: lint, component: backend, source: maven-central, options: "checkstyle 11.0.1, google_checks" }
  - { id: javac, ecosystem: maven, version: platform java, purpose: type-check, component: backend, source: platform, options: "-Xlint:all -Werror" }
  - { id: maven-surefire-plugin, ecosystem: maven, version: 3.5.4, purpose: test, component: backend, source: maven-central }
  - { id: maven-failsafe-plugin, ecosystem: maven, version: 3.5.4, purpose: test, component: backend, source: maven-central, options: "integration tests with Testcontainers" }
  - { id: jacoco-maven-plugin, ecosystem: maven, version: 0.8.13, purpose: coverage, component: backend, source: maven-central }
  - { id: pitest-maven, ecosystem: maven, version: 1.20.3, purpose: mutation, component: backend, source: maven-central, options: "pitest-junit5-plugin 1.2.3; targetClasses app.contacts.*" }
  - { id: spotbugs-maven-plugin, ecosystem: maven, version: 4.9.6.0, purpose: static-analysis, component: backend, source: maven-central, options: "findsecbugs-plugin 1.14.0" }
  # frontend
  - { id: vite, ecosystem: npm, version: 7.1.12, purpose: build, component: frontend, source: npm, version_check: "npx vite --version", options: "@vitejs/plugin-react 5.0.4" }
  - { id: prettier, ecosystem: npm, version: 3.6.2, purpose: format, component: frontend, source: npm, options: "prettier-plugin-tailwindcss 0.7.1" }
  - { id: eslint, ecosystem: npm, version: 9.38.0, purpose: lint, component: frontend, source: npm, options: "typescript-eslint 8.46.2, eslint-plugin-react-hooks 7.0.0, eslint-plugin-jsx-a11y 6.10.2" }
  - { id: typescript, ecosystem: npm, version: 5.9.3, purpose: type-check, component: frontend, source: npm, options: "strict" }
  - { id: vitest, ecosystem: npm, version: 3.2.4, purpose: test, component: frontend, source: npm }
  - { id: "@vitest/coverage-v8", ecosystem: npm, version: 3.2.4, purpose: coverage, component: frontend, source: npm }
  - { id: "@stryker-mutator/core", ecosystem: npm, version: 9.2.0, purpose: mutation, component: frontend, source: npm, options: "@stryker-mutator/vitest-runner 9.2.0" }
  - { id: eslint-plugin-security, ecosystem: npm, version: 3.0.1, purpose: static-analysis, component: frontend, source: npm }
  - { id: shadcn, ecosystem: npm, version: 3.5.0, purpose: ui-stack, component: frontend, source: npm, options: "CLI that adds shadcn/ui components (UI-01)" }
  - { id: "@playwright/test", ecosystem: npm, version: 1.56.1, purpose: browser-test, component: frontend, source: npm, options: "Chromium, Firefox, WebKit; screenshots at the three widths (UI-22)" }
  - { id: "@axe-core/playwright", ecosystem: npm, version: 4.11.0, purpose: accessibility, component: frontend, source: npm }
  # all
  - { id: firebase-tools, ecosystem: npm, version: 14.20.0, purpose: test, component: all, source: npm, options: "Auth emulator only, for local and test (no secrets)" }
  - { id: trivy, ecosystem: binary, version: 0.67.2, purpose: dependency-scan, component: all, source: https://github.com/aquasecurity/trivy, version_check: "trivy --version", options: "fs scan of pom.xml and package-lock.json; image scan" }
  - { id: gitleaks, ecosystem: binary, version: 8.28.0, purpose: secret-scan, component: all, source: https://github.com/gitleaks/gitleaks, version_check: "gitleaks version" }
  - { id: k6, ecosystem: binary, version: 1.3.0, purpose: load-test, component: all, source: https://github.com/grafana/k6, version_check: "k6 version", options: "NFR-01 to NFR-03; AGPL-3.0, tool only, never distributed" }
```
