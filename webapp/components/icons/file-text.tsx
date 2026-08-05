"use client";

import { motion, useAnimation } from "motion/react";
import type React from "react";
import type { HTMLAttributes } from "react";
import { forwardRef, useCallback, useImperativeHandle, useRef, useEffect } from "react";

import { cn } from "@/lib/utils";

export interface FileTextIconHandle {
  startAnimation: () => void;
  stopAnimation: () => void;
}

interface FileTextIconProps extends HTMLAttributes<HTMLDivElement> {
  size?: number | string;
  strokeWidth?: number | string;
  color?: string;
}

const FILE_TEXT = forwardRef<FileTextIconHandle, FileTextIconProps>(
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
        <motion.svg
          animate={controls}
          fill="none"
          
          initial="normal"
          stroke={color || "currentColor"}
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth="2"
          variants={{
            normal: { scale: 1 },
            animate: {
              scale: 1.05,
              transition: {
                duration: 0.3,
                ease: "easeOut",
              },
            },
          }}
          viewBox="0 0 24 24"
          
          xmlns="http://www.w3.org/2000/svg"
            width="100%" height="100%" className="w-full h-full">
          <path d="M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z" />
          <path d="M14 2v4a2 2 0 0 0 2 2h4" />

          <motion.path
            d="M10 9H8"
            stroke="currentColor"
            strokeWidth="2"
            variants={{
              normal: {
                pathLength: 1,
                x1: 8,
                x2: 10,
              },
              animate: {
                pathLength: [1, 0, 1],
                x1: [8, 10, 8],
                x2: [10, 10, 10],
                transition: {
                  duration: 0.7,
                  delay: 0.3,
                },
              },
            }}
          />
          <motion.path
            d="M16 13H8"
            stroke="currentColor"
            strokeWidth="2"
            variants={{
              normal: {
                pathLength: 1,
                x1: 8,
                x2: 16,
              },
              animate: {
                pathLength: [1, 0, 1],
                x1: [8, 16, 8],
                x2: [16, 16, 16],
                transition: {
                  duration: 0.7,
                  delay: 0.5,
                },
              },
            }}
          />
          <motion.path
            d="M16 17H8"
            stroke="currentColor"
            strokeWidth="2"
            variants={{
              normal: {
                pathLength: 1,
                x1: 8,
                x2: 16,
              },
              animate: {
                pathLength: [1, 0, 1],
                x1: [8, 16, 8],
                x2: [16, 16, 16],
                transition: {
                  duration: 0.7,
                  delay: 0.7,
                },
              },
            }}
          />
        </motion.svg>
      </div>
    );
  }
);

FILE_TEXT.displayName = "FileTextIcon";

export { FILE_TEXT as FileTextIcon };
