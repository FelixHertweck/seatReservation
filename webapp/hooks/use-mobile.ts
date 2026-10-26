import * as React from "react";

const MOBILE_BREAKPOINT = 768;

/**
 * Whether the viewport is narrower than `breakpoint`, or `undefined` until
 * it has been measured on the client (first render and SSR) - lets callers
 * render a neutral state instead of briefly flashing the wrong layout.
 */
export function useIsBelowBreakpoint(breakpoint: number) {
  const subscribe = React.useCallback(
    (onChange: () => void) => {
      const mql = window.matchMedia(`(max-width: ${breakpoint - 1}px)`);
      mql.addEventListener("change", onChange);
      return () => mql.removeEventListener("change", onChange);
    },
    [breakpoint],
  );

  return React.useSyncExternalStore<boolean | undefined>(
    subscribe,
    () => window.innerWidth < breakpoint,
    () => undefined,
  );
}

export function useIsMobile() {
  return !!useIsBelowBreakpoint(MOBILE_BREAKPOINT);
}
