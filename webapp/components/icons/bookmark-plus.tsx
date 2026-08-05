"use client";

import type { Variants } from "motion/react";
import { motion, useAnimation } from "motion/react";
import type { HTMLAttributes } from "react";
import { forwardRef, useCallback, useImperativeHandle, useRef, useEffect } from "react";

import { cn } from "@/lib/utils";

export interface BookmarkPlusIconHandle {
  startAnimation: () => void;
  stopAnimation: () => void;
}

interface BookmarkPlusIconProps extends HTMLAttributes<HTMLDivElement> {
  size?: number | string;
  strokeWidth?: number | string;
  color?: string;
}

const BOOKMARK_VARIANTS: Variants = {
  normal: { scaleY: 1, scaleX: 1 },
  animate: {
    scaleY: [1, 1.3, 0.9, 1.05, 1],
    scaleX: [1, 0.9, 1.1, 0.95, 1],
    transition: {
      duration: 0.6,
      ease: "easeOut",
    },
  },
};

const PLUS_LINE_VARIANTS: Variants = {
  normal: { strokeDashoffset: 0, opacity: 1 },
  animate: (i: number) => ({
    strokeDashoffset: [1, 0],
    opacity: 1,
    transition: {
      duration: 0.3,
      ease: "easeOut",
      delay: i * 0.1,
    },
  }),
};

const BookmarkPlusIcon = forwardRef<
  BookmarkPlusIconHandle,
  BookmarkPlusIconProps
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
        <motion.path
          animate={controls}
          d="m19 21-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16z"
          style={{ originX: 0.5, originY: 0.5 }}
          variants={BOOKMARK_VARIANTS}
        />

        <motion.line
          animate={controls}
          custom={0}
          initial="normal"
          pathLength="1"
          strokeDasharray="1 1"
          variants={PLUS_LINE_VARIANTS}
          x1="12"
          x2="12"
          y1="7"
          y2="13"
        />
        <motion.line
          animate={controls}
          custom={1}
          initial="normal"
          pathLength="1"
          strokeDasharray="1 1"
          variants={PLUS_LINE_VARIANTS}
          x1="15"
          x2="9"
          y1="10"
          y2="10"
        />
      </svg>
    </div>
  );
});

BookmarkPlusIcon.displayName = "BookmarkPlusIcon";

export { BookmarkPlusIcon };
