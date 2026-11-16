import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import en from "./en.json";
import sl from "./sl.json";

/** The product languages (project-wide facts); Slovenian is the default (R-020-01). */
export const languages = ["sl", "en"] as const;
export type Language = (typeof languages)[number];

void i18n.use(initReactI18next).init({
  resources: { sl: { translation: sl }, en: { translation: en } },
  lng: "sl",
  fallbackLng: false,
  interpolation: { escapeValue: false },
  returnNull: false,
});

/** The page language follows the chosen language (UI-19: the right voice for screen readers). */
i18n.on("languageChanged", (language) => {
  document.documentElement.lang = language;
});

export default i18n;
