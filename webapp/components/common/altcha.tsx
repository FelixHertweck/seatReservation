"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { getApiAltchaChallenge } from "@/api/sdk.gen";

interface AltchaProps {
  readonly onVerified: (payload: string) => void;
  readonly onError?: (error: unknown) => void;
  readonly resetKey?: number;
}

/**
 * Invisible ALTCHA Proof-of-Work widget.
 *
 * Fetches a fresh challenge via the generated API client on mount,
 * then passes it directly as a JSON string to the widget (attribute `challenge`).
 *
 * The widget runs Proof-of-Work automatically in the background (auto="onload", display="invisible")
 * and notifies `onVerified(payload)` with the Base64-encoded solution payload.
 *
 * The server-signed challenge carries its own expiry, and the widget moves itself to an
 * "expired" state once that passes (e.g. the user took a while filling out the form) without
 * refetching on its own. We react to that by clearing the now-stale payload and fetching a
 * replacement challenge, remounting the widget (via `key`) so its built-in auto="onload"
 * re-solves it.
 */
export function Altcha({ onVerified, onError, resetKey }: AltchaProps) {
  const widgetRef = useRef<HTMLElement>(null);
  const [challengeJson, setChallengeJson] = useState<string | null>(null);
  const [isMounted, setIsMounted] = useState(false);
  const isFirstResetKey = useRef(true);

  const fetchChallenge = useCallback(() => {
    // Any previously-verified payload is no longer trustworthy once we ask for a new challenge.
    onVerified("");
    getApiAltchaChallenge()
      .then(({ data }) => {
        if (data) {
          const serialized = JSON.stringify(data, (_, value) =>
            typeof value === "bigint" ? Number(value) : value,
          );
          setChallengeJson(serialized);
        }
      })
      .catch((err: unknown) => {
        console.error("Failed to fetch ALTCHA challenge:", err);
        onError?.(err);
      });
  }, [onVerified, onError]);

  // Fetch challenge on mount using generated API client
  useEffect(() => {
    setIsMounted(true);
    fetchChallenge();
  }, []);

  // Caller-driven refetch
  useEffect(() => {
    if (isFirstResetKey.current) {
      isFirstResetKey.current = false;
      return;
    }
    fetchChallenge();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [resetKey]);

  useEffect(() => {
    if (!isMounted || !challengeJson) return;

    // Dynamically import custom element on client-side
    import("altcha");

    const widget = widgetRef.current;
    if (!widget) return;

    const handleStateChange = (event: Event) => {
      const customEvent = event as CustomEvent<{
        state?: string;
        payload?: string;
        error?: unknown;
      }>;
      if (
        customEvent.detail?.state === "verified" &&
        customEvent.detail?.payload
      ) {
        onVerified(customEvent.detail.payload);
      } else if (customEvent.detail?.state === "expired") {
        // The solved payload is now stale
        fetchChallenge();
      } else if (customEvent.detail?.state === "error") {
        onVerified("");
        onError?.(customEvent.detail?.error);
      }
    };

    const handleVerified = (event: Event) => {
      const customEvent = event as CustomEvent<{
        payload?: string;
      }>;
      if (customEvent.detail?.payload) {
        onVerified(customEvent.detail.payload);
      }
    };

    widget.addEventListener("statechange", handleStateChange);
    widget.addEventListener("verified", handleVerified);

    return () => {
      widget.removeEventListener("statechange", handleStateChange);
      widget.removeEventListener("verified", handleVerified);
    };
  }, [isMounted, challengeJson, onVerified, onError, fetchChallenge]);

  if (!isMounted || !challengeJson) return null;

  return (
    <div
      style={{ position: "absolute", width: 0, height: 0, overflow: "hidden" }}
      aria-hidden="true"
    >
      <altcha-widget
        key={challengeJson}
        ref={widgetRef as unknown as React.RefObject<HTMLDivElement>}
        challenge={challengeJson}
        auto="onload"
        display="invisible"
        hidefooter="true"
        hidelogo="true"
      />
    </div>
  );
}
