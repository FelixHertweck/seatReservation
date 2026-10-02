import * as React from "react";

const MOBILE_BREAKPOINT = 768;

/**
 * Whether the viewport is narrower than `breakpoint`, or `undefined` until
 * it has been measured on the client (first render and SSR) - lets callers
 * render a neutral state instead of briefly flashing the wrong layout.
 */
export function useIsBelowBreakpoint(breakpoint: number) {
  const [isBelow, setIsBelow] = React.useState<boolean | undefined>(undefined);

  React.useEffect(() => {
    const mql = window.matchMedia(`(max-width: ${breakpoint - 1}px)`);
    const onChange = () => {
      setIsBelow(window.innerWidth < breakpoint);
    };
    mql.addEventListener("change", onChange);
    setIsBelow(window.innerWidth < breakpoint);
    return () => mql.removeEventListener("change", onChange);
  }, [breakpoint]);

  return isBelow;
}

export function useIsMobile() {
  return !!useIsBelowBreakpoint(MOBILE_BREAKPOINT);
}
