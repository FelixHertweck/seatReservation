"use client";

import type { Variants } from "motion/react";
import { motion, useAnimation } from "motion/react";
import type { HTMLAttributes } from "react";
import { forwardRef, useCallback, useImperativeHandle, useRef, useEffect } from "react";

import { cn } from "@/lib/utils";

export interface LanguagesIconHandle {
  startAnimation: () => void;
  stopAnimation: () => void;
}

interface LanguagesIconProps extends HTMLAttributes<HTMLDivElement> {
  size?: number | string;
  strokeWidth?: number | string;
  color?: string;
}

const PATH_VARIANTS: Variants = {
  normal: { opacity: 1, pathLength: 1, pathOffset: 0 },
  animate: (custom: number) => ({
    opacity: [0, 1],
    pathLength: [0, 1],
    pathOffset: [1, 0],
    transition: {
      opacity: { duration: 0.01, delay: custom * 0.1 },
      pathLength: {
        type: "spring",
        duration: 0.5,
        bounce: 0,
        delay: custom * 0.1,
      },
    },
  }),
};

const SVG_VARIANTS: Variants = {
  normal: { opacity: 1 },
  animate: {
    opacity: 1,
    transition: {
      staggerChildren: 0.1,
      delayChildren: 0.2,
    },
  },
};

const LanguagesIcon = forwardRef<LanguagesIconHandle, LanguagesIconProps>(
  ({ onMouseEnter, onMouseLeave, className, size, strokeWidth, color, style, ...props }, ref) => {
    const svgControls = useAnimation();
    const pathControls = useAnimation();

    const isControlledRef = useRef(false);

    useImperativeHandle(ref, () => {
      isControlledRef.current = true;

      return {
        startAnimation: () => {
          svgControls.start("animate");
          pathControls.start("animate");
        },
        stopAnimation: () => {
          svgControls.start("normal");
          pathControls.start("normal");
        },
      };
    });

    const handleMouseEnter = useCallback(
      (e: React.MouseEvent<HTMLDivElement>) => {
        if (isControlledRef.current) {
          onMouseEnter?.(e);
        } else {
          svgControls.start("animate");
          pathControls.start("animate");
        }
      },
      [onMouseEnter, pathControls, svgControls]
    );

    const handleMouseLeave = useCallback(
      (e: React.MouseEvent<HTMLDivElement>) => {
        if (isControlledRef.current) {
          onMouseLeave?.(e);
        } else {
          svgControls.start("normal");
          pathControls.start("normal");
        }
      },
      [svgControls, pathControls, onMouseLeave]
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
        <motion.svg
          animate={svgControls}
          fill="none"
          
          stroke={color || "currentColor"}
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth="2"
          variants={SVG_VARIANTS}
          viewBox="0 0 24 24"
          
          xmlns="http://www.w3.org/2000/svg"
            width="100%" height="100%" className="w-full h-full">
          <motion.path
            animate={pathControls}
            custom={3}
            d="m5 8 6 6"
            variants={PATH_VARIANTS}
          />
          <motion.path
            animate={pathControls}
            custom={2}
            d="m4 14 6-6 3-3"
            variants={PATH_VARIANTS}
          />
          <motion.path
            animate={pathControls}
            custom={1}
            d="M2 5h12"
            variants={PATH_VARIANTS}
          />
          <motion.path
            animate={pathControls}
            custom={0}
            d="M7 2h1"
            variants={PATH_VARIANTS}
          />
          <motion.path
            animate={pathControls}
            custom={3}
            d="m22 22-5-10-5 10"
            variants={PATH_VARIANTS}
          />
          <motion.path
            animate={pathControls}
            custom={3}
            d="M14 18h6"
            variants={PATH_VARIANTS}
          />
        </motion.svg>
      </div>
    );
  }
);

LanguagesIcon.displayName = "LanguagesIcon";

export { LanguagesIcon };
