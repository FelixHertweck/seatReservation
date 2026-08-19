"use client";

import React, { useRef, useCallback, useMemo, useLayoutEffect } from "react";
import type { ReactElement } from "react";
import { Armchair } from "lucide-react";

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
  isLoading?: boolean;
}

const SeatComponent = React.memo(
  ({
    seat,
    seatColor,
    clickable,
    highlighted,
    selected,
    showSeatNumber,
    popDelayMs,
    onSeatSelect,
  }: {
    seat: SeatDto | undefined;
    seatColor: string;
    clickable: boolean;
    highlighted: boolean;
    selected: boolean;
    showSeatNumber: boolean;
    popDelayMs: number;
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
          "w-8 h-8 flex items-center justify-center text-xs font-medium relative z-10 animate-seat-in transition-transform duration-200 ease-out",
          clickable && "cursor-pointer hover:scale-110 active:scale-90",
          selected && "scale-105",
          highlighted &&
            "rounded-full ring-4 ring-primary ring-offset-2 ring-offset-seatmap animate-pulse",
        )}
        style={{ animationDelay: `${popDelayMs}ms` }}
        onClick={handleClick}
        title={t("seatMap.seatTitle", {
          seatNumber: seat.seatNumber,
          seatRow: seat.seatRow,
        })}
      >
        <div
          className={cn(
            "w-full h-full rounded-full flex items-center justify-center text-white text-xs font-medium transition-colors duration-300 ease-out",
            seatColor,
            selected &&
              "ring-2 ring-white/80 dark:ring-white/50 shadow-md shadow-black/25",
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
      prevProps.selected === nextProps.selected &&
      prevProps.showSeatNumber === nextProps.showSeatNumber &&
      prevProps.popDelayMs === nextProps.popDelayMs &&
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

        textEl.style.transform = "scale(1)";

        const textWidth = textEl.scrollWidth;
        const HORIZONTAL_PADDING = 8;

        let finalWidth = textWidth + HORIZONTAL_PADDING;
        let textScale = 1;

        if (finalWidth > SEAT_SIZE) {
          finalWidth = SEAT_SIZE;
          textScale = (SEAT_SIZE - HORIZONTAL_PADDING) / textWidth;
        }

        const cellLeft = cellToPx(marker.coordinate?.xCoordinate ?? 1);
        const newLeft = cellLeft + (SEAT_SIZE - finalWidth) / 2;

        containerEl.style.width = `${finalWidth}px`;
        containerEl.style.left = `${newLeft}px`;
        textEl.style.transform = `scale(${textScale})`;
      }
    }, [marker.label, marker.coordinate?.xCoordinate, showLabel]);

    return (
      <div
        ref={containerRef}
        className="absolute z-0 flex items-center justify-center font-bold text-gray-800 dark:text-gray-200 rounded-md overflow-hidden fade-in"
        style={{
          left: `${cellToPx(marker.coordinate?.xCoordinate ?? 1)}px`,
          top: `${cellToPx(marker.coordinate?.yCoordinate ?? 1)}px`,
          width: `${SEAT_SIZE}px`,
          height: `${SEAT_SIZE}px`,
          fontSize: "14px",
          transition: "width 0.2s ease, left 0.2s ease",
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
  pointsAttr: string;
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
        "absolute rounded-lg border-2 border-dashed pointer-events-none fade-in",
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

const AreaPolygonZoneComponent = React.memo(
  ({ zone }: { zone: AreaPolygonZone }) => {
    const color = getAreaColor(zone.colorIndex);

    return (
      <div
        className="absolute pointer-events-none fade-in"
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

const SeatMapLoadingSpinner = () => {
  const t = useT();

  return (
    <div className="absolute inset-0 z-10 flex flex-col items-center justify-center gap-3">
      <div className="relative h-16 w-16">
        <div className="absolute inset-0 rounded-full border-4 border-primary/15" />
        <div className="absolute inset-0 animate-spin rounded-full border-4 border-transparent border-t-primary border-r-primary/40" />
        <Armchair className="absolute inset-0 m-auto h-7 w-7 animate-pulse text-primary" />
      </div>
      <span className="text-sm font-medium text-muted-foreground animate-pulse">
        {t("seatMap.loading")}
      </span>
    </div>
  );
};

const SeatMapEmptyState = () => {
  const t = useT();

  return (
    <div className="absolute inset-0 z-10 flex flex-col items-center justify-center gap-3">
      <Armchair className="h-10 w-10 text-muted-foreground/50" />
      <span className="text-sm font-medium text-muted-foreground">
        {t("seatMap.empty")}
      </span>
    </div>
  );
};

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
  isLoading = false,
}: Readonly<SeatMapProps>): ReactElement {
  const t = useT();

  const isMapEmpty =
    seats.length === 0 && markers.length === 0 && areas.length === 0;

  const {
    maxX,
    maxY,
    seatPositionMap,
    selectedSeatIds,
    userReservedSeatIds,
    renderedMarkers,
    areaZones,
  } = useMemo(() => {
    if (isLoading || isMapEmpty) {
      return {
        maxX: 14,
        maxY: 12,
        seatPositionMap: new Map<string, SeatDto>(),
        selectedSeatIds: new Set<string>(),
        userReservedSeatIds: new Set<string>(),
        renderedMarkers: [],
        areaZones: [],
      };
    }
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

    const selectedSeatIds = new Set(selectedSeats.map((s) => s.id));
    const userReservedSeatIds = new Set(userReservedSeats.map((s) => s.id));

    const renderedMarkers = markers.filter(
      (marker) =>
        marker.coordinate?.xCoordinate != null &&
        marker.coordinate?.yCoordinate != null,
    );

    const areaZones: AreaZone[] = areas.flatMap((area, index): AreaZone[] => {
      const key =
        area.id != null ? `area-${area.id}` : (area.name ?? `area-${index}`);

      const validBoundaryPoints = (area.boundary ?? []).filter(
        (p): p is Required<CoordinateDto> =>
          p.xCoordinate != null && p.yCoordinate != null,
      );

      if (validBoundaryPoints.length >= 3) {
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
  }, [
    seats,
    selectedSeats,
    userReservedSeats,
    markers,
    areas,
    isLoading,
    isMapEmpty,
  ]);

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

      if (seatStatuses.length > 0 && isSupervisorSeatStatus(seatStatuses[0])) {
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

      if (selectedSeatIds.has(seat.id)) return true;

      if (seatStatuses.length > 0 && isSupervisorSeatStatus(seatStatuses[0])) {
        const hasSupervisorStatus = (
          seatStatuses as SupervisorSeatStatusDto[]
        ).some((s) => s.seatId === seat.id);
        return !hasSupervisorStatus;
      } else {
        const seatStatus = findSeatStatus(
          seat.id,
          seatStatuses as SeatStatusDto[],
        );
        return !seatStatus;
      }
    },
    [readonly, selectedSeatIds, seatStatuses],
  );

  const gridStructure = useMemo(() => {
    return Array.from({ length: maxY }, (_, y) =>
      Array.from({ length: maxX }, (_, x) => {
        const seat = seatPositionMap.get(`${x + 1}-${y + 1}`);
        const seatColor = getSeatColor(seat);
        const clickable = canSelectSeat(seat);
        const highlighted = !!seat && seat.id === highlightedSeatId;
        const selected = !!seat && selectedSeatIds.has(seat.id);

        return {
          key: `${x}-${y}`,
          seat,
          seatColor,
          clickable,
          highlighted,
          selected,
          popDelayMs: Math.min(x * 18 + y * 26, 480),
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
    selectedSeatIds,
  ]);

  const displayFlags = useMemo(
    () => ({
      showSeatNumber: zoom > 0.6,
    }),
    [zoom],
  );

  const gridItems = useMemo(() => {
    return gridStructure.map(
      ({
        key,
        seat,
        seatColor,
        clickable,
        highlighted,
        selected,
        popDelayMs,
      }) => (
        <SeatComponent
          key={key}
          seat={seat}
          seatColor={seatColor}
          clickable={clickable}
          highlighted={highlighted}
          selected={selected}
          popDelayMs={popDelayMs}
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
          type="button"
          onClick={zoomIn}
          className="px-2 py-1 bg-seatmap border rounded shadow-xs hover:bg-secondary text-sm dark:text-white"
        >
          +
        </button>
        <button
          type="button"
          onClick={zoomOut}
          className="px-2 py-1 bg-seatmap border rounded shadow-xs hover:bg-secondary text-sm dark:text-white"
        >
          -
        </button>
        <button
          type="button"
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
            className={cn(
              "border-2 border rounded-lg mb-0 bg-seatmap transition-opacity duration-300",
              isLoading
                ? "opacity-0 pointer-events-none border-transparent bg-transparent"
                : "opacity-100",
            )}
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
            {isLoading && <SeatMapLoadingSpinner />}
            {!isLoading && isMapEmpty && <SeatMapEmptyState />}

            {areaZones.map((zone) =>
              zone.shape === "polygon" ? (
                <AreaPolygonZoneComponent key={zone.key} zone={zone} />
              ) : (
                <AreaZoneComponent key={zone.key} zone={zone} />
              ),
            )}

            {renderedMarkers.map((marker, index) => (
              <MarkerComponent
                key={marker.id ? `marker-${marker.id}` : `marker-${index}`}
                marker={marker}
                showLabel={true}
              />
            ))}

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
