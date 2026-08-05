"use client";

import type { Variants } from "motion/react";
import { AnimatePresence, motion, useAnimation } from "motion/react";
import type { HTMLAttributes } from "react";
import { forwardRef, useCallback, useImperativeHandle, useRef, useEffect } from "react";

import { cn } from "@/lib/utils";

export interface CalendarDaysIconHandle {
  startAnimation: () => void;
  stopAnimation: () => void;
}

interface CalendarDaysIconProps extends HTMLAttributes<HTMLDivElement> {
  size?: number | string;
  strokeWidth?: number | string;
  color?: string;
}

const DOTS = [
  { cx: 8, cy: 14 },
  { cx: 12, cy: 14 },
  { cx: 16, cy: 14 },
  { cx: 8, cy: 18 },
  { cx: 12, cy: 18 },
  { cx: 16, cy: 18 },
];

const VARIANTS: Variants = {
  normal: {
    opacity: 1,
    transition: {
      duration: 0.2,
    },
  },
  animate: (i: number) => ({
    opacity: [1, 0.3, 1],
    transition: {
      delay: i * 0.1,
      duration: 0.4,
      times: [0, 0.5, 1],
    },
  }),
};

const CalendarDaysIcon = forwardRef<
  CalendarDaysIconHandle,
  CalendarDaysIconProps
>(({ onMouseEnter, onMouseLeave, className, size, strokeWidth, color, style, ...props }, ref) => {
  const controls = useAnimation();
  const isControlledRef = useRef(false);

  useImperativeHandle(ref, () => {
    isControlledRef.current = true;
    return {
      startAnimation: () => controls.start("animate"),
      stopAnimation: () => controls.start("normal"),
    };
  });

  const handleMouseEnter = useCallback(
    (e: React.MouseEvent<HTMLDivElement>) => {
      if (isControlledRef.current) {
        onMouseEnter?.(e);
      } else {
        controls.start("animate");
      }
    },
    [controls, onMouseEnter]
  );

  const handleMouseLeave = useCallback(
    (e: React.MouseEvent<HTMLDivElement>) => {
      if (isControlledRef.current) {
        onMouseLeave?.(e);
      } else {
        controls.start("normal");
      }
    },
    [controls, onMouseLeave]
  );

  
    const divRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
      const el = divRef.current;
      if (!el) return;
      const parent = el.closest('button, [data-slot="button"], a, .group, label, [role="button"]');
      if (!parent) return;

      const handleEnter = () => handleMouseEnter({} as any);
      const handleLeave = () => handleMouseLeave({} as any);

      parent.addEventListener("mouseenter", handleEnter);
      parent.addEventListener("mouseleave", handleLeave);

      return () => {
        parent.removeEventListener("mouseenter", handleEnter);
        parent.removeEventListener("mouseleave", handleLeave);
      };
    }, [handleMouseEnter, handleMouseLeave]);

    return (
      <div ref={divRef}
      className={cn("inline-flex items-center justify-center shrink-0 size-6", className)} style={size !== undefined ? { width: size, height: size, ...style } : style} onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
      {...props}
    >
      <svg
        fill="none"
        
        stroke={color || "currentColor"}
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="2"
        viewBox="0 0 24 24"
        
        xmlns="http://www.w3.org/2000/svg"
          width="100%" height="100%" className="w-full h-full">
        <path d="M8 2v4" />
        <path d="M16 2v4" />
        <rect height="18" rx="2" width="18" x="3" y="4" />
        <path d="M3 10h18" />
        <AnimatePresence>
          {DOTS.map((dot, index) => (
            <motion.circle
              animate={controls}
              custom={index}
              cx={dot.cx}
              cy={dot.cy}
              fill="currentColor"
              initial="normal"
              key={`${dot.cx}-${dot.cy}`}
              r="1"
              stroke="none"
              variants={VARIANTS}
            />
          ))}
        </AnimatePresence>
      </svg>
    </div>
  );
});

CalendarDaysIcon.displayName = "CalendarDaysIcon";

export { CalendarDaysIcon };
