"use client";

import { motion, useAnimation, type Variants } from "motion/react";
import type { HTMLAttributes } from "react";
import { forwardRef, useCallback, useImperativeHandle, useRef, useEffect } from "react";
import { cn } from "@/lib/utils";

export interface SwitchCameraIconHandle {
  startAnimation: () => void;
  stopAnimation: () => void;
}

interface SwitchCameraIconProps extends HTMLAttributes<HTMLDivElement> {
  size?: number | string;
  strokeWidth?: number | string;
  color?: string;
}

const PATH_VARIANTS: Variants = {
  normal: { pathLength: 1 },
  animate: {
    pathLength: [0, 1],
    transition: { duration: 0.4, ease: "linear" },
  },
};

const SwitchCameraIcon = forwardRef<
  SwitchCameraIconHandle,
  SwitchCameraIconProps
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
        animate={controls}
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
          d="M11 19H4a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2h5"
          initial="normal"
          variants={PATH_VARIANTS}
        />
        <motion.path
          animate={controls}
          d="M13 5h7a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2h-5"
          initial="normal"
          variants={PATH_VARIANTS}
        />
        <circle cx="12" cy="12" r="3" />
        <motion.path
          animate={controls}
          d="m18 22-3-3 3-3"
          initial="normal"
          variants={PATH_VARIANTS}
        />
        <motion.path
          animate={controls}
          d="m6 2 3 3-3 3"
          initial="normal"
          variants={PATH_VARIANTS}
        />
      </motion.svg>
    </div>
  );
});

SwitchCameraIcon.displayName = "SwitchCameraIcon";

export { SwitchCameraIcon };
