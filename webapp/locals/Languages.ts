"use client";
// Constants for language support

import i18n from "i18next";
import LanguageDetector from "i18next-browser-languagedetector";
import { initReactI18next } from "react-i18next";
import translationEN from "./en/translation.json";
import translationDE from "./de/translation.json";

export const LanguageLabels = new Map([
  ["en", "English"],
  ["de", "Deutsch"],
]);

const resources = {
  en: {
    translation: translationEN,
  },
  de: {
    translation: translationDE,
  },
};

export function initI18N() {
  void i18n
    .use(LanguageDetector)
    .use(initReactI18next)
    .init({
      resources,
      fallbackLng: "en",
      interpolation: {
        escapeValue: false,
      },
    });
}
