"use client";

import { useEffect, useState } from "react";

import { geocode, type GeocodeResult } from "@/lib/geocode-queue";

export type { GeocodeResult };

/**
 * Debounced client-side Nominatim geocoding, purely to position map
 * previews (no server-side lat/lng persistence). Requests are cached and
 * rate-limited across callers by `geocode-queue`.
 */
export function useGeocode(query: string, debounceMs = 800) {
  const trimmed = query.trim();
  const tooShort = trimmed.length < 3;

  const [result, setResult] = useState<GeocodeResult | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    // Too-short queries need no fetch and no state reset here - `tooShort`
    // already masks the stale result/isLoading/notFound below at render time.
    if (tooShort) return;

    const controller = new AbortController();
    // Deferred a tick so these aren't synchronous setState calls in the
    // effect body itself (React flags that as a cascading-render risk).
    Promise.resolve().then(() => {
      if (controller.signal.aborted) return;
      setIsLoading(true);
      setNotFound(false);
    });

    const timer = setTimeout(() => {
      geocode(trimmed, controller.signal)
        .then((data) => {
          if (data === null) {
            setResult(null);
            setNotFound(true);
          } else {
            setResult(data);
            setNotFound(false);
          }
        })
        .catch((err: unknown) => {
          if (err instanceof Error && err.name === "AbortError") return;
          setResult(null);
          setNotFound(true);
        })
        .finally(() => setIsLoading(false));
    }, debounceMs);

    return () => {
      clearTimeout(timer);
      controller.abort();
    };
  }, [trimmed, tooShort, debounceMs]);

  return {
    result: tooShort ? null : result,
    isLoading: !tooShort && isLoading,
    notFound: !tooShort && notFound,
  };
}
