"use client";

import { useEffect, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { getApiAuthUsernameAvailabilityOptions } from "@/api/@tanstack/react-query.gen";

const USERNAME_PATTERN = /^[a-zA-Z0-9._-]{3,64}$/;

/**
 * Debounced live check of whether a username is still free, backed by the
 * indexed `/api/auth/username-availability` lookup. Only fires once the
 * value already matches the username format, so partial/invalid input while
 * typing doesn't trigger a request.
 */
export function useUsernameAvailability(username: string, debounceMs = 500) {
  const trimmed = username.trim();
  const [debounced, setDebounced] = useState(trimmed);

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(trimmed), debounceMs);
    return () => clearTimeout(timer);
  }, [trimmed, debounceMs]);

  const isValidFormat = USERNAME_PATTERN.test(debounced);

  const { data, isFetching } = useQuery({
    ...getApiAuthUsernameAvailabilityOptions({
      query: { username: debounced },
    }),
    enabled: isValidFormat,
    retry: false,
  });

  return {
    available: isValidFormat ? (data?.available ?? null) : null,
    isChecking: isValidFormat && isFetching,
  };
}
