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
