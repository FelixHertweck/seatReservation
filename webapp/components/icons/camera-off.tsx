"use client";

import { motion, useAnimation } from "motion/react";
import { CameraOff as StaticCameraOff } from "lucide-react";
import type { HTMLAttributes } from "react";
import { forwardRef, useCallback, useImperativeHandle, useRef, useEffect } from "react";
import { cn } from "@/lib/utils";

export interface CameraOffIconHandle {
  startAnimation: () => void;
  stopAnimation: () => void;
}

export interface CameraOffIconProps extends HTMLAttributes<HTMLDivElement> {
  size?: number | string;
  strokeWidth?: number | string;
  color?: string;
}

const CameraOffIcon = forwardRef<CameraOffIconHandle, CameraOffIconProps>(
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

    const { onDrag, onDragStart, onDragEnd, onAnimationStart, ...divProps } = props as any;

    
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
      <motion.div ref={divRef}
        className={cn("inline-flex items-center justify-center shrink-0 size-6", className)} style={size !== undefined ? { width: size, height: size, ...style } : style} onMouseEnter={handleMouseEnter}
        onMouseLeave={handleMouseLeave}
        animate={controls}
        initial="normal"
        variants={{
          normal: { scale: 1, rotate: 0 },
          animate: { scale: 1.15, rotate: 5, transition: { type: "spring", stiffness: 300, damping: 15 } },
        }}
        {...divProps}
      >
        <StaticCameraOff size="100%" strokeWidth={strokeWidth} color={color} className="w-full h-full" />
      </motion.div>
    );
  }
);

CameraOffIcon.displayName = "CameraOffIcon";

export { CameraOffIcon };
