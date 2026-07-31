"use client";

import { useState } from "react";

/**
 * Local editable buffer that resets when `resetKey` changes externally,
 * while letting the user type freely in between. Pass a separate `resetKey`
 * when two entities could share the same `value` and fail to reset.
 */
export function useSyncedField<T>(
  value: T,
  resetKey: unknown = value,
): [T, (next: T) => void] {
  const [local, setLocal] = useState(value);
  const [lastKey, setLastKey] = useState(resetKey);

  if (resetKey !== lastKey) {
    setLastKey(resetKey);
    setLocal(value);
  }

  return [local, setLocal];
}
