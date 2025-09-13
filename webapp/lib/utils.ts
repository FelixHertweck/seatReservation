import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function isValidRedirectUrl(url: string): boolean {
  try {
    const decodedUrl = decodeURIComponent(url);

    // Check for relative URLs
    if (decodedUrl.startsWith("/") && !decodedUrl.startsWith("//")) {
      return true;
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
  } catch (e) {
    return false;
  }
}
