"use client";

import { motion, useAnimation, type Variants } from "motion/react";
import type { HTMLAttributes } from "react";
import { forwardRef, useCallback, useImperativeHandle, useRef, useEffect } from "react";
import { cn } from "@/lib/utils";

export interface AlarmClockMinusIconHandle {
  startAnimation: () => void;
  stopAnimation: () => void;
}

interface AlarmClockMinusIconProps extends HTMLAttributes<HTMLDivElement> {
  size?: number | string;
  strokeWidth?: number | string;
  color?: string;
}

const PATH_VARIANTS: Variants = {
  normal: {
    y: 0,
    x: 0,
    transition: {
      duration: 0.2,
      type: "spring",
      stiffness: 200,
      damping: 25,
    },
  },
  animate: {
    y: -1.5,
    x: [-1, 1, -1, 1, -1, 0],
    transition: {
      y: {
        duration: 0.2,
        type: "spring",
        stiffness: 200,
        damping: 25,
      },
      x: {
        duration: 0.3,
        repeat: Number.POSITIVE_INFINITY,
        ease: "linear",
      },
    },
  },
};

const SECONDARY_PATH_VARIANTS: Variants = {
  normal: {
    y: 0,
    x: 0,
    transition: {
      duration: 0.2,
      type: "spring",
      stiffness: 200,
      damping: 25,
    },
  },
  animate: {
    y: -2.5,
    x: [-2, 2, -2, 2, -2, 0],
    transition: {
      y: {
        duration: 0.2,
        type: "spring",
        stiffness: 200,
        damping: 25,
      },
      x: {
        duration: 0.3,
        repeat: Number.POSITIVE_INFINITY,
        ease: "linear",
      },
    },
  },
};

const AlarmClockMinusIcon = forwardRef<
  AlarmClockMinusIconHandle,
  AlarmClockMinusIconProps
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
      <motion.svg
        fill="none"
        
        stroke={color || "currentColor"}
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="2"
        style={{ overflow: "visible" }}
        viewBox="0 0 24 24"
        
        xmlns="http://www.w3.org/2000/svg"
          width="100%" height="100%" className="w-full h-full">
        <motion.circle
          animate={controls}
          cx="12"
          cy="13"
          initial="normal"
          r="8"
          variants={PATH_VARIANTS}
        />
        <motion.path
          animate={controls}
          d="M5 3 2 6"
          initial="normal"
          variants={SECONDARY_PATH_VARIANTS}
        />
        <motion.path
          animate={controls}
          d="m22 6-3-3"
          initial="normal"
          variants={SECONDARY_PATH_VARIANTS}
        />
        <motion.path
          animate={controls}
          d="M6.38 18.7 4 21"
          initial="normal"
          variants={PATH_VARIANTS}
        />
        <motion.path
          animate={controls}
          d="M17.64 18.67 20 21"
          initial="normal"
          variants={PATH_VARIANTS}
        />
        <motion.path
          animate={controls}
          d="M9 13h6"
          initial="normal"
          variants={PATH_VARIANTS}
        />
      </motion.svg>
    </div>
  );
});

AlarmClockMinusIcon.displayName = "AlarmClockMinusIcon";

export { AlarmClockMinusIcon };
