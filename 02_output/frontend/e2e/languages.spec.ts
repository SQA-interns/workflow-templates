// NFR-08 (specification §8): a screenshot of every screen in both languages at the three widths
// (UI-22), with the page language set for screen readers (UI-19) and no horizontal scrolling.
// Screens before sign-in exist in Slovenian only: the language is a setting of the signed-in User
// (R-020-01, D-37). The screenshots are written to the test's output folder.
import { expect, test } from "@playwright/test";
import {
  SCREENS,
  WIDTHS,
  atWidth,
  makeFixture,
  setLanguage,
  type Fixture,
} from "./support/screens";

test.describe.configure({ mode: "serial" });

let fx: Fixture;

test.beforeAll(async () => {
  fx = await makeFixture();
});

for (const language of ["sl", "en"] as const) {
  test.describe(`NFR-08 screens in ${language}`, () => {
    test.beforeAll(async () => {
      await setLanguage(fx, language);
    });

    for (const screen of SCREENS.filter((s) => s.signedIn || language === "sl")) {
      test(`NFR-08 ${screen.id} ${screen.name} in ${language}`, async ({ page }, testInfo) => {
        await screen.open(page, fx);
        await expect(page.locator("html")).toHaveAttribute("lang", language);
        for (const width of WIDTHS) {
          await atWidth(page, width);
          const overflows = await page.evaluate(
            () => document.documentElement.scrollWidth > window.innerWidth,
          );
          expect(overflows, `${screen.id} ${width.label} scrolls sideways`).toBe(false);
          await page.screenshot({
            path: testInfo.outputPath(`${screen.id}_${screen.name}_${language}_${width.label}.png`),
            fullPage: true,
          });
        }
      });
    }
  });
}
