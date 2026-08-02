"use client";

import { useCallback, useEffect, useState } from "react";

/**
 * Measures how tall `ref`'s element can grow to fill the viewport down to
 * the footer, re-measuring on resize since header/footer height varies.
 *
 * Uses a callback ref (rather than useRef) so the measurement effect re-runs
 * whenever the element actually mounts - important when the ref'd node only
 * appears conditionally (e.g. after data loads) instead of on first render.
 */
export function useFillHeight<T extends HTMLElement>(fallbackPx = 480) {
  const [el, setEl] = useState<T | null>(null);
  const [height, setHeight] = useState<number | null>(null);

  const ref = useCallback((node: T | null) => {
    setEl(node);
  }, []);

  useEffect(() => {
    if (!el) return;

    const footer = document.querySelector("footer");

    const measure = () => {
      const top = el.getBoundingClientRect().top;
      const footerHeight = footer?.getBoundingClientRect().height ?? 0;
      const available = window.innerHeight - top - footerHeight;
      setHeight(Math.max(320, Math.round(available)));
    };

    measure();

    const resizeObserver = new ResizeObserver(measure);
    resizeObserver.observe(el);
    if (footer) resizeObserver.observe(footer);
    window.addEventListener("resize", measure);

    return () => {
      resizeObserver.disconnect();
      window.removeEventListener("resize", measure);
    };
  }, [el]);

  return { ref, height: height ?? fallbackPx };
}
