"use client";

import type { Variants } from "motion/react";
import { motion, useAnimation } from "motion/react";
import type { HTMLAttributes } from "react";
import { forwardRef, useCallback, useImperativeHandle, useRef, useEffect } from "react";

import { cn } from "@/lib/utils";

export interface CircleDashedIconHandle {
  startAnimation: () => void;
  stopAnimation: () => void;
}

interface CircleDashedIconProps extends HTMLAttributes<HTMLDivElement> {
  size?: number | string;
  strokeWidth?: number | string;
  color?: string;
}

const PATH_VARIANTS: Variants = {
  normal: { opacity: 1 },
  animate: (i: number) => ({
    opacity: [0, 1],
    transition: { delay: i * 0.1, duration: 0.3 },
  }),
};

const CircleDashedIcon = forwardRef<
  CircleDashedIconHandle,
  CircleDashedIconProps
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
        {[
          "M10.1 2.182a10 10 0 0 1 3.8 0",
          "M13.9 21.818a10 10 0 0 1-3.8 0",
          "M17.609 3.721a10 10 0 0 1 2.69 2.7",
          "M2.182 13.9a10 10 0 0 1 0-3.8",
          "M20.279 17.609a10 10 0 0 1-2.7 2.69",
          "M21.818 10.1a10 10 0 0 1 0 3.8",
          "M3.721 6.391a10 10 0 0 1 2.7-2.69",
          "M6.391 20.279a10 10 0 0 1-2.69-2.7",
        ].map((d, index) => (
          <motion.path
            animate={controls}
            custom={index + 1}
            d={d}
            key={d}
            variants={PATH_VARIANTS}
          />
        ))}
      </svg>
    </div>
  );
});

CircleDashedIcon.displayName = "CircleDashedIcon";

export { CircleDashedIcon };
