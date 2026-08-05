import { useRef } from "react";
import type { AnimatedIconHandle } from "@/lib/icon-type";

// lucide-animated icons only animate on hover of their own (often small)
// SVG. Attach `ref` to the icon and spread the returned handlers onto the
// enclosing interactive element (button, TabsTrigger, ...) so the whole
// element is the hover target instead.
export function useIconHover() {
  const ref = useRef<AnimatedIconHandle>(null);
  return {
    ref,
    onMouseEnter: () => ref.current?.startAnimation(),
    onMouseLeave: () => ref.current?.stopAnimation(),
  };
}
