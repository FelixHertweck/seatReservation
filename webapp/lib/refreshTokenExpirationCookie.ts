"use client";

/**
 * Reads the refreshToken_expiration cookie (Client-Side)
 * For Client Components - only works in the browser
 * @returns The expiration date or null if not present
 */
export function getRefreshTokenExpiration(): Date | null {
  if (typeof document === "undefined") {
    return null;
  }

  const cookies = document.cookie.split("; ");
  const refreshTokenExpirationCookie = cookies.find((cookie) =>
    cookie.startsWith("refreshToken_expiration="),
  );

  if (!refreshTokenExpirationCookie) {
    return null;
  }

  const value = refreshTokenExpirationCookie.split("=")[1];
  const decodedValue = decodeURIComponent(value || "");

  const expirationSeconds = parseInt(decodedValue, 10);
  if (isNaN(expirationSeconds)) {
    return null;
  }

  // Convert seconds to milliseconds for JavaScript Date
  return new Date(expirationSeconds * 1000);
}
