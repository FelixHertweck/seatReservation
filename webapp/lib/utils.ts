import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function isValidRedirectUrl(decodedUrl: string): boolean {
  // Check for relative URLs
  if (decodedUrl.startsWith("/") && !decodedUrl.startsWith("//")) {
    return true;
  }

  if (
    typeof window === "undefined" ||
    !window.location ||
    !window.location.origin
  ) {
    // window is not available (e.g., server-side), cannot validate absolute URLs
    return false;
  }

  // Check for absolute URLs of the same origin
  const currentOrigin = window.location.origin;
  const targetUrl = new URL(decodedUrl, currentOrigin);

  return (
    targetUrl.origin === currentOrigin &&
    targetUrl.pathname.startsWith("/") &&
    !decodedUrl.includes("javascript:") &&
    !decodedUrl.includes("data:")
  );
}

export function isValidRedirectUrlEncoded(url: string): boolean {
  try {
    const decodedUrl = decodeURIComponent(url);
    return isValidRedirectUrl(decodedUrl);
  } catch {
    return false; // Invalid URL encoding
  }
}

/**
 * Formats a date string or Date object to display date and time separately
 * @param dateValue - The date string or Date object to format
 * @param locale - The locale to use for formatting (default: "de-DE")
 * @returns An object with date and time strings, or null if dateValue is invalid
 */
export function formatDateTime(
  dateValue: string | Date | null | undefined,
  locale: string = "de-DE",
): { date: string; time: string } | null {
  if (!dateValue) return null;

  try {
    const date = new Date(dateValue);
    if (isNaN(date.getTime())) return null;

    return {
      date: date.toLocaleDateString(locale, {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
      }),
      time: date.toLocaleTimeString(locale, {
        hour: "2-digit",
        minute: "2-digit",
      }),
    };
  } catch {
    return null;
  }
}
