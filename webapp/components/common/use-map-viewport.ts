"use client";

import { useCallback, useEffect, useRef, useState } from "react";

import { mapPxSize } from "@/components/common/seat-map-geometry";

/**
 * Pan/zoom behavior shared by SeatMap and the location editor canvas.
 * `maxX`/`maxY` (grid cells) drive `resetView`, fitting the map to the
 * container on mount and on extent changes.
 */
export function useMapViewport(maxX: number, maxY: number) {
  const [zoom, setZoom] = useState(1);
  const [pan, setPan] = useState({ x: 0, y: 0 });
  const [isDragging, setIsDragging] = useState(false);
  const [dragStart, setDragStart] = useState({ x: 0, y: 0 });
  const [lastTouchDistance, setLastTouchDistance] = useState<number | null>(
    null,
  );

  const containerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<HTMLDivElement>(null);
  const animationFrameRef = useRef<number | null>(null);

  const handleWheel = useCallback((e: WheelEvent) => {
    e.preventDefault();

    if (animationFrameRef.current) {
      cancelAnimationFrame(animationFrameRef.current);
    }

    animationFrameRef.current = requestAnimationFrame(() => {
      const delta = e.deltaY > 0 ? 0.9 : 1.1;
      setZoom((prev) => Math.max(0.1, Math.min(3, prev * delta)));
    });
  }, []);

  // Register wheel event listener as non-passive
  useEffect(() => {
    const element = containerRef.current;
    if (!element) return;

    element.addEventListener("wheel", handleWheel, { passive: false });

    return () => {
      element.removeEventListener("wheel", handleWheel);
    };
  }, [handleWheel]);

  const handleMouseDown = useCallback(
    (e: React.MouseEvent) => {
      if (e.button === 0) {
        // Left mouse button
        setIsDragging(true);
        setDragStart({ x: e.clientX - pan.x, y: e.clientY - pan.y });
      }
    },
    [pan],
  );

  const handleMouseMove = useCallback(
    (e: React.MouseEvent) => {
      if (isDragging) {
        setPan({
          x: e.clientX - dragStart.x,
          y: e.clientY - dragStart.y,
        });
      }
    },
    [isDragging, dragStart],
  );

  const handleMouseUp = useCallback(() => {
    setIsDragging(false);
  }, []);

  const zoomIn = useCallback(() => {
    setZoom((prev) => Math.min(3, prev * 1.2));
  }, []);

  const zoomOut = useCallback(() => {
    setZoom((prev) => Math.max(0.1, prev * 0.8));
  }, []);

  const resetView = useCallback(() => {
    if (containerRef.current && maxX > 0 && maxY > 0) {
      const container = containerRef.current;
      const containerWidth = container.clientWidth - 32;
      const containerHeight = container.clientHeight - 120;

      const requiredWidth = mapPxSize(maxX);
      const requiredHeight = mapPxSize(maxY);

      const zoomX = containerWidth / requiredWidth;
      const zoomY = containerHeight / requiredHeight;

      const initialZoom = Math.min(zoomX, zoomY, 1);
      setZoom(initialZoom);
      setPan({ x: 0, y: 0 });
    }
  }, [maxX, maxY]);

  const getTouchDistance = useCallback((touches: TouchList) => {
    if (touches.length < 2) return 0;
    const touch1 = touches[0];
    const touch2 = touches[1];
    return Math.sqrt(
      Math.pow(touch2.clientX - touch1.clientX, 2) +
        Math.pow(touch2.clientY - touch1.clientY, 2),
    );
  }, []);

  const handleTouchStart = useCallback(
    (e: TouchEvent) => {
      if (e.touches.length === 1) {
        // Single finger - start panning
        const touch = e.touches[0];
        setIsDragging(true);
        setDragStart({ x: touch.clientX - pan.x, y: touch.clientY - pan.y });
        setLastTouchDistance(null);
      } else if (e.touches.length === 2) {
        // Two fingers - start pinch zoom
        setIsDragging(false);
        setLastTouchDistance(getTouchDistance(e.touches));
      }
    },
    [pan, getTouchDistance],
  );

  const handleTouchMove = useCallback(
    (e: TouchEvent) => {
      if (e.touches.length === 1 && isDragging) {
        const touch = e.touches[0];
        setPan({
          x: touch.clientX - dragStart.x,
          y: touch.clientY - dragStart.y,
        });
      } else if (e.touches.length === 2 && lastTouchDistance) {
        // Two fingers - pinch zoom with throttling
        const currentDistance = getTouchDistance(e.touches);
        const scale = currentDistance / lastTouchDistance;

        if (Math.abs(scale - 1.0) > 0.02) {
          // Only update if significant change
          setZoom((prev) => Math.max(0.1, Math.min(3, prev * scale)));
          setLastTouchDistance(currentDistance);
        }
      }
    },
    [isDragging, dragStart, lastTouchDistance, getTouchDistance],
  );

  const handleTouchEnd = useCallback(() => {
    setIsDragging(false);
    setLastTouchDistance(null);
  }, []);

  // Register touch event listeners as non-passive
  useEffect(() => {
    const element = containerRef.current;
    if (!element) return;

    element.addEventListener("touchstart", handleTouchStart);
    element.addEventListener("touchmove", handleTouchMove);
    element.addEventListener("touchend", handleTouchEnd);

    return () => {
      element.removeEventListener("touchstart", handleTouchStart);
      element.removeEventListener("touchmove", handleTouchMove);
      element.removeEventListener("touchend", handleTouchEnd);
    };
  }, [handleTouchStart, handleTouchMove, handleTouchEnd]);

  useEffect(() => {
    resetView();
  }, [resetView]);

  // Cleanup animation frames on unmount
  useEffect(() => {
    return () => {
      if (animationFrameRef.current) {
        cancelAnimationFrame(animationFrameRef.current);
      }
    };
  }, []);

  const panHandlers = {
    onMouseDown: handleMouseDown,
    onMouseMove: handleMouseMove,
    onMouseUp: handleMouseUp,
    onMouseLeave: handleMouseUp,
  };

  return {
    zoom,
    pan,
    setPan,
    setZoom,
    containerRef,
    mapRef,
    zoomIn,
    zoomOut,
    resetView,
    panHandlers,
  };
}
