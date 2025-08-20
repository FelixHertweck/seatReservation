import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import { detectLanguageFromHeader } from "./lib/i18n/language-detector";
import { fallbackLng, languages, cookieName } from "./lib/i18n/config";

export function middleware(request: NextRequest) {
  if (request.nextUrl.pathname.startsWith("/api/")) {
    return NextResponse.next();
  }

  // Check if there is any supported locale in the pathname
  const pathname = request.nextUrl.pathname;
  const pathnameIsMissingLocale = languages.every(
    (locale) =>
      !pathname.startsWith(`/${locale}/`) && pathname !== `/${locale}`,
  );

  // Redirect if there is no locale
  if (pathnameIsMissingLocale) {
    const locale = getLocale(request);

    // Redirect to the same path with locale prefix
    return NextResponse.redirect(
      new URL(
        `/${locale}${pathname.startsWith("/") ? "" : "/"}${pathname}`,
        request.url,
      ),
    );
  }
}

function getLocale(request: NextRequest): string {
  // Try to get language from cookie first
  const cookieLng = request.cookies.get(cookieName)?.value;
  if (cookieLng && languages.includes(cookieLng)) {
    return cookieLng;
  }

  // Try to get language from Accept-Language header
  const acceptLng = request.headers.get("Accept-Language");
  if (acceptLng) {
    const detectedLng = detectLanguageFromHeader(acceptLng, languages);
    if (detectedLng) {
      return detectedLng;
    }
  }

  return fallbackLng;
}

export const config = {
  // Matcher ignoring `/_next/`, `/api/`, and all static assets
  matcher: [
    "/((?!_next/static|_next/image|favicon.ico|api|.*\\.(?:png|jpg|jpeg|gif|svg|ico|webp|webmanifest|xml|txt)$).*)",
  ],
};
