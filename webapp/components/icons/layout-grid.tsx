"use client";

import type { Variants } from "motion/react";
import { motion, useAnimation } from "motion/react";
import type { HTMLAttributes } from "react";
import { forwardRef, useCallback, useImperativeHandle, useRef, useEffect } from "react";

import { cn } from "@/lib/utils";

export interface LayoutGridIconHandle {
  startAnimation: () => void;
  stopAnimation: () => void;
}

interface LayoutGridIconProps extends HTMLAttributes<HTMLDivElement> {
  size?: number | string;
  strokeWidth?: number | string;
  color?: string;
}

const RECT_1_VARIANTS: Variants = {
  normal: { translateX: 0, translateY: 0 },
  animate: {
    translateX: [0, 11, 11, 0],
    translateY: [0, 0, 0, 0],
    transition: { duration: 0.8, ease: "easeInOut", times: [0, 0.4, 0.6, 1] },
  },
};

const RECT_2_VARIANTS: Variants = {
  normal: { translateX: 0, translateY: 0 },
  animate: {
    translateX: [0, 0, 0, 0],
    translateY: [0, 11, 11, 0],
    transition: { duration: 0.8, ease: "easeInOut", times: [0, 0.4, 0.6, 1] },
  },
};

const RECT_3_VARIANTS: Variants = {
  normal: { translateX: 0, translateY: 0 },
  animate: {
    translateX: [0, -11, -11, 0],
    translateY: [0, 0, 0, 0],
    transition: { duration: 0.8, ease: "easeInOut", times: [0, 0.4, 0.6, 1] },
  },
};

const RECT_4_VARIANTS: Variants = {
  normal: { translateX: 0, translateY: 0 },
  animate: {
    translateX: [0, 0, 0, 0],
    translateY: [0, -11, -11, 0],
    transition: { duration: 0.8, ease: "easeInOut", times: [0, 0.4, 0.6, 1] },
  },
};

const LayoutGridIcon = forwardRef<LayoutGridIconHandle, LayoutGridIconProps>(
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
            width="100%" height="100%" className="w-full h-full">
          <motion.rect
            animate={controls}
            height="7"
            initial="normal"
            rx="1"
            variants={RECT_1_VARIANTS}
            width="7"
            x="3"
            y="3"
          />
          <motion.rect
            animate={controls}
            height="7"
            initial="normal"
            rx="1"
            variants={RECT_2_VARIANTS}
            width="7"
            x="14"
            y="3"
          />
          <motion.rect
            animate={controls}
            height="7"
            initial="normal"
            rx="1"
            variants={RECT_3_VARIANTS}
            width="7"
            x="14"
            y="14"
          />
          <motion.rect
            animate={controls}
            height="7"
            initial="normal"
            rx="1"
            variants={RECT_4_VARIANTS}
            width="7"
            x="3"
            y="14"
          />
        </svg>
      </div>
    );
  }
);

LayoutGridIcon.displayName = "LayoutGridIcon";

export { LayoutGridIcon };
