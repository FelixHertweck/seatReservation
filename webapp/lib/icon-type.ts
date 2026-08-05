import type { ComponentType } from "react";

// lucide-react's own `LucideIcon` type pins the ref to SVGSVGElement, which
// rejects lucide-animated's components (they forward a custom start/stop-
// animation handle instead). This looser shape is satisfied by both, so
// icon fields that hold either family - like nav items rendered as
// `<item.icon />` - can be typed with it instead.
export type AppIcon = ComponentType<{ className?: string; size?: number }>;

// The imperative handle lucide-animated icons expose via ref. Used to drive
// an icon's animation from a hover target other than the icon itself (e.g.
// the whole nav row), since these icons otherwise only animate on hover of
// their own (often small) SVG.
export type AnimatedIconHandle = {
  startAnimation: () => void;
  stopAnimation: () => void;
};
