"use client";

import React, { useRef, useCallback, useMemo, useLayoutEffect } from "react";
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
import { useT } from "@/lib/i18n/hooks";
import { findSeatStatus, isSupervisorSeatStatus } from "@/lib/reservationSeat";
import { getAreaColor } from "@/lib/areaColors";
import { SEAT_STATUS_BG, getSeatVisualStatus } from "@/lib/seatStatusStyles";
import {
  SEAT_SIZE,
  ZONE_INSET,
  cellToPx,
  mapPxSize,
  gridContentPxSize,
  boundaryToPixelPolygon,
} from "@/components/common/seat-map-geometry";
import { useMapViewport } from "@/components/common/use-map-viewport";

interface SeatMapProps {
  seats: SeatDto[];
  seatStatuses: SeatStatusDto[] | SupervisorSeatStatusDto[];
  markers: EventLocationMakerDto[];
  areas?: AreaDto[];
  selectedSeats: SeatDto[];
  userReservedSeats?: SeatDto[];
  highlightedSeatId?: string | null;
  onSeatSelect: (seat: SeatDto) => void;
  readonly?: boolean;
}

const SeatComponent = React.memo(
  ({
    seat,
    seatColor,
    clickable,
    highlighted,
    showSeatNumber,
    onSeatSelect,
  }: {
    seat: SeatDto | undefined;
    seatColor: string;
    clickable: boolean;
    highlighted: boolean;
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
          highlighted &&
            "rounded-full ring-4 ring-primary ring-offset-2 ring-offset-seatmap animate-pulse",
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
      prevProps.highlighted === nextProps.highlighted &&
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
        const cellLeft = cellToPx(marker.coordinate?.xCoordinate ?? 1);
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
          left: `${cellToPx(marker.coordinate?.xCoordinate ?? 1)}px`,
          top: `${cellToPx(marker.coordinate?.yCoordinate ?? 1)}px`,
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
  // Where to place the name label - the polygon's own topmost vertex, not
  // the bounding box's corner (see boundaryToPixelPolygon).
  labelAnchor: { x: number; y: number };
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
        <span
          className={cn(
            "absolute -translate-x-1/2 -translate-y-full px-1.5 rounded-sm bg-seatmap text-[10px] font-semibold whitespace-nowrap",
            color.text,
          )}
          style={{
            left: `${zone.labelAnchor.x}px`,
            top: `${zone.labelAnchor.y - 4}px`,
          }}
        >
          {zone.name}
        </span>
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
  highlightedSeatId = null,
  onSeatSelect,
  readonly = false,
}: SeatMapProps): ReactElement {
  const t = useT();

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
    const seatById = new Map<string, SeatDto>();
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
      // Prefer the id: area names are not guaranteed to be unique within a location.
      const key =
        area.id != null ? `area-${area.id}` : (area.name ?? `area-${index}`);

      const validBoundaryPoints = (area.boundary ?? []).filter(
        (p): p is Required<CoordinateDto> =>
          p.xCoordinate != null && p.yCoordinate != null,
      );

      if (validBoundaryPoints.length >= 3) {
        // Anchor each boundary point to the center of the referenced grid
        // cell, then push it outward from the polygon's centroid so the
        // outline doesn't just clip through the seats it encloses.
        const { left, top, width, height, pointsAttr, labelAnchor } =
          boundaryToPixelPolygon(validBoundaryPoints);

        return [
          {
            shape: "polygon" as const,
            key,
            name: area.name ?? "",
            left,
            top,
            width,
            height,
            pointsAttr,
            labelAnchor,
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
          left: cellToPx(minX) - ZONE_INSET,
          top: cellToPx(minY) - ZONE_INSET,
          width: gridContentPxSize(maxAreaX - minX + 1) + ZONE_INSET * 2,
          height: gridContentPxSize(maxAreaY - minY + 1) + ZONE_INSET * 2,
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

  const {
    zoom,
    pan,
    containerRef,
    mapRef,
    zoomIn,
    zoomOut,
    resetView,
    panHandlers,
  } = useMapViewport(maxX, maxY);

  const getSeatColor = useCallback(
    (seat: SeatDto | undefined) => {
      if (!seat) return "transparent";

      const isSelected = selectedSeatIds.has(seat.id);
      if (isSelected) return SEAT_STATUS_BG.SELECTED;

      const isUserReserved = userReservedSeatIds.has(seat.id);
      if (isUserReserved) return SEAT_STATUS_BG.USER_RESERVED;

      // Check if we're working with SupervisorSeatStatusDto (has liveStatus)
      if (seatStatuses.length > 0 && isSupervisorSeatStatus(seatStatuses[0])) {
        // Handle SupervisorSeatStatusDto
        const supervisorStatus = (
          seatStatuses as SupervisorSeatStatusDto[]
        ).find((s) => s.seatId === seat.id);

        return SEAT_STATUS_BG[
          getSeatVisualStatus(
            supervisorStatus?.status,
            supervisorStatus?.liveStatus,
          )
        ];
      } else {
        // Handle regular SeatStatusDto
        const seatStatus = findSeatStatus(
          seat.id,
          seatStatuses as SeatStatusDto[],
        );

        return SEAT_STATUS_BG[getSeatVisualStatus(seatStatus)];
      }
    },
    [selectedSeatIds, userReservedSeatIds, seatStatuses],
  );

  const canSelectSeat = useCallback(
    (seat: SeatDto | undefined) => {
      if (!seat || readonly) return false;

      // Own selection stays clickable (to deselect), even if refetched as e.g. PENDING -
      // mirrors getSeatColor's isSelected priority.
      if (selectedSeatIds.has(seat.id)) return true;

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
    [readonly, selectedSeatIds, userReservedSeatIds, seatStatuses],
  );

  const gridStructure = useMemo(() => {
    return Array.from({ length: maxY }, (_, y) =>
      Array.from({ length: maxX }, (_, x) => {
        const seat = seatPositionMap.get(`${x + 1}-${y + 1}`);
        const seatColor = getSeatColor(seat);
        const clickable = canSelectSeat(seat);
        const highlighted = !!seat && seat.id === highlightedSeatId;

        return {
          key: `${x}-${y}`,
          seat,
          seatColor,
          clickable,
          highlighted,
        };
      }),
    ).flat();
  }, [
    maxX,
    maxY,
    seatPositionMap,
    getSeatColor,
    canSelectSeat,
    highlightedSeatId,
  ]);

  const displayFlags = useMemo(
    () => ({
      showSeatNumber: zoom > 0.6,
    }),
    [zoom],
  );

  const gridItems = useMemo(() => {
    return gridStructure.map(
      ({ key, seat, seatColor, clickable, highlighted }) => (
        <SeatComponent
          key={key}
          seat={seat}
          seatColor={seatColor}
          clickable={clickable}
          highlighted={highlighted}
          showSeatNumber={displayFlags.showSeatNumber}
          onSeatSelect={onSeatSelect}
        />
      ),
    );
  }, [gridStructure, displayFlags.showSeatNumber, onSeatSelect]);

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
        ref={containerRef}
        className="w-full h-full p-4 pt-16 cursor-grab active:cursor-grabbing flex items-center justify-center"
        {...panHandlers}
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
              width: `${mapPxSize(maxX)}px`,
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
              width: `${mapPxSize(maxX)}px`,
              height: `${mapPxSize(maxY)}px`,
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
                width: `${gridContentPxSize(maxX)}px`,
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
