"use client";

import { useState, useEffect } from "react";

export const EMAIL_RESEND_COOLDOWN_SECONDS = 60;

export function useCooldown() {
  const [retryAt, setRetryAt] = useState<number | null>(null);
  const [now, setNow] = useState(() => Date.now());

  useEffect(() => {
    if (!retryAt) return;

    const interval = setInterval(() => {
      const currentNow = Date.now();
      setNow(currentNow);
      if (currentNow >= retryAt) {
        setRetryAt(null);
      }
    }, 1000);

    return () => clearInterval(interval);
  }, [retryAt]);

  const remainingSeconds = retryAt
    ? Math.max(0, Math.ceil((retryAt - now) / 1000))
    : 0;

  return {
    remainingSeconds,
    isActive: remainingSeconds > 0,
    startForSeconds: (seconds: number) => {
      const target = Date.now() + seconds * 1000;
      setNow(Date.now());
      setRetryAt(target);
    },
    startUntil: (instant: string | Date | number) => {
      const target =
        typeof instant === "number" ? instant : new Date(instant).getTime();
      setNow(Date.now());
      setRetryAt(target);
    },
  };
}
