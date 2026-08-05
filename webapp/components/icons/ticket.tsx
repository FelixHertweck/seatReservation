"use client";

import type { Transition, Variants } from "motion/react";
import { motion, useAnimation } from "motion/react";
import type { HTMLAttributes } from "react";
import { forwardRef, useCallback, useImperativeHandle, useRef, useEffect } from "react";

import { cn } from "@/lib/utils";

export interface TicketIconHandle {
  startAnimation: () => void;
  stopAnimation: () => void;
}

interface TicketIconProps extends HTMLAttributes<HTMLDivElement> {
  size?: number | string;
  strokeWidth?: number | string;
  color?: string;
}

const TRANSITION: Transition = {
  type: "spring",
  stiffness: 300,
  damping: 20,
};

// Left half + perforation dashes slide left together (the tear line follows).
const LEFT_VARIANTS: Variants = {
  normal: { x: 0 },
  animate: { x: -3 },
};

// Right half slides right and tilts clockwise, as if coming away.
const RIGHT_VARIANTS: Variants = {
  normal: { x: 0, rotate: 0 },
  animate: { x: 3, rotate: 4 },
};

const TicketIcon = forwardRef<TicketIconHandle, TicketIconProps>(
  ({ onMouseEnter, onMouseLeave, className, size, strokeWidth, color, style, ...props }, ref) => {
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
            width="100%" height="100%" className="w-full h-full overflow-visible">
          <motion.g
            animate={controls}
            initial="normal"
            transition={TRANSITION}
            variants={LEFT_VARIANTS}
          >
            <path d="M13 5H4a2 2 0 0 0-2 2v2a3 3 0 0 1 0 6v2a2 2 0 0 0 2 2h9" />
            <path d="M13 5v2" />
            <path d="M13 11v2" />
            <path d="M13 17v2" />
          </motion.g>
          <motion.path
            animate={controls}
            d="M13 5h7a2 2 0 0 1 2 2v2a3 3 0 0 0 0 6v2a2 2 0 0 1-2 2h-7"
            initial="normal"
            style={{ transformOrigin: "13px 12px" }}
            transition={TRANSITION}
            variants={RIGHT_VARIANTS}
          />
        </svg>
      </div>
    );
  }
);

TicketIcon.displayName = "TicketIcon";

export { TicketIcon };
