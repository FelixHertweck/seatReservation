import { useSyncExternalStore } from "react";

function subscribe(onChange: () => void) {
  const id = setInterval(onChange, 1000);
  return () => clearInterval(id);
}

/** Current Unix time in whole seconds, ticking every second and frozen at `capSeconds` (stops re-rendering). */
export function useNowSeconds(capSeconds = Number.POSITIVE_INFINITY) {
  return useSyncExternalStore(
    subscribe,
    () => Math.min(Math.floor(Date.now() / 1000), capSeconds),
    () => 0,
  );
}
