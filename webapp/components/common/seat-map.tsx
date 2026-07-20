"use client";

import React from "react";

import type { ReactElement } from "react";

import { cn } from "@/lib/utils";
import type {
  AreaDto,
  CoordinateDto,
  EventLocationMakerDto,
  SeatDto,
  SeatStatusDto,
  SupervisorSeatStatusDto,
} from "@/api";
// Wichtig: 'useLayoutEffect' importieren
import {
  useState,
  useRef,
  useCallback,
  useEffect,
  useMemo,
  useLayoutEffect,
} from "react";
import { useT } from "@/lib/i18n/hooks";
import { findSeatStatus, isSupervisorSeatStatus } from "@/lib/reservationSeat";
import { getAreaColor } from "@/lib/areaColors";

// Shared grid geometry, used both for placing markers/area zones and for
// sizing the seat grid itself.
const SEAT_SIZE = 32;
const GAP = 4;
const PADDING = 16;
const CELL_TOTAL_SIZE = SEAT_SIZE + GAP;
const ZONE_INSET = 6;

interface SeatMapProps {
  seats: SeatDto[];
  seatStatuses: SeatStatusDto[] | SupervisorSeatStatusDto[];
  markers: EventLocationMakerDto[];
  areas?: AreaDto[];
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
    showSeatNumber,
    onSeatSelect,
  }: {
    seat: SeatDto | undefined;
    seatColor: string;
    clickable: boolean;
    showSeatNumber: boolean;
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
          "w-8 h-8 flex items-center justify-center text-xs font-medium relative z-10",
          clickable && "cursor-pointer",
        )}
        onClick={handleClick}
        title={t("seatMap.seatTitle", {
          seatNumber: seat.seatNumber,
          seatRow: seat.seatRow,
        })}
      >
        <div
          className={cn(
            "w-full h-full rounded-full flex items-center justify-center text-white text-xs font-medium",
            seatColor,
          )}
        >
          {showSeatNumber ? seat.seatNumber : ""}
        </div>
      </div>
    );
  },
  (prevProps, nextProps) => {
    return (
      prevProps.seat?.id === nextProps.seat?.id &&
      prevProps.seatColor === nextProps.seatColor &&
      prevProps.clickable === nextProps.clickable &&
      prevProps.showSeatNumber === nextProps.showSeatNumber &&
      prevProps.onSeatSelect === nextProps.onSeatSelect
    );
  },
);

SeatComponent.displayName = "SeatComponent";

const MarkerComponent = React.memo(
  ({
    marker,
    showLabel,
  }: {
    marker: EventLocationMakerDto;
    showLabel: boolean;
  }) => {
    const containerRef = useRef<HTMLDivElement>(null);
    const textRef = useRef<HTMLSpanElement>(null);

    useLayoutEffect(() => {
      if (containerRef.current && textRef.current && showLabel) {
        const textEl = textRef.current;
        const containerEl = containerRef.current;

        // Reset styles for accurate measurement
        textEl.style.transform = "scale(1)";

        const textWidth = textEl.scrollWidth;
        const HORIZONTAL_PADDING = 8; // 4px padding left & right

        let finalWidth = textWidth + HORIZONTAL_PADDING;
        let textScale = 1;

        // If the marker would become wider than a seat, cap its width and scale the text
        if (finalWidth > SEAT_SIZE) {
          finalWidth = SEAT_SIZE;
          textScale = (SEAT_SIZE - HORIZONTAL_PADDING) / textWidth;
        }

        // --- Centering Logic ---
        // Calculate the original starting position of the grid cell
        const cellLeft =
          PADDING +
          ((marker.coordinate?.xCoordinate ?? 1) - 1) * CELL_TOTAL_SIZE;
        // Adjust the left position to center the new, smaller width within the cell
        const newLeft = cellLeft + (SEAT_SIZE - finalWidth) / 2;

        // Apply all the new styles
        containerEl.style.width = `${finalWidth}px`;
        containerEl.style.left = `${newLeft}px`;
        textEl.style.transform = `scale(${textScale})`;
      }
    }, [marker.label, marker.coordinate?.xCoordinate, showLabel]);

    return (
      <div
        ref={containerRef}
        className="absolute z-0 flex items-center justify-center font-bold text-gray-800 dark:text-gray-200 rounded-md overflow-hidden"
        style={{
          // Initial position and size before dynamic adjustment
          left: `${PADDING + ((marker.coordinate?.xCoordinate ?? 1) - 1) * CELL_TOTAL_SIZE}px`,
          top: `${PADDING + ((marker.coordinate?.yCoordinate ?? 1) - 1) * CELL_TOTAL_SIZE}px`,
          width: `${SEAT_SIZE}px`,
          height: `${SEAT_SIZE}px`,
          fontSize: "14px",
          transition: "width 0.2s ease, left 0.2s ease", // Optional: smooth transition
        }}
        title={marker.label || ""}
      >
        {showLabel && (
          <span
            ref={textRef}
            style={{ whiteSpace: "nowrap", display: "inline-block" }}
          >
            {marker.label}
          </span>
        )}
      </div>
    );
  },
);

MarkerComponent.displayName = "MarkerComponent";

interface AreaRectZone {
  shape: "rect";
  key: string;
  name: string;
  left: number;
  top: number;
  width: number;
  height: number;
  colorIndex: number;
}

interface AreaPolygonZone {
  shape: "polygon";
  key: string;
  name: string;
  left: number;
  top: number;
  width: number;
  height: number;
  // Points relative to (left, top), as an SVG `points` attribute value.
  pointsAttr: string;
  colorIndex: number;
}

type AreaZone = AreaRectZone | AreaPolygonZone;

const AreaZoneLabel = ({
  name,
  textClass,
}: {
  name: string;
  textClass: string;
}) => (
  <span
    className={cn(
      "absolute -top-3 left-2 px-1.5 rounded-sm bg-seatmap text-[10px] font-semibold whitespace-nowrap",
      textClass,
    )}
  >
    {name}
  </span>
);

const AreaZoneComponent = React.memo(({ zone }: { zone: AreaRectZone }) => {
  const color = getAreaColor(zone.colorIndex);

  return (
    <div
      className={cn(
        "absolute rounded-lg border-2 border-dashed pointer-events-none",
        color.fill,
        color.border,
      )}
      style={{
        left: `${zone.left}px`,
        top: `${zone.top}px`,
        width: `${zone.width}px`,
        height: `${zone.height}px`,
      }}
    >
      <AreaZoneLabel name={zone.name} textClass={color.text} />
    </div>
  );
});

AreaZoneComponent.displayName = "AreaZoneComponent";

// Renders a custom area boundary polygon, used instead of AreaZoneComponent
// when the API supplies explicit boundary points for an area.
const AreaPolygonZoneComponent = React.memo(
  ({ zone }: { zone: AreaPolygonZone }) => {
    const color = getAreaColor(zone.colorIndex);

    return (
      <div
        className="absolute pointer-events-none"
        style={{
          left: `${zone.left}px`,
          top: `${zone.top}px`,
          width: `${zone.width}px`,
          height: `${zone.height}px`,
        }}
      >
        <svg
          width="100%"
          height="100%"
          style={{ overflow: "visible" }}
          preserveAspectRatio="none"
        >
          <polygon
            points={zone.pointsAttr}
            fill={color.hex}
            fillOpacity={0.12}
            stroke={color.hex}
            strokeOpacity={0.7}
            strokeWidth={2}
            strokeDasharray="6 4"
            strokeLinejoin="round"
          />
        </svg>
        <AreaZoneLabel name={zone.name} textClass={color.text} />
      </div>
    );
  },
);

AreaPolygonZoneComponent.displayName = "AreaPolygonZoneComponent";

export function SeatMap({
  seats,
  seatStatuses,
  markers,
  areas = [],
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

  const mapRef = useRef<HTMLDivElement>(null);
  const animationFrameRef = useRef<number | null>(null);

  const {
    maxX,
    maxY,
    seatPositionMap,
    selectedSeatIds,
    userReservedSeatIds,
    renderedMarkers,
    areaZones,
  } = useMemo(() => {
    const seatMaxX = Math.max(
      ...seats.map((s) => s.coordinate?.xCoordinate || 0),
    );
    const seatMaxY = Math.max(
      ...seats.map((s) => s.coordinate?.yCoordinate || 0),
    );
    const markerMaxX = Math.max(
      ...markers.map((m) => m.coordinate?.xCoordinate || 0),
    );
    const markerMaxY = Math.max(
      ...markers.map((m) => m.coordinate?.yCoordinate || 0),
    );
    // A custom area boundary polygon (see below) may intentionally extend past the
    // outermost seats (e.g. a rounded balcony edge) - include it so the grid container
    // is sized to fit it instead of clipping it via the map's `overflow-hidden` wrapper.
    const areaBoundaryPoints = areas.flatMap((area) => area.boundary ?? []);
    const areaMaxX = Math.max(
      0,
      ...areaBoundaryPoints.map((p) => p.xCoordinate || 0),
    );
    const areaMaxY = Math.max(
      0,
      ...areaBoundaryPoints.map((p) => p.yCoordinate || 0),
    );

    const maxX = Math.max(seatMaxX, markerMaxX, areaMaxX);
    const maxY = Math.max(seatMaxY, markerMaxY, areaMaxY);

    // Create a map for O(1) seat lookup
    const seatPositionMap = new Map<string, SeatDto>();
    const seatById = new Map<bigint, SeatDto>();
    seats.forEach((seat) => {
      if (
        seat.coordinate?.xCoordinate != null &&
        seat.coordinate?.yCoordinate != null
      ) {
        seatPositionMap.set(
          `${seat.coordinate.xCoordinate}-${seat.coordinate.yCoordinate}`,
          seat,
        );
      }
      if (seat.id !== undefined) {
        seatById.set(seat.id, seat);
      }
    });

    // Create a Set for O(1) selected seat lookup
    const selectedSeatIds = new Set(selectedSeats.map((s) => s.id));

    const userReservedSeatIds = new Set(userReservedSeats.map((s) => s.id));

    // Filter markers with valid coordinates
    const renderedMarkers = markers.filter(
      (marker) =>
        marker.coordinate?.xCoordinate != null &&
        marker.coordinate?.yCoordinate != null,
    );

    // Each area is rendered either from custom boundary points (when the API
    // supplies at least 3 - a valid polygon) or, failing that, as a
    // bounding-box derived from its member seats' coordinates. Areas are
    // usually contiguous blocks (e.g. "Parkett", "Balkon"), so a rectangle is
    // a good enough default shape without needing a more elaborate one.
    const areaZones: AreaZone[] = areas.flatMap((area, index): AreaZone[] => {
      const key = area.name ?? `area-${index}`;

      const validBoundaryPoints = (area.boundary ?? []).filter(
        (p): p is Required<CoordinateDto> =>
          p.xCoordinate != null && p.yCoordinate != null,
      );

      if (validBoundaryPoints.length >= 3) {
        // Anchor each boundary point to the center of the referenced grid
        // cell, then push it outward from the polygon's centroid so the
        // outline doesn't just clip through the seats it encloses.
        const rawPoints = validBoundaryPoints.map((p) => ({
          x: PADDING + (p.xCoordinate - 1) * CELL_TOTAL_SIZE + SEAT_SIZE / 2,
          y: PADDING + (p.yCoordinate - 1) * CELL_TOTAL_SIZE + SEAT_SIZE / 2,
        }));
        const centroidX =
          rawPoints.reduce((sum, p) => sum + p.x, 0) / rawPoints.length;
        const centroidY =
          rawPoints.reduce((sum, p) => sum + p.y, 0) / rawPoints.length;
        const inflatedPoints = rawPoints.map((p) => {
          const dx = p.x - centroidX;
          const dy = p.y - centroidY;
          const len = Math.hypot(dx, dy) || 1;
          return {
            x: p.x + (dx / len) * ZONE_INSET,
            y: p.y + (dy / len) * ZONE_INSET,
          };
        });

        const left = Math.min(...inflatedPoints.map((p) => p.x));
        const top = Math.min(...inflatedPoints.map((p) => p.y));
        const width = Math.max(...inflatedPoints.map((p) => p.x)) - left;
        const height = Math.max(...inflatedPoints.map((p) => p.y)) - top;

        return [
          {
            shape: "polygon" as const,
            key,
            name: area.name ?? "",
            left,
            top,
            width,
            height,
            pointsAttr: inflatedPoints
              .map((p) => `${p.x - left},${p.y - top}`)
              .join(" "),
            colorIndex: index,
          },
        ];
      }

      const memberSeats = (area.seatIds ?? [])
        .map((id) => seatById.get(id))
        .filter(
          (s): s is SeatDto =>
            !!s &&
            s.coordinate?.xCoordinate != null &&
            s.coordinate?.yCoordinate != null,
        );
      if (memberSeats.length === 0) return [];

      const xs = memberSeats.map((s) => s.coordinate!.xCoordinate!);
      const ys = memberSeats.map((s) => s.coordinate!.yCoordinate!);
      const minX = Math.min(...xs);
      const maxAreaX = Math.max(...xs);
      const minY = Math.min(...ys);
      const maxAreaY = Math.max(...ys);

      return [
        {
          shape: "rect" as const,
          key,
          name: area.name ?? "",
          left: PADDING + (minX - 1) * CELL_TOTAL_SIZE - ZONE_INSET,
          top: PADDING + (minY - 1) * CELL_TOTAL_SIZE - ZONE_INSET,
          width:
            (maxAreaX - minX + 1) * SEAT_SIZE +
            (maxAreaX - minX) * GAP +
            ZONE_INSET * 2,
          height:
            (maxAreaY - minY + 1) * SEAT_SIZE +
            (maxAreaY - minY) * GAP +
            ZONE_INSET * 2,
          colorIndex: index,
        },
      ];
    });

    return {
      maxX,
      maxY,
      seatPositionMap,
      selectedSeatIds,
      userReservedSeatIds,
      renderedMarkers,
      areaZones,
    };
  }, [seats, selectedSeats, userReservedSeats, markers, areas]);

  const getSeatColor = useCallback(
    (seat: SeatDto | undefined) => {
      if (!seat) return "transparent";

      const isSelected = selectedSeatIds.has(seat.id);
      if (isSelected) return "bg-blue-500 dark:bg-blue-600";

      const isUserReserved = userReservedSeatIds.has(seat.id);
      if (isUserReserved) return "bg-yellow-500 dark:bg-yellow-600";

      // Check if we're working with SupervisorSeatStatusDto (has liveStatus)
      if (seatStatuses.length > 0 && isSupervisorSeatStatus(seatStatuses[0])) {
        // Handle SupervisorSeatStatusDto
        const supervisorStatus = (
          seatStatuses as SupervisorSeatStatusDto[]
        ).find((s) => s.seatId === seat.id);

        if (supervisorStatus) {
          // If status is RESERVED and has live status, show live status color
          if (
            supervisorStatus.status === "RESERVED" &&
            supervisorStatus.liveStatus
          ) {
            switch (supervisorStatus.liveStatus) {
              case "CHECKED_IN":
                return "bg-yellow-300 dark:bg-yellow-600";
              case "CANCELLED":
                return "bg-violet-500 dark:bg-violet-500";
              case "NO_SHOW":
                return "bg-orange-500 dark:bg-orange-600";
              default:
                return "bg-red-500 dark:bg-red-600";
            }
          }

          // Otherwise use regular status
          switch (supervisorStatus.status) {
            case "RESERVED":
              return "bg-red-500 dark:bg-red-600";
            case "BLOCKED":
              return "bg-gray-500 dark:bg-gray-600";
            default:
              return "bg-green-500 dark:bg-green-600";
          }
        }

        return "bg-green-500 dark:bg-green-600";
      } else {
        // Handle regular SeatStatusDto
        const seatStatus = findSeatStatus(
          seat.id,
          seatStatuses as SeatStatusDto[],
        );

        switch (seatStatus) {
          case "RESERVED":
            return "bg-red-500 dark:bg-red-600";
          case "BLOCKED":
            return "bg-gray-500 dark:bg-gray-600";
          default:
            return "bg-green-500 dark:bg-green-600";
        }
      }
    },
    [selectedSeatIds, userReservedSeatIds, seatStatuses],
  );

  const canSelectSeat = useCallback(
    (seat: SeatDto | undefined) => {
      if (!seat || readonly) return false;

      const isUserReserved = userReservedSeatIds.has(seat.id);
      if (isUserReserved) return true;

      // Check if we're working with SupervisorSeatStatusDto
      if (seatStatuses.length > 0 && isSupervisorSeatStatus(seatStatuses[0])) {
        // Handle SupervisorSeatStatusDto - can only select seats without status
        const supervisorStatus = (
          seatStatuses as SupervisorSeatStatusDto[]
        ).find((s) => s.seatId === seat.id);
        return !supervisorStatus; // Can only select seats without status (available)
      } else {
        // Handle regular SeatStatusDto
        const seatStatus = findSeatStatus(
          seat.id,
          seatStatuses as SeatStatusDto[],
        );
        return !seatStatus; // Can only select seats without status (available)
      }
    },
    [readonly, userReservedSeatIds, seatStatuses],
  );

  const gridStructure = useMemo(() => {
    return Array.from({ length: maxY }, (_, y) =>
      Array.from({ length: maxX }, (_, x) => {
        const seat = seatPositionMap.get(`${x + 1}-${y + 1}`);
        const seatColor = getSeatColor(seat);
        const clickable = canSelectSeat(seat);

        return {
          key: `${x}-${y}`,
          seat,
          seatColor,
          clickable,
        };
      }),
    ).flat();
  }, [maxX, maxY, seatPositionMap, getSeatColor, canSelectSeat]);

  const displayFlags = useMemo(
    () => ({
      showSeatNumber: zoom > 0.6,
    }),
    [zoom],
  );

  const gridItems = useMemo(() => {
    return gridStructure.map(({ key, seat, seatColor, clickable }) => (
      <SeatComponent
        key={key}
        seat={seat}
        seatColor={seatColor}
        clickable={clickable}
        showSeatNumber={displayFlags.showSeatNumber}
        onSeatSelect={onSeatSelect}
      />
    ));
  }, [gridStructure, displayFlags.showSeatNumber, onSeatSelect]);

  const wheelRef = useRef<HTMLDivElement>(null);

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
    const element = wheelRef.current;
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
    if (wheelRef.current && maxX > 0 && maxY > 0) {
      const container = wheelRef.current;
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
    const element = wheelRef.current;
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

  return (
    <div className="relative w-full h-full rounded-lg overflow-hidden">
      <div className="absolute top-2 right-2 z-10 flex gap-2">
        <button
          onClick={zoomIn}
          className="px-2 py-1 bg-seatmap border rounded shadow-xs hover:bg-secondary text-sm dark:text-white"
        >
          +
        </button>
        <button
          onClick={zoomOut}
          className="px-2 py-1 bg-seatmap border rounded shadow-xs hover:bg-secondary text-sm dark:text-white"
        >
          -
        </button>
        <button
          onClick={resetView}
          className="px-2 py-1 bg-seatmap border rounded shadow-xs hover:bg-secondary text-sm dark:text-white"
        >
          {t("seatMap.resetButton")}
        </button>
      </div>

      <div
        ref={wheelRef}
        className="w-full h-full p-4 pt-16 cursor-grab active:cursor-grabbing flex items-center justify-center"
        onMouseDown={handleMouseDown}
        onMouseMove={handleMouseMove}
        onMouseUp={handleMouseUp}
        onMouseLeave={handleMouseUp}
        style={{
          touchAction: "none",
          willChange: "transform",
        }}
      >
        <div
          ref={mapRef}
          style={{
            transform: `scale(${zoom}) translate3d(${pan.x / zoom}px, ${
              pan.y / zoom
            }px, 0)`,
            transformOrigin: "center center",
            willChange: "transform",
            backfaceVisibility: "hidden",
          }}
        >
          <div
            className="border-2 border rounded-lg mb-0 bg-seatmap"
            style={{
              width: `${maxX * SEAT_SIZE + (maxX - 1) * GAP + PADDING * 2}px`,
              height: "120px",
            }}
          >
            <div className="w-full h-full flex items-center justify-center text-gray-800 dark:text-gray-200 text-3xl font-bold">
              {t("seatMap.stageText")}
            </div>
          </div>

          <div
            className="border-2 border rounded-lg p-4 bg-seatmap relative"
            style={{
              width: `${maxX * SEAT_SIZE + (maxX - 1) * GAP + PADDING * 2}px`,
              height: `${maxY * SEAT_SIZE + (maxY - 1) * GAP + PADDING * 2}px`,
            }}
          >
            {/* Area Zone Layer - ganz im Hintergrund */}
            {areaZones.map((zone) =>
              zone.shape === "polygon" ? (
                <AreaPolygonZoneComponent key={zone.key} zone={zone} />
              ) : (
                <AreaZoneComponent key={zone.key} zone={zone} />
              ),
            )}

            {/* Marker Layer - Hintergrund */}
            {renderedMarkers.map((marker, index) => (
              <MarkerComponent
                key={`marker-${index}`}
                marker={marker}
                showLabel={true}
              />
            ))}

            {/* Sitzplatz Layer - Vordergrund */}
            <div
              className="grid gap-1 relative z-10"
              style={{
                gridTemplateColumns: `repeat(${maxX}, 1fr)`,
                width: `${maxX * SEAT_SIZE + (maxX - 1) * GAP}px`,
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
