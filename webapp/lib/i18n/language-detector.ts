import { cookies, headers } from "next/headers";
import { fallbackLng, languages, cookieName } from "./config";

function parseAcceptLanguage(acceptLanguage: string): string[] {
  return acceptLanguage
    .split(",")
    .map((lang) => {
      const [code, q = "1"] = lang.trim().split(";q=");
      return {
        code: code.split("-")[0], // Get base language code (e.g., 'en' from 'en-US')
        quality: Number.parseFloat(q),
      };
    })
    .sort((a, b) => b.quality - a.quality)
    .map((lang) => lang.code);
}

export function detectLanguageFromHeader(
  acceptLanguage: string,
  supportedLanguages: string[],
): string | null {
  const preferredLanguages = parseAcceptLanguage(acceptLanguage);
  return (
    preferredLanguages.find((lang) => supportedLanguages.includes(lang)) || null
  );
}

export async function detectLanguage(): Promise<string> {
  // Try to get language from cookie first
  const cookieStore = cookies();
  const cookieLng = (await cookieStore).get(cookieName)?.value;

  if (cookieLng && languages.includes(cookieLng)) {
    return cookieLng;
  }

  // Try to get language from Accept-Language header
  const headersList = headers();
  const acceptLng = (await headersList).get("Accept-Language");

  if (acceptLng) {
    const preferredLanguages = parseAcceptLanguage(acceptLng);
    const detectedLng = preferredLanguages.find((lang) =>
      languages.includes(lang),
    );

    if (detectedLng) {
      return detectedLng;
    }
  }

  return fallbackLng;
}
