import { cleanup, configure } from "@testing-library/react";
import { afterEach } from "vitest";
import "../i18n";

// Vitest globals are off, so Testing Library cannot register its automatic cleanup.
afterEach(cleanup);

// findBy*/waitFor give up after 1 s by default; under coverage or mutation testing a full form render
// plus the 300 ms Tag suggestion delay can take longer (TagsField.test.tsx, quality gate).
configure({ asyncUtilTimeout: 4000 });
