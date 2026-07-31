"use client";

import { useEffect, useRef, useState } from "react";

/**
 * Measures how tall `ref`'s element can grow to fill the viewport down to
 * the footer, re-measuring on resize since header/footer height varies.
 */
export function useFillHeight<T extends HTMLElement>(fallbackPx = 480) {
  const ref = useRef<T | null>(null);
  const [height, setHeight] = useState<number | null>(null);

  useEffect(() => {
    const el = ref.current;
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
  }, []);

  return { ref, height: height ?? fallbackPx };
}
