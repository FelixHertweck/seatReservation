"use client";

import React, { useMemo, useRef, useState } from "react";
import {
  DndContext,
  useDraggable,
  useSensor,
  useSensors,
  PointerSensor,
  KeyboardSensor,
  type DragEndEvent,
  type DragMoveEvent,
  type DragStartEvent,
} from "@dnd-kit/core";

import { cn } from "@/lib/utils";
import { useT } from "@/lib/i18n/hooks";
import { getAreaColor } from "@/lib/areaColors";
import { useMapViewport } from "@/components/common/use-map-viewport";
import {
  SEAT_SIZE,
  CELL_TOTAL_SIZE,
  ZONE_INSET,
  cellToPx,
  mapPxSize,
  gridContentPxSize,
  boundaryToPixelPolygon,
} from "@/components/common/seat-map-geometry";
import type { useLocationEditorSave } from "@/components/management/location-editor/use-location-editor-save";
import type {
  EditorMarker,
  EditorSeat,
  LocalId,
  LocationEditorState,
} from "@/components/management/location-editor/types";

const EDGE_PAD = 4;
const MIN_GRID_W = 20;
const MIN_GRID_H = 15;
const MAX_GRID = 200;

export type EditorTool = "select" | "draw-area";

interface SeatMapEditorProps {
  state: LocationEditorState;
  autosave: ReturnType<typeof useLocationEditorSave>;
  selection: Set<LocalId>;
  onSelectionChange: (next: Set<LocalId>) => void;
  selectedAreaId: LocalId | null;
  onSelectedAreaChange: (id: LocalId | null) => void;
  tool: EditorTool;
  onAreaDrawn: (boundary: { x: number; y: number }[]) => void;
  onCancelDrawArea: () => void;
}

function useGridExtent(state: LocationEditorState) {
  return useMemo(() => {
    const xs = [
      ...state.seats.map((s) => s.x),
      ...state.markers.map((m) => m.x),
      ...state.areas.flatMap((a) => a.boundary.map((p) => p.x)),
    ];
    const ys = [
      ...state.seats.map((s) => s.y),
      ...state.markers.map((m) => m.y),
      ...state.areas.flatMap((a) => a.boundary.map((p) => p.y)),
    ];
    const contentMaxX = xs.length > 0 ? Math.max(...xs) : 0;
    const contentMaxY = ys.length > 0 ? Math.max(...ys) : 0;
    return {
      maxX: Math.min(MAX_GRID, Math.max(MIN_GRID_W, contentMaxX + EDGE_PAD)),
      maxY: Math.min(MAX_GRID, Math.max(MIN_GRID_H, contentMaxY + EDGE_PAD)),
    };
  }, [state.seats, state.markers, state.areas]);
}

const SyncBadge = ({ syncState }: { syncState: EditorSeat["syncState"] }) => {
  if (syncState === "synced") return null;
  return (
    <div
      className={cn(
        "absolute -right-0.5 -top-0.5 h-2.5 w-2.5 rounded-full border border-white",
        syncState === "saving" && "animate-pulse bg-amber-400",
        syncState === "error" && "bg-red-500",
      )}
    />
  );
};

interface SeatNodeProps {
  seat: EditorSeat;
  selected: boolean;
  dimmed: boolean;
  tool: EditorTool;
  zoom: number;
  onClick: (id: LocalId, shift: boolean) => void;
}

const SeatNode = React.memo(function SeatNode({
  seat,
  selected,
  dimmed,
  tool,
  zoom,
  onClick,
}: SeatNodeProps) {
  const { attributes, listeners, setNodeRef, transform, isDragging } =
    useDraggable({ id: seat.localId, disabled: tool !== "select" });

  return (
    <button
      ref={setNodeRef}
      type="button"
      {...listeners}
      {...attributes}
      onMouseDown={(e) => e.stopPropagation()}
      onTouchStart={(e) => e.stopPropagation()}
      onClick={(e) => {
        if (tool !== "select") return;
        e.stopPropagation();
        onClick(seat.localId, e.shiftKey);
      }}
      className={cn(
        "absolute z-10 flex h-8 w-8 items-center justify-center rounded-full text-[10px] font-medium text-white transition-opacity",
        "bg-emerald-600 hover:brightness-110 focus-visible:outline focus-visible:outline-2 focus-visible:outline-ring",
        selected && "ring-2 ring-offset-1 ring-blue-500",
        seat.syncState === "error" && "outline outline-2 outline-red-500",
        isDragging && "opacity-70 z-30",
        dimmed && "opacity-25 saturate-0 hover:brightness-100",
      )}
      style={{
        left: cellToPx(seat.x),
        top: cellToPx(seat.y),
        width: SEAT_SIZE,
        height: SEAT_SIZE,
        touchAction: "none",
        transform: transform
          ? `translate3d(${transform.x / zoom}px, ${transform.y / zoom}px, 0)`
          : undefined,
      }}
      title={`${seat.seatRow}${seat.seatNumber}`}
    >
      {seat.seatNumber}
      <SyncBadge syncState={seat.syncState} />
    </button>
  );
});

interface MarkerNodeProps {
  marker: EditorMarker;
  selected: boolean;
  tool: EditorTool;
  zoom: number;
  onClick: (id: LocalId, shift: boolean) => void;
}

const MarkerNode = React.memo(function MarkerNode({
  marker,
  selected,
  tool,
  zoom,
  onClick,
}: MarkerNodeProps) {
  const { attributes, listeners, setNodeRef, transform, isDragging } =
    useDraggable({ id: marker.localId, disabled: tool !== "select" });

  return (
    <button
      ref={setNodeRef}
      type="button"
      {...listeners}
      {...attributes}
      onMouseDown={(e) => e.stopPropagation()}
      onTouchStart={(e) => e.stopPropagation()}
      onClick={(e) => {
        if (tool !== "select") return;
        e.stopPropagation();
        onClick(marker.localId, e.shiftKey);
      }}
      className={cn(
        "absolute z-0 flex h-8 w-8 items-center justify-center overflow-hidden rounded-md bg-slate-300/80 px-0.5 text-[10px] font-bold text-slate-800 dark:bg-slate-600/80 dark:text-slate-100",
        selected && "ring-2 ring-offset-1 ring-blue-500",
        isDragging && "opacity-70 z-30",
      )}
      style={{
        left: cellToPx(marker.x),
        top: cellToPx(marker.y),
        width: SEAT_SIZE,
        height: SEAT_SIZE,
        touchAction: "none",
        transform: transform
          ? `translate3d(${transform.x / zoom}px, ${transform.y / zoom}px, 0)`
          : undefined,
      }}
      title={marker.label}
    >
      <span className="truncate">{marker.label}</span>
      <SyncBadge syncState={marker.syncState} />
    </button>
  );
});

function VertexHandle({
  areaLocalId,
  index,
  x,
  y,
  zoom,
  onDelete,
}: {
  areaLocalId: LocalId;
  index: number;
  x: number;
  y: number;
  zoom: number;
  onDelete: () => void;
}) {
  const id = `${areaLocalId}:vertex:${index}`;
  const { attributes, listeners, setNodeRef, transform, isDragging } =
    useDraggable({ id });

  return (
    <button
      ref={setNodeRef}
      type="button"
      {...listeners}
      {...attributes}
      onMouseDown={(e) => e.stopPropagation()}
      onTouchStart={(e) => e.stopPropagation()}
      onClick={(e) => e.stopPropagation()}
      onContextMenu={(e) => {
        e.preventDefault();
        e.stopPropagation();
        onDelete();
      }}
      className={cn(
        // Hit area is intentionally larger than the visible dot, so a click
        // aimed at the corner can't land just past a too-small hit target
        // and fall through to the canvas background, deselecting the area.
        "absolute z-20 flex h-6 w-6 -translate-x-1/2 -translate-y-1/2 items-center justify-center",
        isDragging && "opacity-70",
      )}
      style={{
        left: x,
        top: y,
        touchAction: "none",
        transform: transform
          ? `translate3d(calc(-50% + ${transform.x / zoom}px), calc(-50% + ${transform.y / zoom}px), 0)`
          : "translate(-50%, -50%)",
      }}
    >
      <span className="pointer-events-none h-3.5 w-3.5 rounded-full border-2 border-white bg-blue-600 shadow" />
    </button>
  );
}

export function SeatMapEditor({
  state,
  autosave,
  selection,
  onSelectionChange,
  selectedAreaId,
  onSelectedAreaChange,
  tool,
  onAreaDrawn,
  onCancelDrawArea,
}: SeatMapEditorProps) {
  const t = useT();
  const { maxX, maxY } = useGridExtent(state);
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

  const [drawPoints, setDrawPoints] = useState<{ x: number; y: number }[]>([]);
  const [collisionCells, setCollisionCells] = useState<Set<string> | null>(
    null,
  );
  // Live preview while a boundary vertex is being dragged, so the area's
  // fill/outline follows the point instead of only snapping into place on
  // drop. dx/dy are unscaled local px (already divided by zoom), matching
  // the coordinate space VertexHandle's own drag transform renders in.
  const [vertexDrag, setVertexDrag] = useState<{
    areaLocalId: LocalId;
    index: number;
    dx: number;
    dy: number;
  } | null>(null);
  const gridRef = useRef<HTMLDivElement>(null);

  // Shared with handleCanvasClick's draw-area point placement: converts a
  // click's screen position into the grid cell under the cursor, accounting
  // for the map's zoom and the grid's own padding/border offset.
  const clientToCell = (clientX: number, clientY: number) => {
    const rect = gridRef.current!.getBoundingClientRect();
    const px = (clientX - rect.left) / zoom;
    const py = (clientY - rect.top) / zoom;
    return {
      x: Math.max(1, Math.round((px - 16) / CELL_TOTAL_SIZE) + 1),
      y: Math.max(1, Math.round((py - 16) / CELL_TOTAL_SIZE) + 1),
    };
  };

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 4 } }),
    useSensor(KeyboardSensor),
  );

  const selectedArea = state.areas.find((a) => a.localId === selectedAreaId);
  const areaColorIndexByArea = useMemo(() => {
    const m = new Map<LocalId, number>();
    state.areas.forEach((a, i) => m.set(a.localId, i));
    return m;
  }, [state.areas]);

  const occupiedCells = useMemo(() => {
    const m = new Map<string, LocalId>();
    state.seats.forEach((s) => m.set(`${s.x}-${s.y}`, s.localId));
    state.markers.forEach((mk) => m.set(`${mk.x}-${mk.y}`, mk.localId));
    return m;
  }, [state.seats, state.markers]);

  const handleEntityClick = (id: LocalId, shift: boolean) => {
    onSelectedAreaChange(null);
    const next = new Set(selection);
    if (shift) {
      if (next.has(id)) next.delete(id);
      else next.add(id);
    } else {
      next.clear();
      next.add(id);
    }
    onSelectionChange(next);
  };

  const handleDragStart = (event: DragStartEvent) => {
    const id = String(event.active.id);
    // Vertex handles use a synthetic "areaLocalId:vertex:index" id that's
    // never a real seat/marker - selecting it here would wipe `selection`
    // and make SelectionPanel stop showing the (still) selected area.
    if (id.includes(":vertex:")) return;
    if (!selection.has(id)) {
      onSelectionChange(new Set([id]));
    }
  };

  const handleDragMove = (event: DragMoveEvent) => {
    const activeId = String(event.active.id);
    if (activeId.includes(":vertex:")) {
      const [areaLocalId, , indexStr] = activeId.split(":");
      setVertexDrag({
        areaLocalId,
        index: Number(indexStr),
        dx: event.delta.x / zoom,
        dy: event.delta.y / zoom,
      });
      return;
    }
    const ids = selection.has(activeId) ? selection : new Set([activeId]);
    const dx = Math.round(event.delta.x / zoom / CELL_TOTAL_SIZE);
    const dy = Math.round(event.delta.y / zoom / CELL_TOTAL_SIZE);
    if (dx === 0 && dy === 0) {
      setCollisionCells(null);
      return;
    }
    const targets = new Set<string>();
    let blocked = false;
    ids.forEach((id) => {
      const seat = state.seats.find((s) => s.localId === id);
      const marker = seat
        ? undefined
        : state.markers.find((m) => m.localId === id);
      const entity = seat ?? marker;
      if (!entity) return;
      const tx = entity.x + dx;
      const ty = entity.y + dy;
      const key = `${tx}-${ty}`;
      const occupant = occupiedCells.get(key);
      if (occupant && !ids.has(occupant)) blocked = true;
      targets.add(key);
    });
    setCollisionCells(blocked ? targets : null);
  };

  // Shared by drag-end and arrow-key nudging: moves the given seats/markers
  // by (dx, dy) unless that would push one off-grid or onto an occupied cell.
  const moveSelection = (ids: Set<LocalId>, dx: number, dy: number) => {
    if (dx === 0 && dy === 0) return;

    let blocked = false;
    ids.forEach((id) => {
      const entity =
        state.seats.find((s) => s.localId === id) ??
        state.markers.find((m) => m.localId === id);
      if (!entity) return;
      const tx = entity.x + dx;
      const ty = entity.y + dy;
      if (tx < 1 || ty < 1) {
        blocked = true;
        return;
      }
      const key = `${tx}-${ty}`;
      if (occupiedCells.get(key) && !ids.has(occupiedCells.get(key)!)) {
        blocked = true;
      }
    });
    if (blocked) return;

    autosave.moveEntities(ids, dx, dy);
  };

  const handleDragEnd = (event: DragEndEvent) => {
    const activeId = String(event.active.id);
    setCollisionCells(null);
    setVertexDrag(null);

    if (activeId.includes(":vertex:")) {
      const [areaLocalId, , indexStr] = activeId.split(":");
      const index = Number(indexStr);
      const area = state.areas.find((a) => a.localId === areaLocalId);
      const point = area?.boundary[index];
      if (!point) return;
      const dx = Math.round(event.delta.x / zoom / CELL_TOTAL_SIZE);
      const dy = Math.round(event.delta.y / zoom / CELL_TOTAL_SIZE);
      if (dx === 0 && dy === 0) return;
      autosave.moveAreaPoint(
        areaLocalId,
        index,
        Math.max(1, point.x + dx),
        Math.max(1, point.y + dy),
      );
      return;
    }

    const ids = selection.has(activeId) ? selection : new Set([activeId]);
    const dx = Math.round(event.delta.x / zoom / CELL_TOTAL_SIZE);
    const dy = Math.round(event.delta.y / zoom / CELL_TOTAL_SIZE);
    moveSelection(ids, dx, dy);
  };

  const handleDragCancel = () => {
    setCollisionCells(null);
    setVertexDrag(null);
  };

  const handleEdgeClick = (
    e: React.MouseEvent,
    areaLocalId: LocalId,
    edgeIndex: number,
  ) => {
    e.stopPropagation();
    if (tool !== "select") return;
    const cell = clientToCell(e.clientX, e.clientY);
    autosave.insertAreaPoint(areaLocalId, edgeIndex + 1, cell.x, cell.y);
  };

  const handleCanvasClick = (e: React.MouseEvent) => {
    if (tool !== "draw-area") {
      onSelectionChange(new Set());
      onSelectedAreaChange(null);
      return;
    }
    const rect = e.currentTarget.getBoundingClientRect();
    const px = (e.clientX - rect.left) / zoom;
    const py = (e.clientY - rect.top) / zoom;

    if (drawPoints.length >= 3) {
      const first = drawPoints[0];
      const firstPx = cellToPx(first.x) + SEAT_SIZE / 2;
      const firstPy = cellToPx(first.y) + SEAT_SIZE / 2;
      // Generous, raw-pixel hit radius around the first point (not
      // grid-snapped) so closing the shape doesn't require clicking the
      // exact same cell the first point sits in.
      if (Math.hypot(px - firstPx, py - firstPy) < CELL_TOTAL_SIZE) {
        onAreaDrawn(drawPoints);
        setDrawPoints([]);
        return;
      }
    }
    const cellX = Math.max(1, Math.round((px - 16) / CELL_TOTAL_SIZE) + 1);
    const cellY = Math.max(1, Math.round((py - 16) / CELL_TOTAL_SIZE) + 1);
    setDrawPoints([...drawPoints, { x: cellX, y: cellY }]);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (tool === "draw-area") {
      if (e.key === "Enter" && drawPoints.length >= 3) {
        onAreaDrawn(drawPoints);
        setDrawPoints([]);
      } else if (e.key === "Escape") {
        setDrawPoints([]);
        onCancelDrawArea();
      }
      return;
    }
    if (e.key === "Delete" || e.key === "Backspace") {
      if (selection.size > 0) {
        autosave.deleteEntities(selection);
        onSelectionChange(new Set());
      }
    } else if (e.key === "Escape") {
      onSelectionChange(new Set());
      onSelectedAreaChange(null);
    } else if (
      tool === "select" &&
      selection.size > 0 &&
      (e.key === "ArrowUp" ||
        e.key === "ArrowDown" ||
        e.key === "ArrowLeft" ||
        e.key === "ArrowRight")
    ) {
      e.preventDefault();
      const dx = e.key === "ArrowLeft" ? -1 : e.key === "ArrowRight" ? 1 : 0;
      const dy = e.key === "ArrowUp" ? -1 : e.key === "ArrowDown" ? 1 : 0;
      moveSelection(selection, dx, dy);
    }
  };

  return (
    <div
      className="relative h-full w-full overflow-hidden rounded-lg border bg-seatmap"
      tabIndex={0}
      onKeyDown={handleKeyDown}
    >
      <div className="absolute right-2 top-2 z-40 flex gap-2">
        <button
          onClick={zoomIn}
          className="rounded border bg-seatmap px-2 py-1 text-sm shadow-xs hover:bg-secondary"
        >
          +
        </button>
        <button
          onClick={zoomOut}
          className="rounded border bg-seatmap px-2 py-1 text-sm shadow-xs hover:bg-secondary"
        >
          -
        </button>
        <button
          onClick={resetView}
          className="rounded border bg-seatmap px-2 py-1 text-sm shadow-xs hover:bg-secondary"
        >
          {t("seatMap.resetButton")}
        </button>
      </div>

      {tool === "draw-area" && (
        <div className="absolute left-2 top-2 z-40 max-w-xs rounded-md border bg-seatmap px-3 py-2 text-xs shadow">
          {t("management.locationEditor.areas.drawing")}
        </div>
      )}

      {tool === "select" && selectedArea && (
        <div className="absolute left-2 top-2 z-40 max-w-xs rounded-md border bg-seatmap px-3 py-2 text-xs shadow">
          {t("management.locationEditor.areas.editHint")}
        </div>
      )}

      <DndContext
        sensors={sensors}
        onDragStart={handleDragStart}
        onDragMove={handleDragMove}
        onDragEnd={handleDragEnd}
        onDragCancel={handleDragCancel}
      >
        <div
          ref={containerRef}
          className="flex h-full w-full items-center justify-center p-4 pt-16 cursor-grab active:cursor-grabbing"
          {...panHandlers}
          style={{ touchAction: "none", willChange: "transform" }}
        >
          <div
            ref={mapRef}
            style={{
              transform: `scale(${zoom}) translate3d(${pan.x / zoom}px, ${pan.y / zoom}px, 0)`,
              transformOrigin: "center center",
              willChange: "transform",
            }}
          >
            <div
              ref={gridRef}
              className="relative rounded-lg border bg-seatmap"
              style={{
                width: mapPxSize(maxX),
                height: mapPxSize(maxY),
                backgroundImage:
                  "linear-gradient(to right, var(--border) 1px, transparent 1px), linear-gradient(to bottom, var(--border) 1px, transparent 1px)",
                backgroundSize: `${CELL_TOTAL_SIZE}px ${CELL_TOTAL_SIZE}px`,
                backgroundPosition: "12px 12px",
              }}
              onClick={handleCanvasClick}
            >
              {/* Areas */}
              <svg
                className="pointer-events-none absolute inset-0 z-0"
                width="100%"
                height="100%"
                style={{ overflow: "visible" }}
              >
                {state.areas.map((area) => {
                  const color = getAreaColor(
                    areaColorIndexByArea.get(area.localId) ?? 0,
                  );

                  if (area.boundary.length < 3) {
                    // No explicit boundary - fall back to a bounding-box rect
                    // over the area's member seats, mirroring the read-only
                    // SeatMap's auto rendering, so the area stays visible.
                    const memberSeats = state.seats.filter(
                      (s) => s.areaRef === area.localId,
                    );
                    if (memberSeats.length === 0) return null;
                    const xs = memberSeats.map((s) => s.x);
                    const ys = memberSeats.map((s) => s.y);
                    const minX = Math.min(...xs);
                    const maxAreaX = Math.max(...xs);
                    const minY = Math.min(...ys);
                    const maxAreaY = Math.max(...ys);
                    return (
                      <rect
                        key={area.localId}
                        x={cellToPx(minX) - ZONE_INSET}
                        y={cellToPx(minY) - ZONE_INSET}
                        width={
                          gridContentPxSize(maxAreaX - minX + 1) +
                          ZONE_INSET * 2
                        }
                        height={
                          gridContentPxSize(maxAreaY - minY + 1) +
                          ZONE_INSET * 2
                        }
                        rx={8}
                        fill={color.hex}
                        fillOpacity={
                          area.localId === selectedAreaId ? 0.22 : 0.12
                        }
                        stroke={color.hex}
                        strokeOpacity={0.8}
                        strokeWidth={area.localId === selectedAreaId ? 3 : 2}
                        strokeDasharray="6 4"
                        className="pointer-events-auto cursor-pointer"
                        onClick={(e) => {
                          e.stopPropagation();
                          if (tool !== "select") return;
                          onSelectionChange(new Set());
                          onSelectedAreaChange(area.localId);
                        }}
                      />
                    );
                  }

                  const zone = boundaryToPixelPolygon(
                    area.boundary.map((p, i) => {
                      // While a vertex of this area is being dragged, offset
                      // just that point live so the fill/outline tracks the
                      // handle instead of only snapping into place on drop.
                      if (
                        vertexDrag &&
                        vertexDrag.areaLocalId === area.localId &&
                        vertexDrag.index === i
                      ) {
                        return {
                          xCoordinate: p.x + vertexDrag.dx / CELL_TOTAL_SIZE,
                          yCoordinate: p.y + vertexDrag.dy / CELL_TOTAL_SIZE,
                        };
                      }
                      return { xCoordinate: p.x, yCoordinate: p.y };
                    }),
                  );
                  return (
                    <polygon
                      key={area.localId}
                      points={zone.pointsAttr
                        .split(" ")
                        .map((pair) => {
                          const [px, py] = pair.split(",").map(Number);
                          return `${px + zone.left},${py + zone.top}`;
                        })
                        .join(" ")}
                      fill={color.hex}
                      fillOpacity={
                        area.localId === selectedAreaId ? 0.22 : 0.12
                      }
                      stroke={color.hex}
                      strokeOpacity={0.8}
                      strokeWidth={area.localId === selectedAreaId ? 3 : 2}
                      strokeDasharray="6 4"
                      className="pointer-events-auto cursor-pointer"
                      onClick={(e) => {
                        e.stopPropagation();
                        if (tool !== "select") return;
                        onSelectionChange(new Set());
                        onSelectedAreaChange(area.localId);
                      }}
                    />
                  );
                })}
                {drawPoints.length > 0 && (
                  <polyline
                    points={drawPoints
                      .map(
                        (p) =>
                          `${cellToPx(p.x) + SEAT_SIZE / 2},${cellToPx(p.y) + SEAT_SIZE / 2}`,
                      )
                      .join(" ")}
                    fill="none"
                    stroke="var(--primary)"
                    strokeWidth={2}
                    strokeDasharray="4 4"
                  />
                )}

                {/* Invisible hit-areas along the selected area's edges - click
                    to insert a new (immediately draggable) boundary point. */}
                {tool === "select" &&
                  selectedArea &&
                  selectedArea.boundary.map((p, i) => {
                    const next =
                      selectedArea.boundary[
                        (i + 1) % selectedArea.boundary.length
                      ];
                    return (
                      <line
                        key={`${selectedArea.localId}-edge-${i}`}
                        x1={cellToPx(p.x) + SEAT_SIZE / 2}
                        y1={cellToPx(p.y) + SEAT_SIZE / 2}
                        x2={cellToPx(next.x) + SEAT_SIZE / 2}
                        y2={cellToPx(next.y) + SEAT_SIZE / 2}
                        stroke="transparent"
                        strokeWidth={10}
                        className="pointer-events-auto cursor-copy"
                        onClick={(e) =>
                          handleEdgeClick(e, selectedArea.localId, i)
                        }
                      />
                    );
                  })}
              </svg>

              {/* Vertex handles for the selected area */}
              {selectedArea &&
                selectedArea.boundary.map((p, i) => (
                  <VertexHandle
                    key={`${selectedArea.localId}-${i}`}
                    areaLocalId={selectedArea.localId}
                    index={i}
                    x={cellToPx(p.x) + SEAT_SIZE / 2}
                    y={cellToPx(p.y) + SEAT_SIZE / 2}
                    zoom={zoom}
                    onDelete={() => {
                      if (selectedArea.boundary.length <= 3) return;
                      autosave.deleteAreaPoint(selectedArea.localId, i);
                    }}
                  />
                ))}

              {/* Draw-mode point markers. Once there are enough points to
                  close the shape, the first point is highlighted so it's
                  clear it can be clicked again to finish the area. */}
              {drawPoints.map((p, i) => {
                const closable = i === 0 && drawPoints.length >= 3;
                return (
                  <div
                    key={i}
                    className={cn(
                      "absolute z-20 -translate-x-1/2 -translate-y-1/2 rounded-full border-2 border-white bg-primary",
                      closable
                        ? "h-4 w-4 animate-pulse cursor-pointer ring-2 ring-primary ring-offset-2 ring-offset-seatmap"
                        : "h-3 w-3",
                    )}
                    style={{
                      left: cellToPx(p.x) + SEAT_SIZE / 2,
                      top: cellToPx(p.y) + SEAT_SIZE / 2,
                    }}
                    title={
                      closable
                        ? t("management.locationEditor.areas.closeHint")
                        : undefined
                    }
                  />
                );
              })}

              {/* Collision hint */}
              {collisionCells &&
                [...collisionCells].map((key) => {
                  const [cx, cy] = key.split("-").map(Number);
                  return (
                    <div
                      key={key}
                      className="pointer-events-none absolute z-30 rounded-full ring-2 ring-red-500"
                      style={{
                        left: cellToPx(cx),
                        top: cellToPx(cy),
                        width: SEAT_SIZE,
                        height: SEAT_SIZE,
                      }}
                    />
                  );
                })}

              {/* Markers */}
              {state.markers.map((marker) => (
                <MarkerNode
                  key={marker.localId}
                  marker={marker}
                  selected={selection.has(marker.localId)}
                  tool={tool}
                  zoom={zoom}
                  onClick={handleEntityClick}
                />
              ))}

              {/* Seats */}
              {state.seats.map((seat) => (
                <SeatNode
                  key={seat.localId}
                  seat={seat}
                  selected={selection.has(seat.localId)}
                  dimmed={
                    selectedAreaId != null && seat.areaRef !== selectedAreaId
                  }
                  tool={tool}
                  zoom={zoom}
                  onClick={handleEntityClick}
                />
              ))}
            </div>
          </div>
        </div>
      </DndContext>
    </div>
  );
}
