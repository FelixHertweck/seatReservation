"use client";

import { Eraser, PenLine, Trash2, Wand2 } from "lucide-react";

import { useT } from "@/lib/i18n/hooks";
import { Label } from "@/components/custom-ui/label";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/custom-ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useSyncedField } from "@/components/management/location-editor/use-synced-field";
import type { useLocationEditorSave } from "@/components/management/location-editor/use-location-editor-save";
import {
  isCellOccupied,
  type EditorPoint,
  type LocalId,
  type LocationEditorState,
} from "@/components/management/location-editor/types";

interface SelectionPanelProps {
  state: LocationEditorState;
  selection: Set<LocalId>;
  onSelectionChange: (next: Set<LocalId>) => void;
  selectedAreaId: LocalId | null;
  onSelectedAreaChange: (id: LocalId | null) => void;
  autosave: ReturnType<typeof useLocationEditorSave>;
  onDrawAreaBoundary: (areaLocalId: LocalId) => void;
}

const NONE = "__none__";

// Standard ray-casting point-in-polygon test over grid coordinates.
function pointInPolygon(point: EditorPoint, polygon: EditorPoint[]): boolean {
  let inside = false;
  for (let i = 0, j = polygon.length - 1; i < polygon.length; j = i++) {
    const xi = polygon[i].x;
    const yi = polygon[i].y;
    const xj = polygon[j].x;
    const yj = polygon[j].y;
    const intersects =
      yi > point.y !== yj > point.y &&
      point.x < ((xj - xi) * (point.y - yi)) / (yj - yi) + xi;
    if (intersects) inside = !inside;
  }
  return inside;
}

export function SelectionPanel({
  state,
  selection,
  onSelectionChange,
  selectedAreaId,
  onSelectedAreaChange,
  autosave,
  onDrawAreaBoundary,
}: SelectionPanelProps) {
  const t = useT();
  const ids = [...selection];
  const seat =
    ids.length === 1
      ? state.seats.find((s) => s.localId === ids[0])
      : undefined;
  const marker =
    ids.length === 1
      ? state.markers.find((m) => m.localId === ids[0])
      : undefined;
  const selectedArea =
    selection.size === 0
      ? state.areas.find((a) => a.localId === selectedAreaId)
      : undefined;

  const [seatNumber, setSeatNumber] = useSyncedField(
    seat?.seatNumber ?? "",
    `${seat?.localId}:${seat?.seatNumber}`,
  );
  const [seatRow, setSeatRow] = useSyncedField(
    seat?.seatRow ?? "",
    `${seat?.localId}:${seat?.seatRow}`,
  );
  const [markerLabel, setMarkerLabel] = useSyncedField(
    marker?.label ?? "",
    `${marker?.localId}:${marker?.label}`,
  );
  const [areaName, setAreaName] = useSyncedField(
    selectedArea?.name ?? "",
    `${selectedArea?.localId}:${selectedArea?.name}`,
  );

  const entity = seat ?? marker;
  const [posX, setPosX] = useSyncedField(
    String(entity?.x ?? ""),
    `${entity?.localId}:${entity?.x}`,
  );
  const [posY, setPosY] = useSyncedField(
    String(entity?.y ?? ""),
    `${entity?.localId}:${entity?.y}`,
  );

  const positionOccupied =
    !!entity &&
    isCellOccupied(
      state,
      Number.parseInt(posX, 10),
      Number.parseInt(posY, 10),
      entity.localId,
    );

  const commitPosition = () => {
    if (!entity) return;
    const nextX = Number.parseInt(posX, 10);
    const nextY = Number.parseInt(posY, 10);
    if (Number.isNaN(nextX) || Number.isNaN(nextY)) return;
    const dx = nextX - entity.x;
    const dy = nextY - entity.y;
    if (dx === 0 && dy === 0) return;
    if (isCellOccupied(state, nextX, nextY, entity.localId)) return;
    autosave.moveEntities(new Set([entity.localId]), dx, dy);
  };

  const handleDelete = () => {
    autosave.deleteEntities(selection);
    onSelectionChange(new Set());
  };

  const seatCountForArea = (areaLocalId: LocalId) =>
    state.seats.filter((s) => s.areaRef === areaLocalId).length;

  const handleDeleteArea = (areaLocalId: LocalId) => {
    const referencedCount = seatCountForArea(areaLocalId);
    if (referencedCount > 0) {
      const seatIds = state.seats
        .filter((s) => s.areaRef === areaLocalId)
        .map((s) => s.localId);
      autosave.assignAreaToSeats(new Set(seatIds), undefined);
    }
    autosave.deleteAreas(new Set([areaLocalId]));
    onSelectedAreaChange(null);
  };

  if (selection.size === 0 && !selectedArea) {
    return (
      <p className="text-sm text-muted-foreground">
        {t("management.locationEditor.selection.empty")}
      </p>
    );
  }

  if (selection.size > 1) {
    const selectedSeatIds = ids.filter((id) =>
      state.seats.some((s) => s.localId === id),
    );
    const allSeats = selectedSeatIds.length === ids.length;

    return (
      <div className="space-y-4">
        <p className="text-sm">
          {t("management.locationEditor.selection.multiple", {
            count: selection.size,
          })}
        </p>
        {allSeats && state.areas.some((a) => a.serverId) && (
          <div className="space-y-2">
            <Label>{t("management.locationEditor.seats.areaLabel")}</Label>
            <Select
              value={NONE}
              onValueChange={(value) =>
                autosave.assignAreaToSeats(
                  new Set(selectedSeatIds),
                  value === NONE ? undefined : value,
                )
              }
            >
              <SelectTrigger>
                <SelectValue
                  placeholder={t(
                    "management.locationEditor.selection.assignAreaPlaceholder",
                  )}
                />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={NONE}>
                  {t("management.locationEditor.seats.noneOption")}
                </SelectItem>
                {state.areas
                  .filter((a) => a.serverId)
                  .map((a) => (
                    <SelectItem key={a.localId} value={a.localId}>
                      {a.name}
                    </SelectItem>
                  ))}
              </SelectContent>
            </Select>
          </div>
        )}
        <Button variant="destructive" size="sm" onClick={handleDelete}>
          <Trash2 className="h-4 w-4" />
          {t("management.locationEditor.selection.deleteButton")}
        </Button>
      </div>
    );
  }

  if (seat) {
    return (
      <div className="space-y-4">
        <p className="text-xs font-medium text-muted-foreground">
          {t("management.locationEditor.selection.seat")}
        </p>
        <div className="grid grid-cols-2 gap-3">
          <div className="space-y-2">
            <Label>
              {t("management.locationEditor.seats.seatNumberLabel")}
            </Label>
            <Input
              value={seatNumber}
              onChange={(e) => setSeatNumber(e.target.value)}
              onBlur={() =>
                seatNumber !== seat.seatNumber &&
                autosave.updateSeat(seat.localId, { seatNumber })
              }
            />
          </div>
          <div className="space-y-2">
            <Label>{t("management.locationEditor.seats.seatRowLabel")}</Label>
            <Input
              value={seatRow}
              onChange={(e) => setSeatRow(e.target.value)}
              onBlur={() =>
                seatRow !== seat.seatRow &&
                autosave.updateSeat(seat.localId, { seatRow })
              }
            />
          </div>
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div className="space-y-2">
            <Label>{t("management.locationEditor.seats.xLabel")}</Label>
            <Input
              type="number"
              min={1}
              value={posX}
              onChange={(e) => setPosX(e.target.value)}
              onBlur={commitPosition}
            />
          </div>
          <div className="space-y-2">
            <Label>{t("management.locationEditor.seats.yLabel")}</Label>
            <Input
              type="number"
              min={1}
              value={posY}
              onChange={(e) => setPosY(e.target.value)}
              onBlur={commitPosition}
            />
          </div>
        </div>
        {positionOccupied && (
          <p className="text-xs text-destructive">
            {t("management.locationEditor.positionOccupied")}
          </p>
        )}

        <div className="space-y-2">
          <Label>{t("management.locationEditor.seats.entranceLabel")}</Label>
          <Select
            value={seat.entranceRef ?? NONE}
            onValueChange={(value) =>
              autosave.updateSeat(seat.localId, {
                entranceRef: value === NONE ? undefined : value,
              })
            }
          >
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={NONE}>
                {t("management.locationEditor.seats.noneOption")}
              </SelectItem>
              {state.entrances
                .filter((e) => e.serverId)
                .map((e) => (
                  <SelectItem key={e.localId} value={e.localId}>
                    {e.name}
                  </SelectItem>
                ))}
            </SelectContent>
          </Select>
        </div>

        <div className="space-y-2">
          <Label>{t("management.locationEditor.seats.areaLabel")}</Label>
          <Select
            value={seat.areaRef ?? NONE}
            onValueChange={(value) =>
              autosave.updateSeat(seat.localId, {
                areaRef: value === NONE ? undefined : value,
              })
            }
          >
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={NONE}>
                {t("management.locationEditor.seats.noneOption")}
              </SelectItem>
              {state.areas
                .filter((a) => a.serverId)
                .map((a) => (
                  <SelectItem key={a.localId} value={a.localId}>
                    {a.name}
                  </SelectItem>
                ))}
            </SelectContent>
          </Select>
        </div>

        <Button variant="destructive" size="sm" onClick={handleDelete}>
          <Trash2 className="h-4 w-4" />
          {t("management.locationEditor.selection.deleteButton")}
        </Button>
      </div>
    );
  }

  if (marker) {
    return (
      <div className="space-y-4">
        <p className="text-xs font-medium text-muted-foreground">
          {t("management.locationEditor.selection.marker")}
        </p>
        <div className="space-y-2">
          <Label>{t("management.locationEditor.markers.labelLabel")}</Label>
          <Input
            value={markerLabel}
            onChange={(e) => setMarkerLabel(e.target.value)}
            onBlur={() =>
              markerLabel !== marker.label &&
              autosave.updateMarker(marker.localId, { label: markerLabel })
            }
          />
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div className="space-y-2">
            <Label>{t("management.locationEditor.seats.xLabel")}</Label>
            <Input
              type="number"
              min={1}
              value={posX}
              onChange={(e) => setPosX(e.target.value)}
              onBlur={commitPosition}
            />
          </div>
          <div className="space-y-2">
            <Label>{t("management.locationEditor.seats.yLabel")}</Label>
            <Input
              type="number"
              min={1}
              value={posY}
              onChange={(e) => setPosY(e.target.value)}
              onBlur={commitPosition}
            />
          </div>
        </div>
        {positionOccupied && (
          <p className="text-xs text-destructive">
            {t("management.locationEditor.positionOccupied")}
          </p>
        )}
        <Button variant="destructive" size="sm" onClick={handleDelete}>
          <Trash2 className="h-4 w-4" />
          {t("management.locationEditor.selection.deleteButton")}
        </Button>
      </div>
    );
  }

  if (selectedArea) {
    const referencedCount = seatCountForArea(selectedArea.localId);
    return (
      <div className="space-y-4">
        <p className="text-xs font-medium text-muted-foreground">
          {t("management.locationEditor.selection.area")}
        </p>
        <div className="space-y-2">
          <Label>{t("management.locationEditor.areas.nameLabel")}</Label>
          <Input
            value={areaName}
            onChange={(e) => setAreaName(e.target.value)}
            onBlur={() =>
              areaName.trim() &&
              areaName !== selectedArea.name &&
              autosave.renameArea(selectedArea.localId, areaName.trim())
            }
          />
        </div>
        <p className="text-xs text-muted-foreground">
          {t("management.locationEditor.areas.seatCount", {
            count: referencedCount,
          })}
        </p>
        <div className="flex flex-wrap gap-2">
          <Button
            size="sm"
            variant="outline"
            disabled={selectedArea.boundary.length < 3}
            title={t("management.locationEditor.areas.autoAssignHint")}
            onClick={() => {
              const inside = state.seats.filter((s) =>
                pointInPolygon({ x: s.x, y: s.y }, selectedArea.boundary),
              );
              autosave.assignAreaToSeats(
                new Set(inside.map((s) => s.localId)),
                selectedArea.localId,
              );
            }}
          >
            <Wand2 className="h-4 w-4" />
            {t("management.locationEditor.areas.autoAssign")}
          </Button>
          {selectedArea.boundary.length > 0 ? (
            <Button
              size="sm"
              variant="outline"
              title={t("management.locationEditor.areas.clearBoundaryHint")}
              onClick={() =>
                autosave.updateAreaBoundary(selectedArea.localId, [])
              }
            >
              <Eraser className="h-4 w-4" />
              {t("management.locationEditor.areas.clearBoundary")}
            </Button>
          ) : (
            <Button
              size="sm"
              variant="outline"
              title={t("management.locationEditor.areas.drawBoundaryHint")}
              onClick={() => onDrawAreaBoundary(selectedArea.localId)}
            >
              <PenLine className="h-4 w-4" />
              {t("management.locationEditor.areas.drawBoundary")}
            </Button>
          )}
          <Button
            variant="destructive"
            size="sm"
            title={
              referencedCount > 0
                ? t("management.locationEditor.areas.cannotDeleteReferenced", {
                    count: referencedCount,
                  })
                : undefined
            }
            onClick={() => handleDeleteArea(selectedArea.localId)}
          >
            <Trash2 className="h-4 w-4" />
            {t("management.locationEditor.selection.deleteButton")}
          </Button>
        </div>
      </div>
    );
  }

  return null;
}
