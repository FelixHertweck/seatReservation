"use client";

import React from "react";

import type { ReactElement } from "react";

import { cn } from "@/lib/utils";
import type { SeatDto } from "@/api";
import { useState, useRef, useCallback, useEffect, useMemo } from "react";
import { useT } from "@/lib/i18n/hooks";

interface SeatMapProps {
  seats: SeatDto[];
  selectedSeats: SeatDto[];
  userReservedSeats?: SeatDto[];
  onSeatSelect: (seat: SeatDto) => void;
  readonly?: boolean;
}

const SeatComponent = React.memo(
  ({
    seat,
    seatColor,
    clickable,
    zoom,
    onSeatSelect,
  }: {
    seat: SeatDto | undefined;
    seatColor: string;
    clickable: boolean;
    zoom: number;
    onSeatSelect: (seat: SeatDto) => void;
  }) => {
    const t = useT();

    const handleClick = useCallback(
      (e: React.MouseEvent) => {
        e.stopPropagation();
        if (seat && clickable) onSeatSelect(seat);
      },
      [seat, clickable, onSeatSelect],
    );

    if (!seat) return <div className="w-8 h-8" />;

    return (
      <div
        className={cn(
          "w-8 h-8 flex items-center justify-center text-xs font-medium transition-colors relative",
          clickable && "cursor-pointer",
        )}
        onClick={handleClick}
        title={t("seatMap.seatTitle", { seatNumber: seat.seatNumber })}
      >
        <div
          className={cn(
            "w-full h-full rounded-full transition-all duration-200 hover:scale-105 flex items-center justify-center text-white text-xs font-medium drop-shadow-sm",
            seatColor,
          )}
        >
          {zoom > 0.8 ? seat.seatNumber : ""}
        </div>
      </div>
    );
  },
);

SeatComponent.displayName = "SeatComponent";

export function SeatMap({
  seats,
  selectedSeats,
  userReservedSeats = [],
  onSeatSelect,
  readonly = false,
}: SeatMapProps): ReactElement {
  const t = useT();

  const [zoom, setZoom] = useState(1);
  const [pan, setPan] = useState({ x: 0, y: 0 });
  const [isDragging, setIsDragging] = useState(false);
  const [dragStart, setDragStart] = useState({ x: 0, y: 0 });
  const [lastTouchDistance, setLastTouchDistance] = useState<number | null>(
    null,
  );
  const containerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<HTMLDivElement>(null);

  const { maxX, maxY, seatPositionMap, selectedSeatIds, userReservedSeatIds } =
    useMemo(() => {
      const maxX = Math.max(...seats.map((s) => s.xCoordinate || 0));
      const maxY = Math.max(...seats.map((s) => s.yCoordinate || 0));

      // Create a map for O(1) seat lookup
      const seatPositionMap = new Map<string, SeatDto>();
      seats.forEach((seat) => {
        if (seat.xCoordinate && seat.yCoordinate) {
          seatPositionMap.set(`${seat.xCoordinate}-${seat.yCoordinate}`, seat);
        }
      });

      // Create a Set for O(1) selected seat lookup
      const selectedSeatIds = new Set(selectedSeats.map((s) => s.id));

      const userReservedSeatIds = new Set(userReservedSeats.map((s) => s.id));

      return {
        maxX,
        maxY,
        seatPositionMap,
        selectedSeatIds,
        userReservedSeatIds,
      };
    }, [seats, selectedSeats, userReservedSeats]);

  const getSeatColor = useCallback(
    (seat: SeatDto | undefined) => {
      if (!seat) return "transparent";

      const isSelected = selectedSeatIds.has(seat.id);
      if (isSelected)
        return "bg-blue-500 hover:bg-blue-600 dark:bg-blue-600 dark:hover:bg-blue-700";

      const isUserReserved = userReservedSeatIds.has(seat.id);
      if (isUserReserved)
        return "bg-yellow-500 hover:bg-yellow-600 dark:bg-yellow-600 dark:hover:bg-yellow-700";

      switch (seat.status) {
        case "RESERVED":
          return "bg-red-500 dark:bg-red-600";
        case "BLOCKED":
          return "bg-gray-500 dark:bg-gray-600";
        default:
          return "bg-green-500 hover:bg-green-600 dark:bg-green-600 dark:hover:bg-green-700";
      }
    },
    [selectedSeatIds, userReservedSeatIds],
  );

  const canSelectSeat = useCallback(
    (seat: SeatDto | undefined) => {
      if (!seat || readonly) return false;
      const isUserReserved = userReservedSeatIds.has(seat.id);
      if (isUserReserved) return true;
      return !seat.status; // Can only select seats without status (available)
    },
    [readonly, userReservedSeatIds],
  );

  const gridItems = useMemo(() => {
    return Array.from({ length: maxY }, (_, y) =>
      Array.from({ length: maxX }, (_, x) => {
        const seat = seatPositionMap.get(`${x + 1}-${y + 1}`);
        const seatColor = getSeatColor(seat);
        const clickable = canSelectSeat(seat);

        return (
          <SeatComponent
            key={`${x}-${y}`}
            seat={seat}
            seatColor={seatColor}
            clickable={clickable}
            zoom={zoom}
            onSeatSelect={onSeatSelect}
          />
        );
      }),
    ).flat();
  }, [
    maxX,
    maxY,
    seatPositionMap,
    getSeatColor,
    canSelectSeat,
    zoom,
    onSeatSelect,
  ]);

  const handleWheel = useCallback((e: React.WheelEvent) => {
    e.preventDefault();
    const delta = e.deltaY > 0 ? 0.9 : 1.1;
    setZoom((prev) => Math.max(0.1, Math.min(3, prev * delta)));
  }, []);

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

      const seatSize = 32;
      const gap = 4;
      const borderPadding = 16;

      const requiredWidth =
        maxX * seatSize + (maxX - 1) * gap + borderPadding * 2;
      const requiredHeight =
        maxY * seatSize + (maxY - 1) * gap + borderPadding * 2;

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
    (e: React.TouchEvent) => {
      e.preventDefault();

      if (e.touches.length === 1) {
        // Single finger - start panning
        const touch = e.touches[0];
        setIsDragging(true);
        setDragStart({ x: touch.clientX - pan.x, y: touch.clientY - pan.y });
        setLastTouchDistance(null);
      } else if (e.touches.length === 2) {
        // Two fingers - start pinch zoom
        setIsDragging(false);
        setLastTouchDistance(
          getTouchDistance(e.touches as unknown as TouchList),
        );
      }
    },
    [pan, getTouchDistance],
  );

  const handleTouchMove = useCallback(
    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    (e: React.TouchEvent) => {
      e.preventDefault();

      if (e.touches.length === 1 && isDragging) {
        // Single finger - pan
        const touch = e.touches[0];
        setPan({
          x: touch.clientX - dragStart.x,
          y: touch.clientY - dragStart.y,
        });
      } else if (e.touches.length === 2 && lastTouchDistance) {
        // Two fingers - pinch zoom
        const currentDistance = getTouchDistance(
          e.touches as unknown as TouchList,
        );
        const scale = currentDistance / lastTouchDistance;

        setZoom((prev) => Math.max(0.1, Math.min(3, prev * scale)));
        setLastTouchDistance(currentDistance);
      }
    },
    [isDragging, dragStart, lastTouchDistance, getTouchDistance],
  );

  const handleTouchEnd = useCallback(() => {
    setIsDragging(false);
    setLastTouchDistance(null);
  }, []);

  useEffect(() => {
    resetView();
  }, [resetView]);

  return (
    <div className="relative w-full h-full bg-gray-50 dark:bg-gray-900 rounded-lg overflow-hidden">
      <div className="absolute top-2 right-2 z-10 flex gap-2">
        <button
          onClick={zoomIn}
          className="px-2 py-1 bg-white dark:bg-gray-800 border dark:border-gray-700 rounded shadow-sm hover:bg-gray-50 dark:hover:bg-gray-700 text-sm dark:text-white"
        >
          +
        </button>
        <button
          onClick={zoomOut}
          className="px-2 py-1 bg-white dark:bg-gray-800 border dark:border-gray-700 rounded shadow-sm hover:bg-gray-50 dark:hover:bg-gray-700 text-sm dark:text-white"
        >
          -
        </button>
        <button
          onClick={resetView}
          className="px-2 py-1 bg-white dark:bg-gray-800 border dark:border-gray-700 rounded shadow-sm hover:bg-gray-50 dark:hover:bg-gray-700 text-sm dark:text-white"
        >
          {t("seatMap.resetButton")}
        </button>
      </div>

      <div
        ref={containerRef}
        className="w-full h-full p-4 pt-16 cursor-grab active:cursor-grabbing flex items-center justify-center"
        onWheel={handleWheel}
        onMouseDown={handleMouseDown}
        onMouseMove={handleMouseMove}
        onMouseUp={handleMouseUp}
        onMouseLeave={handleMouseUp}
        onTouchStart={handleTouchStart}
        onTouchMove={handleTouchMove}
        onTouchEnd={handleTouchEnd}
        style={{ touchAction: "none" }}
      >
        <div
          ref={mapRef}
          style={{
            transform: `scale(${zoom}) translate(${pan.x / zoom}px, ${pan.y / zoom}px)`,
            transformOrigin: "center center",
            transition: isDragging ? "none" : "transform 0.1s ease-out",
          }}
        >
          <div
            className="border-2 border-gray-400 dark:border-gray-600 rounded-lg mb-0 bg-white/50 dark:bg-gray-800/50"
            style={{
              width: `${maxX * 32 + (maxX - 1) * 4 + 32}px`,
              height: "120px",
            }}
          >
            <div className="w-full h-full flex items-center justify-center text-gray-800 dark:text-gray-200 text-3xl font-bold">
              {t("seatMap.stageText")}
            </div>
          </div>

          <div
            className="border-2 border-gray-400 dark:border-gray-600 rounded-lg p-4 bg-white/50 dark:bg-gray-800/50"
            style={{
              width: `${maxX * 32 + (maxX - 1) * 4 + 32}px`,
              height: `${maxY * 32 + (maxY - 1) * 4 + 32}px`,
            }}
          >
            <div
              className="grid gap-1"
              style={{
                gridTemplateColumns: `repeat(${maxX}, 1fr)`,
                width: `${maxX * 32 + (maxX - 1) * 4}px`,
              }}
            >
              {gridItems}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
